import logging
from app.providers import BaseLLMProvider, ProviderConfig
from app.providers.mock_provider import MockProvider

logger = logging.getLogger(__name__)

# Map of provider names to their import paths
# Lazy imports so missing SDK packages don't crash the service
_PROVIDER_MAP = {
    "openai":  ("app.providers.openai_provider",  "OpenAIProvider"),
    "claude":  ("app.providers.claude_provider",  "ClaudeProvider"),
    "gemini":  ("app.providers.gemini_provider",  "GeminiProvider"),
    "anthropic": ("app.providers.claude_provider", "ClaudeProvider"),  # alias
}


def build_provider(config: ProviderConfig | None) -> BaseLLMProvider:
    """
    Builds the correct LLM provider from a ProviderConfig.
    Falls back to MockProvider if config is None or provider is unknown.
    """
    if config is None or not config.api_key:
        logger.warning("no provider config — using mock LLM")
        return MockProvider()

    provider_name = config.provider.lower()
    entry = _PROVIDER_MAP.get(provider_name)

    if entry is None:
        logger.warning("unknown provider '%s' — using mock LLM", provider_name)
        return MockProvider()

    module_path, class_name = entry
    try:
        import importlib
        module = importlib.import_module(module_path)
        cls = getattr(module, class_name)
        provider = cls(config)
        logger.info("built provider: %s (model=%s)", provider_name, config.model)
        return provider
    except ImportError as e:
        logger.error(
            "provider '%s' SDK not installed: %s — using mock LLM",
            provider_name, e
        )
        return MockProvider()
    except Exception as e:
        logger.error("provider build failed: %s — using mock LLM", e)
        return MockProvider()