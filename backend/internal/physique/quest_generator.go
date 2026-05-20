package physique

import (
	"context"
	"fmt"
	"math/rand"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

// GenerateExerciseQuests creates physique-specific quests for a user.
func GenerateExerciseQuests(ctx context.Context, db *pgxpool.Pool, userID string) error {
	profile, metrics, err := GetProfile(ctx, db, userID)
	if err != nil {
		return fmt.Errorf("physique profile not set up: %w", err)
	}

	exercises := GetExercisesForGoal(profile.BodyGoal, profile.FitnessLevel, 3)
	if len(exercises) == 0 {
		return fmt.Errorf("no exercises found for goal: %s", profile.BodyGoal)
	}

	// add a calorie context note
	calsNote := fmt.Sprintf(
		"Goal: %d kcal/day to %s",
		metrics.GoalCalories,
		goalDescription(profile.BodyGoal),
	)

	r := rand.New(rand.NewSource(time.Now().UnixNano()))
	r.Shuffle(len(exercises), func(i, j int) { exercises[i], exercises[j] = exercises[j], exercises[i] })

	for _, ex := range exercises {
		questID := uuid.NewString()
		title := fmt.Sprintf("%s — %s sets × %s reps",
			ex["name"], fmt.Sprint(ex["sets"]), ex["reps"])
		description := fmt.Sprintf(
			"Rest %s between sets. %s\n\n%s",
			ex["rest"], ex["notes"], calsNote,
		)
		xpReward := ex["xp_reward"].(int)

		difficulty := difficultyFromFitness(profile.FitnessLevel)
		expires := time.Now().Add(24 * time.Hour)

		_, err := db.Exec(ctx,
			`INSERT INTO quests
			   (id, user_id, title, description, type, difficulty, xp_reward,
			    status, is_ai_generated, skill_area, expires_at, created_at)
			 VALUES ($1,$2,$3,$4,'daily',$5,$6,'active',false,'fitness',$7,$8)
			 ON CONFLICT DO NOTHING`,
			questID, userID, title, description,
			difficulty, xpReward, expires, time.Now(),
		)
		if err != nil {
			return fmt.Errorf("inserting exercise quest: %w", err)
		}
	}
	return nil
}

func difficultyFromFitness(level string) int {
	switch level {
	case "intermediate":
		return 3
	case "advanced":
		return 4
	default:
		return 2
	}
}

func goalDescription(goal string) string {
	m := map[string]string{
		"lean_athletic":  "build a lean, athletic physique",
		"bulky_muscular": "gain muscle mass",
		"powerlifter":    "maximise strength",
		"endurance":      "build cardiovascular endurance",
		"maintain":       "maintain your current physique",
		"lose_fat":       "burn fat",
	}
	if d, ok := m[goal]; ok {
		return d
	}
	return "reach your goal"
}
