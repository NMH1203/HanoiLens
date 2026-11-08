"""Historical RAG (Model 2) package for Hanoi Lens AI Runtime.

Provides document chunking, multilingual text embedding (768-dim for pgvector),
and source-grounded answer generation with mandatory citation checks.
"""

from app.rag.embedder import (
    EMBEDDING_DIMENSION,
    chunk_document,
    embed_text,
    embed_texts,
)

__all__ = [
    "EMBEDDING_DIMENSION",
    "chunk_document",
    "embed_text",
    "embed_texts",
]