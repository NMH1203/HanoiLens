"""Text chunking and 768-dimensional multilingual embedding for Historical RAG."""

from __future__ import annotations

import hashlib
import math
import os
import re
from typing import Any

EMBEDDING_DIMENSION: int = 768
DEFAULT_EMBEDDING_MODEL: str = os.getenv(
    "RAG_EMBEDDING_MODEL",
    "sentence-transformers/paraphrase-multilingual-mpnet-base-v2",
)
DEFAULT_MAX_WORDS: int = 300
DEFAULT_OVERLAP_WORDS: int = 50

_model_instance: Any = None


def chunk_document(
    text: str,
    max_words: int = DEFAULT_MAX_WORDS,
    overlap: int = DEFAULT_OVERLAP_WORDS,
) -> list[str]:
    """Split a historical article into overlapping word chunks.

    Args:
        text: Raw historical document text (Vietnamese or English).
        max_words: Maximum number of words per chunk (must be > 0).
        overlap: Number of overlapping words between consecutive chunks.

    Returns:
        List of non-empty text chunks ready for vector embedding.
    """
    if max_words <= 0:
        raise ValueError("max_words must be greater than 0.")
    if overlap < 0 or overlap >= max_words:
        raise ValueError("overlap must be >= 0 and strictly less than max_words.")

    cleaned = re.sub(r"\s+", " ", text).strip()
    if not cleaned:
        return []

    words = cleaned.split(" ")
    if len(words) <= max_words:
        return [cleaned]

    chunks: list[str] = []
    step = max_words - overlap
    for start in range(0, len(words), step):
        slice_words = words[start : start + max_words]
        if not slice_words:
            break
        chunks.append(" ".join(slice_words))
        if start + max_words >= len(words):
            break

    return chunks


def _load_sentence_transformer() -> Any:
    """Lazily load SentenceTransformer if available and enabled."""
    global _model_instance
    if _model_instance is not None:
        return _model_instance

    if os.getenv("RAG_USE_DETERMINISTIC_EMBEDDING", "false").lower() == "true":
        return None

    try:
        from sentence_transformers import SentenceTransformer

        _model_instance = SentenceTransformer(DEFAULT_EMBEDDING_MODEL)
        return _model_instance
    except Exception:
        return None


def _deterministic_embedding(text: str) -> list[float]:
    """Generate a deterministic, L2-normalized 768-dim vector when model weights are absent."""
    normalized_text = re.sub(r"\s+", " ", text.lower()).strip()
    raw_values: list[float] = []

    for idx in range(EMBEDDING_DIMENSION):
        digest = hashlib.sha256(f"{normalized_text}:{idx}".encode("utf-8")).digest()
        int_val = int.from_bytes(digest[:4], byteorder="big", signed=False)
        signed_val = (int_val / 2147483647.5) - 1.0
        raw_values.append(signed_val)

    norm = math.sqrt(sum(val * val for val in raw_values)) or 1.0
    return [round(val / norm, 6) for val in raw_values]


def embed_texts(texts: list[str]) -> list[list[float]]:
    """Convert a batch of texts into 768-dimensional L2-normalized vectors.

    Args:
        texts: List of text strings (questions or historical chunks).

    Returns:
        List of 768-float embedding vectors matching pgvector VECTOR(768).
    """
    if not texts:
        return []

    cleaned_texts = [re.sub(r"\s+", " ", t).strip() for t in texts]
    model = _load_sentence_transformer()

    if model is not None:
        vectors = model.encode(cleaned_texts, normalize_embeddings=True)
        return [[round(float(x), 6) for x in vec[:EMBEDDING_DIMENSION]] for vec in vectors]

    return [_deterministic_embedding(t) for t in cleaned_texts]


def embed_text(text: str) -> list[float]:
    """Convert a single text string into a 768-dimensional vector."""
    cleaned = text.strip()
    if not cleaned:
        raise ValueError("Input text for embedding must not be empty.")
    return embed_texts([cleaned])[0]
