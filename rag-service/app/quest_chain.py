import json
import logging
import os
from pathlib import Path
from typing import Any

from app.config import settings
from app.context_builder import UserContext, format_user_context_for_prompt
from app.retriever import RetrievedMemory, format_memories_for_prompt
from app.providers import ProviderConfig
from app.providers.factory import build_provider

logger = logging.getLogger(__name__)

PROMPT_VERSION = "v1"
PROMPTS_DIR = Path(__file__).parent / "prompts"


def _load_prompt(version: str) -> str:
    path = PROMPTS_DIR / f"{version}.txt"
    return path.read_text()


def _build_prompt_text(ctx: UserContext, memories: list[RetrievedMemory]) -> str:
    template = _load_prompt(PROMPT_VERSION)
    user_profile = format_user_context_for_prompt(ctx)
    memory_context = format_memories_for_prompt(memories)
    return template.format(
        user_profile=user_profile,
        memory_context=memory_context,
    )


class MockLLM:
    """Returned when OPENAI_API_KEY is not set — deterministic fake response."""
    async def ainvoke(self, prompt: str) -> str:
        return json.dumps({
            "quests": [
                {
                    "title": "Complete a 20-minute walk",
                    "description": "Head outside for a brisk 20-minute walk. "
                                   "Focus on breathing and pace. No phone allowed.",
                    "type": "daily",
                    "difficulty": 1,
                    "skill_area": "fitness",
                    "xp_reward": 25,
                    "rationale": "Building a movement baseline is the foundation of fitness goals."
                },
                {
                    "title": "Read one chapter of a non-fiction book",
                    "description": "Pick up a non-fiction book and read one full chapter. "
                                   "Take a brief note on the key idea afterwards.",
                    "type": "daily",
                    "difficulty": 1,
                    "skill_area": "learning",
                    "xp_reward": 20,
                    "rationale": "Consistent reading compounds into significant knowledge over time."
                },
                {
                    "title": "5-minute breathing meditation",
                    "description": "Sit quietly and focus only on your breath for 5 minutes. "
                                   "Use box breathing: 4 counts in, hold 4, out 4, hold 4.",
                    "type": "daily",
                    "difficulty": 1,
                    "skill_area": "mindfulness",
                    "xp_reward": 15,
                    "rationale": "Short daily mindfulness sessions reduce stress and improve focus."
                }
            ]
        })


def _get_llm():
    if not settings.openai_api_key or settings.app_env == "test":
        logger.warning("OPENAI_API_KEY not set — using mock LLM")
        return MockLLM()

    from langchain_openai import ChatOpenAI
    return ChatOpenAI(
        model="gpt-4o",
        temperature=0.7,
        api_key=settings.openai_api_key,
    )


async def run_quest_chain(
    ctx: "UserContext",
    memories: list,
    provider_config: ProviderConfig | None = None
) -> dict:
    """
    Runs the full generation pipeline using whichever provider is configured.
    Falls back to mock if no config provided.
    """
    prompt_text = _build_prompt_text(ctx, memories)
    provider=build_provider(provider_config)
    system_prompt, user_prompt=_split_prompt(prompt_text)

    for attempt in range(1, 3):  # max 2 attempts
        try:
            raw=await provider.complete(system_prompt, user_prompt)
            parsed=json.loads(raw)
            _validate_quests(parsed)
            return parsed
        except (json.JSONDecodeError, ValueError, KeyError) as e:
            if attempt == 2:
                logger.error(
                    "quest chain failed after 2 attempts: %s",
                    e
                )
                raise
            logger.warning("attempt %d failed: %s — retrying", attempt, e)
    raise RuntimeError("quest generation failed")

def _validate_quests(parsed: dict) -> None:
    """Raises ValueError if the LLM output does not match expected schema."""
    quests = parsed.get("quests")
    if not isinstance(quests, list) or len(quests) == 0:
        raise ValueError("missing or empty quests array")

    required = {"title", "description", "type", "difficulty",
                "skill_area", "xp_reward"}
    for q in quests:
        missing = required - set(q.keys())
        if missing:
            raise ValueError(f"quest missing fields: {missing}")
        if q["type"] not in ("daily", "weekly"):
            raise ValueError(f"invalid quest type: {q['type']}")
        if not (1 <= q["difficulty"] <= 5):
            raise ValueError(f"difficulty out of range: {q['difficulty']}")
        if not (15 <= q["xp_reward"] <= 150):
            raise ValueError(f"xp_reward out of range: {q['xp_reward']}")

def _split_prompt(full_prompt: str) -> tuple[str, str]:
    """
    Splits the monolithic prompt file into system + user parts.
    The prompt file uses --- as a separator between system and user sections.
    """
    if "---USER---" in full_prompt:
        parts=full_prompt.split("---USER---",1)
        return parts[0].strip(), parts[1].strip()
    return full_prompt,"Generate Quests Now."
    