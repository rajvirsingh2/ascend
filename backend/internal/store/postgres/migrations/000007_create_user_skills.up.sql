CREATE TABLE user_skills (
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_name   VARCHAR(50) NOT NULL,
    skill_level  INTEGER     NOT NULL DEFAULT 1 CHECK (skill_level BETWEEN 1 AND 10),
    xp_in_skill  INTEGER     NOT NULL DEFAULT 0,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (user_id, skill_name)
);

CREATE INDEX idx_user_skills_user ON user_skills (user_id);