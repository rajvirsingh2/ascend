from dataclasses import dataclass
from datetime import datetime
from typing import Any


@dataclass
class MemoryDocument:
    """A single document ready to be embedded and stored in user_memories."""
    user_id: str
    doc_type: str          # quest_history | goal | habit_pattern
    entity_id: str
    content: str           # the text that gets embedded
    metadata: dict[str, Any]


def build_quest_document(quest: dict, user_id: str) -> MemoryDocument:
    """
    Converts a quest completion/skip event into an embeddable document.
    Prose beats JSON for embedding quality — describe what happened.
    """
    status = quest.get("status", "unknown")
    outcome = (
        "completed successfully"
        if status == "completed"
        else f"was skipped or abandoned"
    )

    difficulty_label = {1: "very easy", 2: "easy", 3: "moderate",
                        4: "hard", 5: "very hard"}.get(quest.get("difficulty", 1), "moderate")

    content = (
        f"Quest titled '{quest['title']}' in the {quest.get('skill_area', 'general')} "
        f"skill area {outcome}. "
        f"It was a {difficulty_label} {quest.get('type', 'daily')} quest "
        f"worth {quest.get('xp_reward', 0)} XP. "
        f"This quest was {'AI-generated' if quest.get('is_ai_generated') else 'a standard quest'}."
    )

    if quest.get("goal_title"):
        content += f" It was part of the goal: '{quest['goal_title']}'."

    return MemoryDocument(
        user_id=user_id,
        doc_type="quest_history",
        entity_id=quest["id"],
        content=content,
        metadata={
            "status": status,
            "skill_area": quest.get("skill_area", "general"),
            "difficulty": quest.get("difficulty", 1),
            "type": quest.get("type", "daily"),
            "xp_reward": quest.get("xp_reward", 0),
            "is_ai_generated": quest.get("is_ai_generated", False),
        },
    )


def build_goal_document(goal: dict, user_id: str) -> MemoryDocument:
    """Converts a goal into an embeddable document."""
    days_remaining = None
    if goal.get("target_date"):
        try:
            target = datetime.fromisoformat(str(goal["target_date"]))
            days_remaining = (target - datetime.now()).days
        except (ValueError, TypeError):
            pass

    urgency = (
        f"The target date is {days_remaining} days away."
        if days_remaining is not None
        else "No deadline has been set."
    )

    priority_label = {1: "low", 2: "medium", 3: "high"}.get(
        goal.get("priority", 2), "medium"
    )

    content = (
        f"User goal: '{goal['title']}'. "
        f"Category: {goal.get('category', 'general')}, "
        f"skill area: {goal.get('skill_area', 'general')}. "
        f"Priority: {priority_label}. "
        f"Current progress: {goal.get('progress', 0)}%. "
        f"{urgency}"
    )

    if goal.get("description"):
        content += f" Description: {goal['description']}"

    return MemoryDocument(
        user_id=user_id,
        doc_type="goal",
        entity_id=goal["id"],
        content=content,
        metadata={
            "skill_area": goal.get("skill_area", "general"),
            "priority": goal.get("priority", 2),
            "status": goal.get("status", "active"),
            "progress": goal.get("progress", 0),
        },
    )


def build_habit_document(habit: dict, user_id: str) -> MemoryDocument:
    """Converts a habit streak milestone into an embeddable document."""
    streak = habit.get("current_streak", 0)
    content = (
        f"Habit: '{habit['title']}'. "
        f"Frequency: {habit.get('frequency', 'daily')}. "
        f"Current streak: {streak} days. "
        f"Longest streak: {habit.get('longest_streak', 0)} days. "
        f"XP reward per completion: {habit.get('xp_reward', 10)}."
    )

    return MemoryDocument(
        user_id=user_id,
        doc_type="habit_pattern",
        entity_id=habit["id"],
        content=content,
        metadata={
            "frequency": habit.get("frequency", "daily"),
            "current_streak": streak,
            "xp_reward": habit.get("xp_reward", 10),
        },
    )