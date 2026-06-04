package workers

import (
	"context"
	"log/slog"
	"strconv"
	"time"

	"ascend-backend/internal/interests"
	"ascend-backend/internal/mlservice"
	"ascend-backend/internal/quest"
	"github.com/jackc/pgx/v5/pgxpool"
)

type QuestGenerationWorkerConfig struct {
	DB             *pgxpool.Pool
	MLClient       *mlservice.Client
	InterestsStore *interests.Store
	QuestTracker   *quest.InteractionTracker
}

func StartQuestGenerationWorker(ctx context.Context, cfg QuestGenerationWorkerConfig) {
	slog.Info("[quest-generation-worker] starting midnight cron")

	// Helper to calculate time until next midnight
	timeUntilMidnight := func() time.Duration {
		now := time.Now()
		next := time.Date(now.Year(), now.Month(), now.Day()+1, 0, 0, 0, 0, now.Location())
		return next.Sub(now)
	}

	for {
		wait := timeUntilMidnight()
		slog.Info("[quest-generation-worker] waiting for next run", "wait_hours", wait.Hours())

		select {
		case <-ctx.Done():
			slog.Info("[quest-generation-worker] stopping")
			return
		case <-time.After(wait):
			// Midnight! Run the job
			slog.Info("[quest-generation-worker] midnight trigger reached, starting quest generation")
			runGenerationJob(ctx, cfg)
		}
	}
}

func runGenerationJob(ctx context.Context, cfg QuestGenerationWorkerConfig) {
	// 1. Fetch all active users
	rows, err := cfg.DB.Query(ctx, `SELECT id FROM users`)
	if err != nil {
		slog.Error("[quest-generation-worker] failed to fetch users", "error", err)
		return
	}
	defer rows.Close()

	var userIDs []string
	for rows.Next() {
		var id string
		if err := rows.Scan(&id); err == nil {
			userIDs = append(userIDs, id)
		}
	}
	rows.Close()

	slog.Info("[quest-generation-worker] starting generation", "user_count", len(userIDs))

	isMonday := time.Now().Weekday() == time.Monday

	// Re-use the existing GenerateHandler logic by creating a dummy one for backend use
	// We need a Redis client, but for background job we can pass nil or refactor if needed.
	// Actually, we can just call mlClient directly and persist the quests using the DB!

	for _, userID := range userIDs {
		// Respect context cancellation
		if ctx.Err() != nil {
			return
		}

		slog.Info("[quest-generation-worker] generating quests for user", "user_id", userID)

		// 1. Fetch interests
		userInterests, err := cfg.InterestsStore.GetByUser(ctx, userID)
		if err != nil {
			userInterests = []interests.UserInterest{}
		}

		// 2. Build Profile
		var level int
		_ = cfg.DB.QueryRow(ctx, `SELECT level FROM users WHERE id = $1`, userID).Scan(&level)

		var questsCompleted int
		_ = cfg.DB.QueryRow(ctx, `SELECT COUNT(*) FROM quests WHERE user_id=$1 AND status='completed'`, userID).Scan(&questsCompleted)

		var currentStreak int
		_ = cfg.DB.QueryRow(ctx, `SELECT COALESCE(MAX(current_streak), 0) FROM habits WHERE user_id=$1 AND is_active=true`, userID).Scan(&currentStreak)

		profile := mlservice.UserProfile{
			Level:           level,
			QuestsCompleted: questsCompleted,
			CurrentStreak:   currentStreak,
			RequestedDaily:  5,
		}

		if isMonday {
			profile.RequestedWeekly = 2
		}

		// We omit goal/archetype/rank fetching here for simplicity, or we can fetch them.
		// Let's fetch them!
		var rank, archetype, goal string
		_ = cfg.DB.QueryRow(ctx, `SELECT rank, archetype FROM users WHERE id=$1`, userID).Scan(&rank, &archetype)
		_ = cfg.DB.QueryRow(ctx, `SELECT title FROM goals WHERE user_id=$1 AND status='active' LIMIT 1`, userID).Scan(&goal)
		
		profile.Rank = rank
		profile.Archetype = archetype
		profile.Goal = goal

		for _, ui := range userInterests {
			profile.Interests = append(profile.Interests, mlservice.Interest{
				Category:    ui.Category,
				Subcategory: ui.Subcategory,
				Priority:    strconv.Itoa(int(ui.Priority)), // Or keep it string
			})
		}

		// 3. Call ML
		mlQuests, mlErr := cfg.MLClient.GenerateQuests(ctx, profile)
		if mlErr != nil {
			slog.Error("[quest-generation-worker] ml generation failed", "user_id", userID, "error", mlErr)
			continue
		}

		// 4. Persist
		// Since we don't have access to GenerateHandler's private persist logic, 
		// we can write a simple insert here.
		for _, q := range mlQuests {
			diff := 1 // map logic omitted
			if q.Difficulty == "Medium" { diff = 2 }
			if q.Difficulty == "Hard" { diff = 3 }
			if q.Difficulty == "Epic" { diff = 4 }
			if q.Difficulty == "Legendary" { diff = 5 }

			questType := q.QuestType
			if questType != "daily" && questType != "weekly" {
				questType = "daily"
			}

			expires := time.Now().Add(24 * time.Hour)
			if questType == "weekly" {
				expires = time.Now().Add(7 * 24 * time.Hour)
			}

			_, _ = cfg.DB.Exec(ctx,
				`INSERT INTO quests
				   (id, user_id, title, description, type, difficulty, xp_reward,
				    status, is_ai_generated, skill_area, expires_at, created_at)
				 VALUES (gen_random_uuid(),$1,$2,$3,$4,$5,$6,'active',true,$7,$8,NOW())`,
				userID, q.Title, q.Description, questType,
				diff, q.XPReward, q.SkillArea, expires,
			)
		}
		
		slog.Info("[quest-generation-worker] success", "user_id", userID, "count", len(mlQuests))
	}
}
