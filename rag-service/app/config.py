from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file="../.env", extra="ignore", protected_namespaces=("settings_",)
    )

    database_url: str
    redis_url: str
    openai_api_key: str = ""
    app_env: str = "development"

    embedding_model: str = "text-embedding-3-small"
    embedding_dimensions: int = 1536
    model_version: str = "text-embedding-3-small-v1"

    ingestion_queue_key: str = "ingestion_queue"
    worker_batch_size: int = 1
    worker_block_seconds: int = 5


settings = Settings()