"""Internal FastAPI routes for Historical RAG (Model 2).

Called exclusively by the Spring Boot backend over the internal network.
Never exposed directly to the Android client.
"""

from __future__ import annotations

from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel, Field

from app.rag.embedder import (
    DEFAULT_MAX_WORDS,
    DEFAULT_OVERLAP_WORDS,
    EMBEDDING_DIMENSION,
    chunk_document,
    embed_text,
    embed_texts,
)

router = APIRouter(prefix="/internal/rag", tags=["internal-rag"])


class EmbedRequest(BaseModel):
    """Request payload for embedding a single question or text chunk."""

    text: str = Field(
        ...,
        min_length=1,
        max_length=10000,
        description="Question or historical passage to convert into a 768-dim vector.",
    )


class EmbedResponse(BaseModel):
    """Response payload returning a 768-dimensional embedding vector."""

    dimension: int = Field(
        default=EMBEDDING_DIMENSION,
        description="Vector dimension matching pgvector VECTOR(768).",
    )
    embedding: list[float] = Field(
        ...,
        description="L2-normalized embedding vector of length 768.",
    )


class BatchEmbedRequest(BaseModel):
    """Request payload for embedding multiple text chunks in one call."""

    texts: list[str] = Field(
        ...,
        min_length=1,
        max_length=128,
        description="Batch of historical text chunks to embed.",
    )


class BatchEmbedResponse(BaseModel):
    """Response payload returning a batch of 768-dimensional vectors."""

    dimension: int = Field(default=EMBEDDING_DIMENSION)
    embeddings: list[list[float]]


class ChunkDocumentRequest(BaseModel):
    """Request payload for splitting a historical article into overlapping chunks."""

    text: str = Field(
        ...,
        min_length=1,
        description="Raw historical article text in Vietnamese or English.",
    )
    max_words: int = Field(
        default=DEFAULT_MAX_WORDS,
        gt=0,
        le=2000,
        description="Maximum number of words per chunk.",
    )
    overlap: int = Field(
        default=DEFAULT_OVERLAP_WORDS,
        ge=0,
        description="Number of overlapping words between consecutive chunks.",
    )


class ChunkDocumentResponse(BaseModel):
    """Response payload returning the list of text chunks."""

    chunk_count: int
    chunks: list[str]


@router.post(
    "/embed",
    response_model=EmbedResponse,
    status_code=status.HTTP_200_OK,
    summary="Embed a single question or chunk into a 768-dim vector",
)
def create_embedding(payload: EmbedRequest) -> EmbedResponse:
    """Convert a single text string into a 768-dimensional vector for pgvector search."""
    cleaned = payload.text.strip()
    if not cleaned:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Text must not be blank or whitespace only.",
        )

    vector = embed_text(cleaned)
    return EmbedResponse(dimension=len(vector), embedding=vector)


@router.post(
    "/embed-batch",
    response_model=BatchEmbedResponse,
    status_code=status.HTTP_200_OK,
    summary="Embed a batch of historical chunks into 768-dim vectors",
)
def create_batch_embeddings(payload: BatchEmbedRequest) -> BatchEmbedResponse:
    """Convert a list of historical chunks into 768-dimensional vectors."""
    cleaned_texts = [t.strip() for t in payload.texts if t and t.strip()]
    if not cleaned_texts:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="At least one non-empty text string is required.",
        )

    vectors = embed_texts(cleaned_texts)
    return BatchEmbedResponse(dimension=EMBEDDING_DIMENSION, embeddings=vectors)


@router.post(
    "/chunk",
    response_model=ChunkDocumentResponse,
    status_code=status.HTTP_200_OK,
    summary="Split a historical article into overlapping chunks",
)
def split_document(payload: ChunkDocumentRequest) -> ChunkDocumentResponse:
    """Split a long historical document into smaller overlapping chunks."""
    if payload.overlap >= payload.max_words:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="overlap must be strictly less than max_words.",
        )

    chunks = chunk_document(
        text=payload.text,
        max_words=payload.max_words,
        overlap=payload.overlap,
    )
    return ChunkDocumentResponse(chunk_count=len(chunks), chunks=chunks)