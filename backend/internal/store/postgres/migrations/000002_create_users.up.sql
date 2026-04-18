CREATE TABLE users (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email            VARCHAR(255) NOT NULL UNIQUE,
    password_hash    TEXT         NOT NULL,
    username         VARCHAR(50)  NOT NULL UNIQUE,
    avatar_url       TEXT,
    level            INTEGER      NOT NULL DEFAULT 1,
    total_xp         BIGINT       NOT NULL DEFAULT 0,
    current_xp       INTEGER      NOT NULL DEFAULT 0,
    xp_to_next       INTEGER      NOT NULL DEFAULT 100,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);