CREATE TABLE IF NOT EXISTS user_interests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category        VARCHAR(100) NOT NULL,       -- e.g. "technology", "physical", "mental"
    subcategory     VARCHAR(100) NOT NULL DEFAULT '',  -- e.g. "software", "strength", "meditation"
    custom_goal     TEXT NOT NULL DEFAULT '',    -- free-text expectation from the user
    priority        INT  NOT NULL DEFAULT 1,     -- 1=primary, 2=secondary, 3=optional
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT user_interests_unique UNIQUE (user_id, category, subcategory)
);
 
CREATE INDEX IF NOT EXISTS idx_user_interests_user_id ON user_interests(user_id);

ALTER TABLE users ADD COLUMN IF NOT EXISTS interests_configured BOOLEAN NOT NULL DEFAULT FALSE;