CREATE TABLE quest_generation_log (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    context_hash         VARCHAR(64) NOT NULL,
    retrieved_memory_ids BIGINT[],
    prompt_version       VARCHAR(20) NOT NULL DEFAULT 'v1',
    llm_model            VARCHAR(50) NOT NULL DEFAULT 'gpt-4o',
    raw_response         JSONB,
    latency_ms           INTEGER,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_genlog_user_hash
    ON quest_generation_log (user_id, context_hash, created_at DESC);