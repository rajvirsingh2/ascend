from abc import ABC, abstractmethod
from dataclasses import dataclass


@dataclass
class ProviderConfig:
    provider: str     # "openai" | "claude" | "gemini"
    api_key: str
    model: str | None = None


class BaseLLMProvider(ABC):
    """All concrete providers implement this interface."""

    @abstractmethod
    async def complete(self, system_prompt: str, user_prompt: str) -> str:
        """Returns the raw text completion."""
        pass

    @property
    @abstractmethod
    def provider_name(self) -> str:
        pass