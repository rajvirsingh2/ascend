import logging
from app.providers.base import BaseLLMProvider, ProviderConfig

logger = logging.getLogger(__name__)


class ClaudeProvider(BaseLLMProvider):
    def __init__(self, config: ProviderConfig):
        import anthropic
        self._client = anthropic.AsyncAnthropic(api_key=config.api_key)
        self._model = config.model or "claude-sonnet-4-6"

    @property
    def provider_name(self) -> str:
        return "claude"

    async def complete(self, system_prompt: str, user_prompt: str) -> str:
        response = await self._client.messages.create(
            model=self._model,
            max_tokens=1200,
            system=system_prompt,
            messages=[{"role": "user", "content": user_prompt}],
        )
        return response.content[0].text