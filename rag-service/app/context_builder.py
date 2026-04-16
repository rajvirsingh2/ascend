import json
import logging
from dataclasses import dataclass

from app.database import get_conn

logger = logging.getLogger(__name__)


@dataclass
class UserContext:
    user_id: str
    username: str
    level: int
    skills: list[dict]       # [{skill_name, skill_level, xp_in_skill}]
    active_goals: list[dict] # [{id, title, skill_area, priority, progress}]
    generate_for: str        # "daily" | "weekly"


async def build_user_context(user_id: str, generate_for: str = "daily") -> UserContext:
    """
    Assembles everything the LLM needs about the user in a single DB round-trip.
    """
    async with get_conn() as conn:
        # user profile
        cur_user = await conn.execute(
            "SELECT username, level FROM users WHERE id = %s",
            (user_id,)
        )
        user_row = await cur_user.fetchone()
        if not user_row:
            raise ValueError(f"user {user_id} not found")

        # skills
        cur_skills = await conn.execute(
            """SELECT skill_name, skill_level, xp_in_skill
               FROM user_skills WHERE user_id = %s
               ORDER BY skill_level DESC""",
            (user_id,)
        )
        skill_rows = await cur_skills.fetchall()

        # active goals (top 5 by priority)
        cur_goals = await conn.execute(
            """SELECT id, title, skill_area, priority, progress
               FROM goals
               WHERE user_id = %s AND status = 'active'
               ORDER BY priority DESC, created_at DESC
               LIMIT 5""",
            (user_id,)
        )
        goal_rows = await cur_goals.fetchall()

    return UserContext(
        user_id=user_id,
        username=user_row[0],
        level=user_row[1],
        skills=[
            {"skill_name": r[0], "skill_level": r[1], "xp_in_skill": r[2]}
            for r in (skill_rows or [])
        ],
        active_goals=[
            {"id": r[0], "title": r[1], "skill_area": r[2],
             "priority": r[3], "progress": r[4]}
            for r in (goal_rows or [])
        ],
        generate_for=generate_for,
    )


def format_user_context_for_prompt(ctx: UserContext) -> str:
    """Converts UserContext into readable text for the system prompt."""
    lines = [
        f"Username: {ctx.username}",
        f"Level: {ctx.level}",
        f"Generating: {ctx.generate_for} quests",
    ]

    if ctx.skills:
        skill_text = ", ".join(
            f"{s['skill_name']} (level {s['skill_level']})"
            for s in ctx.skills
        )
        lines.append(f"Skills: {skill_text}")

    if ctx.active_goals:
        lines.append("Active goals:")
        for g in ctx.active_goals:
            lines.append(
                f"  - '{g['title']}' [{g['skill_area']}] "
                f"{g['progress']}% complete"
            )

    return "\n".join(lines)