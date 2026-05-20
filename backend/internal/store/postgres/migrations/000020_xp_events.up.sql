CREATE TABLE IF NOT EXISTS xp_events (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount     INT NOT NULL,
    source     VARCHAR(50) NOT NULL,   -- 'quest' | 'habit' | 'physique'
    source_id  VARCHAR(100) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_xp_events_user_id    ON xp_events(user_id);
CREATE INDEX IF NOT EXISTS idx_xp_events_created_at ON xp_events(created_at DESC);

-- Add hp and total_xp columns to users if not already present
ALTER TABLE users ADD COLUMN IF NOT EXISTS hp       INT NOT NULL DEFAULT 100;
ALTER TABLE users ADD COLUMN IF NOT EXISTS total_xp INT NOT NULL DEFAULT 0;