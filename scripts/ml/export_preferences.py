"""
Phase 5: Export preference pairs from Postgres → DPO training format
Run weekly when enough new pairs exist (~1000+ recommended).

Output: data/preferences.jsonl
"""

import json, os, argparse
import psycopg2
import psycopg2.extras

SYSTEM_PROMPT = (
    "You are the Ascend Quest Master — the AI engine behind a gamified personal development RPG.\n\n"
    "Given a player profile, output exactly 3 personalized quests as a JSON array.\n\n"
    "Rules:\n"
    "- Match quest difficulty exactly to the player's RANK (E easiest → S hardest)\n"
    "- Quests must directly serve the player's stated GOAL and primary interest\n"
    "- Every quest must be completable in 1-2 days maximum\n"
    "- Output ONLY a valid JSON array. No markdown. No explanation. No preamble.\n"
    '- Schema: [{"title":str,"description":str,"difficulty":str,"xp_reward":int,'
    '"skill_area":str,"subcategory":str,"quest_type":str,"duration_minutes":int}]'
)

def profile_to_prompt(p):
    interests_str = "\n".join(
        f"  - {i['category']} / {i['subcategory']} ({i['priority']})"
        for i in p.get("interests", [])
    )
    physique_str = ""
    if p.get("physique"):
        ph = p["physique"]
        physique_str = f"\nPhysique: BMI {ph.get('bmi','?')}, Goal: {ph.get('goal','?')}, Activity: {ph.get('activity_level','?')}"
    return (
        f"Generate 3 personalized quests for this Ascend player:\n\n"
        f"Player Profile:\n"
        f"- Level: {p.get('level',1)} | Rank: {p.get('rank','E')}\n"
        f"- Archetype: {p.get('archetype','casual')}\n"
        f"- Streak: {p.get('current_streak',0)} days\n"
        f"- Quests completed: {p.get('quests_completed',0)}\n\n"
        f"Interests:\n{interests_str}\n\n"
        f"Goal: \"{p.get('goal','Improve myself')}\"{physique_str}\n\n"
        f"Generate quests appropriate for Rank {p.get('rank','E')} difficulty."
    )

def build_chat_format(profile, quest):
    """DPO needs same input prompt + different responses for chosen/rejected"""
    return {
        "prompt": (
            f"<|system|>\n{SYSTEM_PROMPT}<|end|>\n"
            f"<|user|>\n{profile_to_prompt(profile)}<|end|>\n"
            f"<|assistant|>\n"
        ),
        "response": json.dumps([quest])  # wrap single quest as array
    }

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--db",  default=os.environ.get("DATABASE_URL"))
    parser.add_argument("--out", default="data/preferences.jsonl")
    parser.add_argument("--min-pairs", type=int, default=500)
    args = parser.parse_args()

    os.makedirs(os.path.dirname(args.out), exist_ok=True)

    conn = psycopg2.connect(args.db)
    cur = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)

    cur.execute("""
        SELECT profile_snapshot, chosen_quest, rejected_quest
        FROM dpo_preference_pairs
        ORDER BY pair_date DESC
        LIMIT 50000
    """)

    rows = cur.fetchall()
    print(f"Found {len(rows)} preference pairs")

    if len(rows) < args.min_pairs:
        print(f"⚠️  Not enough pairs (need {args.min_pairs}+). Skip DPO this round.")
        return

    written = 0
    with open(args.out, "w", encoding='utf-8') as f:
        for row in rows:
            profile  = row["profile_snapshot"]
            chosen   = row["chosen_quest"]
            rejected = row["rejected_quest"]

            chosen_fmt   = build_chat_format(profile, chosen)
            rejected_fmt = build_chat_format(profile, rejected)

            # DPO format: prompt + chosen + rejected
            f.write(json.dumps({
                "prompt":   chosen_fmt["prompt"],
                "chosen":   chosen_fmt["response"],
                "rejected": rejected_fmt["response"]
            }, ensure_ascii=False) + "\n")
            written += 1

    print(f"✅ Wrote {written} pairs → {args.out}")

if __name__ == "__main__":
    main()
