CREATE TABLE habits (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_id           UUID        REFERENCES goals(id) ON DELETE SET NULL,
    title             VARCHAR(255) NOT NULL,
    frequency         VARCHAR(20)  NOT NULL DEFAULT 'daily'
                          CHECK (frequency IN ('daily','weekly')),
    xp_reward         INTEGER      NOT NULL DEFAULT 10,
    current_streak    INTEGER      NOT NULL DEFAULT 0,
    longest_streak    INTEGER      NOT NULL DEFAULT 0,
    last_completed_at TIMESTAMPTZ,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_habits_user_id ON habits (user_id);
CREATE INDEX idx_habits_user_active ON habits (user_id, is_active);