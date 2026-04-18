CREATE TABLE user_api_keys (
    user_id       UUID        PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    provider      VARCHAR(20) NOT NULL DEFAULT 'openai'
                      CHECK (provider IN ('openai','claude','gemini','anthropic')),
    model_override VARCHAR(100),
    wrapped_dek   BYTEA       NOT NULL,
    ciphertext    BYTEA       NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);