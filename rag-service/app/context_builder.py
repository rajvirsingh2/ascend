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
    interests: list[dict]    # [{category, subcategory, custom_goal}]
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
        
        # interests
        cur_interests = await conn.execute(
            """SELECT category, subcategory, custom_goal
               FROM user_interests
               WHERE user_id = %s
               ORDER BY priority ASC""",
            (user_id,)
        )
        interest_rows = await cur_interests.fetchall()

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
        interests=[
            {"category": r[0], "subcategory": r[1], "custom_goal": r[2]}
            for r in (interest_rows or [])
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

    if ctx.interests:
        lines.append("Interests (use these to theme the quests):")
        for i in ctx.interests:
            cg = f" (goal: {i['custom_goal']})" if i['custom_goal'] else ""
            lines.append(f"  - {i['category']} / {i['subcategory']}{cg}")

    return "\n".join(lines)

async def build_physique_context(user_id:str, db_pool)->str:
    """
    Fetches physique profile and formats it for the LLM prompt.
    """

    try:
        async with db_pool.connection() as conn:
            cur = await conn.execute(
                """
                SELECT age, sex, height_cm, weight_kg, target_weight_kg, body_goal, activity_level, fitness_level, bmi, bmr, tdee FROM physique_profiles WHERE user_id=%s
                """, (user_id,)
            )
            row = await cur.fetchone()
        if not row:
            return "No physique profile set up-generate generic quests only"
        
        age, sex, height, weight, target, goal, activity, fitness, bmi, bmr, tdee = row

        goal_labels={
            "lean_athletic": "Lean & Athletic",
            "bulky_muscular": "Bulky & Muscular",
            "powerlifter":    "Powerlifter (max strength)",
            "endurance":      "Endurance Athlete",
            "maintain":       "Maintain current physique",
            "lose_fat":       "Lose fat",
        }

        bmi_cat = "normal"
        if bmi < 18.5:   bmi_cat = "underweight"
        elif bmi < 25:   bmi_cat = "normal"
        elif bmi < 30:   bmi_cat = "overweight"
        else:            bmi_cat = "obese"

        goal_cals = tdee
        if goal == "lose_fat":          goal_cals = tdee - 500
        elif goal == "lean_athletic":   goal_cals = tdee - 200
        elif goal in ("bulky_muscular", "powerlifter"): goal_cals = tdee + 300

        return (
            f"Age: {age}, Sex: {sex}\n"
            f"Height: {height}cm, Current weight: {weight}kg\n"
            f"Target weight: {target}kg\n"
            f"BMI: {bmi} ({bmi_cat})\n"
            f"Body goal: {goal_labels.get(goal, goal)}\n"
            f"Fitness level: {fitness}\n"
            f"Activity level: {activity}\n"
            f"BMR: {bmr} kcal | TDEE: {tdee} kcal | Goal intake: {goal_cals} kcal/day\n"
        )
    except Exception as e:
        return f"Physique data unavailable: {e}"
