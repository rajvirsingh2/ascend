import logging
from app.providers.base import BaseLLMProvider, ProviderConfig

logger = logging.getLogger(__name__)


class GeminiProvider(BaseLLMProvider):
    def __init__(self, config: ProviderConfig):
        import google.generativeai as genai
        genai.configure(api_key=config.api_key)
        model_name = config.model or "gemini-1.5-flash"
        self._model = genai.GenerativeModel(
            model_name=model_name,
            system_instruction=None,  # set per-call
        )
        self._model_name = model_name

    @property
    def provider_name(self) -> str:
        return "gemini"

    async def complete(self, system_prompt: str, user_prompt: str) -> str:
        import google.generativeai as genai
        model = genai.GenerativeModel(
            model_name=self._model_name,
            system_instruction=system_prompt,
        )
        response = await model.generate_content_async(user_prompt)
        return response.text