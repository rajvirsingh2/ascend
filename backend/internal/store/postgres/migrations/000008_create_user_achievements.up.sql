CREATE TABLE user_achievements (
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_key VARCHAR(100) NOT NULL,
    earned_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata        JSONB        NOT NULL DEFAULT '{}',

    PRIMARY KEY (user_id, achievement_key)
);

CREATE INDEX idx_user_achievements_user ON user_achievements (user_id);