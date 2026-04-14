CREATE TABLE goals (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    category     VARCHAR(50),
    skill_area   VARCHAR(50)  NOT NULL,
    priority     SMALLINT     NOT NULL DEFAULT 2 CHECK (priority BETWEEN 1 AND 3),
    target_date  DATE,
    status       VARCHAR(20)  NOT NULL DEFAULT 'active'
                     CHECK (status IN ('active','completed','abandoned')),
    progress     INTEGER      NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_goals_user_id      ON goals (user_id);
CREATE INDEX idx_goals_user_status  ON goals (user_id, status);