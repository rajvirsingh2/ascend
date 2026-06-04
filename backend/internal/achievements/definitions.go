package achievements

import (
	"context"

	"github.com/jackc/pgx/v5/pgxpool"
)

type Achievement struct {
	Key         string
	Title       string
	Description string
	Tag         string // short display label shown in UI
	Icon        string // emoji-style label
}

var All = []Achievement{
	// streak achievements
	{"first_habit", "First Step", "Complete your first habit", "BEGINNER", "🌱"},
	{"streak_7", "Week Warrior", "7-day habit streak", "STREAK-7", "🔥"},
	{"streak_30", "Iron Will", "30-day habit streak", "IRON WILL", "⚡"},
	{"streak_100", "Centurion", "100-day habit streak", "CENTURION", "💎"},
	// quest achievements
	{"quest_10", "Quest Hunter", "Complete 10 quests", "HUNTER", "🗡"},
	{"quest_50", "Slayer", "Complete 50 quests", "SLAYER", "⚔"},
	{"quest_100", "Legendary", "Complete 100 quests", "LEGEND", "👑"},
	{"first_ai_quest", "AI Chosen", "Complete your first AI quest", "AI CHOSEN", "🤖"},
	{"s_rank_quest", "S-Rank Conqueror", "Complete an S-Rank quest", "S-RANK", "★"},
	// fitness
	{"runner", "Runner", "Complete 10 fitness quests", "RUNNER", "🏃"},
	{"marathon", "Marathon Mind", "Complete 50 fitness quests", "MARATHON", "🏅"},
	// learning
	{"scholar", "Scholar", "Complete 10 learning quests", "SCHOLAR", "📚"},
	{"sage", "Sage", "Complete 50 learning quests", "SAGE", "🧠"},
	// level milestones
	{"level_5", "Rising Star", "Reach level 5", "LV.5", "⭐"},
	{"level_10", "Seasoned", "Reach level 10", "LV.10", "✨"},
	{"level_25", "Ascendant", "Reach level 25", "ASCENDANT", "🌟"},
}

// CheckAndAward evaluates all achievements for a user and awards unearned ones.
// Returns a list of newly awarded achievements.
func CheckAndAward(ctx context.Context, db *pgxpool.Pool, userID string, newXP, newLevel int) ([]Achievement, error) {
	// fetch user stats in one query
	var totalQuests, fitnessQuests, learningQuests int
	var maxStreak int
	var aiQuestsCompleted, sRankCompleted bool

	_ = db.QueryRow(ctx,
		`SELECT
		   COUNT(*) FILTER (WHERE status='completed') AS total,
		   COUNT(*) FILTER (WHERE status='completed' AND skill_area='fitness') AS fitness,
		   COUNT(*) FILTER (WHERE status='completed' AND skill_area='learning') AS learning,
		   BOOL_OR(status='completed' AND is_ai_generated=true) AS ai_done,
		   BOOL_OR(status='completed' AND difficulty=5) AS s_rank_done
		 FROM quests WHERE user_id=$1`,
		userID,
	).Scan(&totalQuests, &fitnessQuests, &learningQuests, &aiQuestsCompleted, &sRankCompleted)

	_ = db.QueryRow(ctx,
		`SELECT COALESCE(MAX(longest_streak), 0) FROM habits WHERE user_id=$1`, userID,
	).Scan(&maxStreak)

	var habitsDone int
	_ = db.QueryRow(ctx,
		`SELECT COUNT(*) FROM habits WHERE user_id=$1 AND current_streak > 0`, userID,
	).Scan(&habitsDone)

	// evaluate each achievement
	earned := map[string]bool{
		"first_habit":    habitsDone >= 1,
		"streak_7":       maxStreak >= 7,
		"streak_30":      maxStreak >= 30,
		"streak_100":     maxStreak >= 100,
		"quest_10":       totalQuests >= 10,
		"quest_50":       totalQuests >= 50,
		"quest_100":      totalQuests >= 100,
		"first_ai_quest": aiQuestsCompleted,
		"s_rank_quest":   sRankCompleted,
		"runner":         fitnessQuests >= 10,
		"marathon":       fitnessQuests >= 50,
		"scholar":        learningQuests >= 10,
		"sage":           learningQuests >= 50,
		"level_5":        newLevel >= 5,
		"level_10":       newLevel >= 10,
		"level_25":       newLevel >= 25,
	}

	var newlyAwarded []Achievement

	// award any that are earned but not yet recorded
	for _, a := range All {
		if !earned[a.Key] {
			continue
		}
		tag, err := db.Exec(ctx,
			`INSERT INTO user_achievements (user_id, achievement_key, earned_at)
			 VALUES ($1, $2, NOW())
			 ON CONFLICT DO NOTHING`,
			userID, a.Key,
		)
		if err == nil && tag.RowsAffected() > 0 {
			newlyAwarded = append(newlyAwarded, a)
		}
	}
	return newlyAwarded, nil
}
