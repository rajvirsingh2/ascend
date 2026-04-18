CREATE TABLE quests (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_id         UUID        REFERENCES goals(id) ON DELETE SET NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    type            VARCHAR(20)  NOT NULL DEFAULT 'daily'
                        CHECK (type IN ('daily','weekly')),
    difficulty      SMALLINT     NOT NULL DEFAULT 1 CHECK (difficulty BETWEEN 1 AND 5),
    xp_reward       INTEGER      NOT NULL DEFAULT 25,
    status          VARCHAR(20)  NOT NULL DEFAULT 'active'
                        CHECK (status IN ('active','completed','skipped','expired')),
    is_ai_generated BOOLEAN      NOT NULL DEFAULT FALSE,
    ai_prompt_hash  VARCHAR(64),
    skill_area      VARCHAR(50),
    expires_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_quests_user_id       ON quests (user_id);
CREATE INDEX idx_quests_user_status   ON quests (user_id, status);
CREATE INDEX idx_quests_expires_at    ON quests (expires_at)
    WHERE status = 'active';