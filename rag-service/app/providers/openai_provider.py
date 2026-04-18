import logging
from app.providers import BaseLLMProvider, ProviderConfig

logger = logging.getLogger(__name__)


class OpenAIProvider(BaseLLMProvider):
    def __init__(self, config: ProviderConfig):
        from openai import AsyncOpenAI
        self._client = AsyncOpenAI(api_key=config.api_key)
        self._model = config.model or "gpt-4o"

    @property
    def provider_name(self) -> str:
        return "openai"

    async def complete(self, system_prompt: str, user_prompt: str) -> str:
        response = await self._client.chat.completions.create(
            model=self._model,
            temperature=0.7,
            max_tokens=1200,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user",   "content": user_prompt},
            ],
        )
        return response.choices[0].message.content