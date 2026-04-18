import json
from app.providers import BaseLLMProvider, ProviderConfig


class MockProvider(BaseLLMProvider):
    """Used in tests and when no API key is configured."""

    @property
    def provider_name(self) -> str:
        return "mock"

    async def complete(self, system_prompt: str, user_prompt: str) -> str:
        return json.dumps({
            "quests": [
                {
                    "title": "Complete a 20-minute walk",
                    "description": "Head outside for a brisk 20-minute walk. No phone allowed.",
                    "type": "daily", "difficulty": 1,
                    "skill_area": "fitness", "xp_reward": 25,
                    "rationale": "Building movement baseline."
                },
                {
                    "title": "Read one chapter",
                    "description": "Pick up a non-fiction book and read one full chapter.",
                    "type": "daily", "difficulty": 1,
                    "skill_area": "learning", "xp_reward": 20,
                    "rationale": "Consistent reading compounds over time."
                },
                {
                    "title": "5-minute breathing meditation",
                    "description": "Sit quietly and focus only on your breath for 5 minutes.",
                    "type": "daily", "difficulty": 1,
                    "skill_area": "mindfulness", "xp_reward": 15,
                    "rationale": "Daily mindfulness reduces stress."
                }
            ]
        })