-- =============================================================================
-- V5 — Historical RAG
-- Owner : Nguyễn Minh Lương (knowledge)
-- Tables: knowledge_chunks, qa_conversations, qa_messages
-- The backend owns retrieval; the AI runtime never reads these tables.
-- =============================================================================

CREATE TABLE knowledge_chunks (
    id             UUID         NOT NULL,
    destination_id UUID         NOT NULL,
    landmark_id    UUID,
    content        TEXT         NOT NULL,
    embedding      vector(768)  NOT NULL,
    source_title   VARCHAR(240) NOT NULL,
    source_url     TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_knowledge_chunks PRIMARY KEY (id),
    CONSTRAINT fk_knowledge_chunks_destination
        FOREIGN KEY (destination_id) REFERENCES destinations (id),
    CONSTRAINT fk_knowledge_chunks_landmark
        FOREIGN KEY (landmark_id) REFERENCES landmarks (id) ON DELETE SET NULL
);

-- Cosine-distance search: ORDER BY embedding <=> :query LIMIT k.
-- The dimension (768) is fixed by the column type; switching to a 1024-d embedding
-- model needs a new migration (and re-embedding every chunk).
-- With destination/landmark filters, consider `SET hnsw.iterative_scan = relaxed_order`
-- (pgvector >= 0.8) so filtered queries still return k rows.
CREATE INDEX idx_knowledge_chunks_embedding_hnsw
    ON knowledge_chunks USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE INDEX idx_knowledge_chunks_destination_id ON knowledge_chunks (destination_id);
CREATE INDEX idx_knowledge_chunks_landmark_id    ON knowledge_chunks (landmark_id) WHERE landmark_id IS NOT NULL;

COMMENT ON COLUMN knowledge_chunks.landmark_id IS 'NULL = general destination-level material.';

-- -----------------------------------------------------------------------------

CREATE TABLE qa_conversations (
    id             UUID         NOT NULL,
    user_id        UUID         NOT NULL,
    journey_id     UUID,
    destination_id UUID         NOT NULL,
    landmark_id    UUID,
    title          VARCHAR(200),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_qa_conversations PRIMARY KEY (id),
    CONSTRAINT fk_qa_conversations_user        FOREIGN KEY (user_id)        REFERENCES users (id),
    CONSTRAINT fk_qa_conversations_journey     FOREIGN KEY (journey_id)     REFERENCES journeys (id) ON DELETE SET NULL,
    CONSTRAINT fk_qa_conversations_destination FOREIGN KEY (destination_id) REFERENCES destinations (id),
    CONSTRAINT fk_qa_conversations_landmark    FOREIGN KEY (landmark_id)    REFERENCES landmarks (id) ON DELETE SET NULL
);

CREATE INDEX idx_qa_conversations_user_created_at ON qa_conversations (user_id, created_at DESC);
CREATE INDEX idx_qa_conversations_destination_id  ON qa_conversations (destination_id);
CREATE INDEX idx_qa_conversations_journey_id      ON qa_conversations (journey_id)  WHERE journey_id IS NOT NULL;
CREATE INDEX idx_qa_conversations_landmark_id     ON qa_conversations (landmark_id) WHERE landmark_id IS NOT NULL;

-- -----------------------------------------------------------------------------

CREATE TABLE qa_messages (
    id               UUID        NOT NULL,
    conversation_id  UUID        NOT NULL,
    role             VARCHAR(16) NOT NULL,
    content          TEXT        NOT NULL,
    cited_chunk_ids  JSONB,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_qa_messages PRIMARY KEY (id),
    CONSTRAINT fk_qa_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES qa_conversations (id) ON DELETE CASCADE,
    CONSTRAINT ck_qa_messages_role CHECK (role IN ('USER', 'ASSISTANT')),
    CONSTRAINT ck_qa_messages_cited_is_array
        CHECK (cited_chunk_ids IS NULL OR jsonb_typeof(cited_chunk_ids) = 'array')
);

CREATE INDEX idx_qa_messages_conversation_created_at ON qa_messages (conversation_id, created_at);

COMMENT ON COLUMN qa_messages.cited_chunk_ids IS 'JSON array of knowledge_chunks.id; NULL for USER messages.';
