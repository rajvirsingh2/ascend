
CREATE TABLE event_outbox (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    topic      VARCHAR(100) NOT NULL,
    payload    JSONB       NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'pending'
                   CHECK (status IN ('pending','published','failed')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_pending
    ON event_outbox (status, created_at)
    WHERE status = 'pending';