CREATE TABLE progress_logs (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entity_type  VARCHAR(20) NOT NULL CHECK (entity_type IN ('quest','habit','goal','system')),
    entity_id    UUID,
    event_type   VARCHAR(50) NOT NULL,
    xp_delta     INTEGER     NOT NULL DEFAULT 0,
    xp_before    INTEGER     NOT NULL,
    xp_after     INTEGER     NOT NULL,
    level_before INTEGER     NOT NULL,
    level_after  INTEGER     NOT NULL,
    metadata     JSONB        NOT NULL DEFAULT '{}',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_progress_logs_user_id    ON progress_logs (user_id);
CREATE INDEX idx_progress_logs_user_date  ON progress_logs (user_id, created_at DESC);
CREATE INDEX idx_progress_logs_entity     ON progress_logs (entity_type, entity_id);