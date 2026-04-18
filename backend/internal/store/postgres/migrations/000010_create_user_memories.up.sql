CREATE TABLE user_memories (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    doc_type      VARCHAR(30)  NOT NULL
                      CHECK (doc_type IN (
                          'quest_history',
                          'goal',
                          'habit_pattern',
                          'user_reflection'
                      )),
    entity_id     UUID,
    content       TEXT         NOT NULL,
    embedding     VECTOR(1536) NOT NULL,
    metadata      JSONB        NOT NULL DEFAULT '{}',
    model_version VARCHAR(50)  NOT NULL DEFAULT 'text-embedding-3-small-v1',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- HNSW index: best query latency for cosine similarity
-- m=16 (connections per layer), ef_construction=64 (build quality)
CREATE INDEX idx_memories_embedding_hnsw
    ON user_memories
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Required: prevents full-table scan on every similarity search
CREATE INDEX idx_memories_user_type
    ON user_memories (user_id, doc_type, created_at DESC);

-- Required: for metadata key filtering (skill_area, status, etc.)
CREATE INDEX idx_memories_metadata
    ON user_memories
    USING gin (metadata);