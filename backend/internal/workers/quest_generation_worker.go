package workers

import (
	"context"
	"fmt"
	"log/slog"
	"strconv"
	"strings"
	"time"

	"ascend-backend/internal/interests"
	"ascend-backend/internal/mlservice"
	"ascend-backend/internal/notifications"
	pgstore "ascend-backend/internal/store/postgres"

	"github.com/jackc/pgx/v5/pgxpool"
)

type QuestGenerationWorkerConfig struct {
	DB             *pgxpool.Pool
	MLClient       *mlservice.Client // may be nil — seeded fallback still runs
	InterestsStore *interests.Store
	Notifier       *notifications.FCMNotifier // may be nil
	Loc            *time.Location
}

// seedQuest is a curated default quest used when the ML model is
// unavailable or fails. Users must always receive quests at midnight.
type seedQuest struct {
	Title       string
	Description string
	SkillArea   string
	Difficulty  int
	XPReward    int
}

var seededDailyQuests = []seedQuest{
	{"Morning Movement", "Do 20 minutes of any physical activity.", "Fitness", 2, 50},
	{"Deep Work Block", "Complete one 45-minute focused session, no distractions.", "Productivity", 3, 75},
	{"Read & Reflect", "Read 15 pages of any book and note one takeaway.", "Learning", 2, 50},
	{"Hydration Protocol", "Drink at least 8 glasses of water today.", "Health", 1, 30},
	{"Evening Reset", "Tidy your workspace and plan tomorrow's top 3 tasks.", "Discipline", 1, 30},
}

func StartQuestGenerationWorker(ctx context.Context, cfg QuestGenerationWorkerConfig) {
	slog.Info("[quest-generation-worker] starting midnight cron")

	loc := cfg.Loc
	if loc == nil {
		loc = time.UTC
	}

	// Time until next midnight in the configured timezone.
	timeUntilMidnight := func() time.Duration {
		now := time.Now().In(loc)
		next := time.Date(now.Year(), now.Month(), now.Day()+1, 0, 0, 0, 0, loc)
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
			slog.Info("[quest-generation-worker] midnight trigger reached, starting quest generation")
			runGenerationJob(ctx, cfg, loc)
		}
	}
}

func runGenerationJob(ctx context.Context, cfg QuestGenerationWorkerConfig, loc *time.Location) {
	rows, err := cfg.DB.Query(ctx, `SELECT id FROM users`)
	if err != nil {
		slog.Error("[quest-generation-worker] failed to fetch users", "error", err)
		return
	}
	var userIDs []string
	for rows.Next() {
		var id string
		if err := rows.Scan(&id); err == nil {
			userIDs = append(userIDs, id)
		}
	}
	rows.Close()

	slog.Info("[quest-generation-worker] starting generation", "user_count", len(userIDs))

	notifStore := pgstore.NewNotificationStore(cfg.DB)

	// Weekday must be evaluated in the app timezone, not the container's
	// (midnight IST is still the previous day in UTC).
	isMonday := time.Now().In(loc).Weekday() == time.Monday

	for _, userID := range userIDs {
		if ctx.Err() != nil {
			return
		}

		// Idempotency guard: skip users who already received today's drop
		// (e.g. the process restarted right after midnight).
		var alreadyToday int
		_ = cfg.DB.QueryRow(ctx,
			`SELECT COUNT(*) FROM quests
			 WHERE user_id=$1 AND is_ai_generated=true AND created_at >= $2`,
			userID, startOfDay(loc),
		).Scan(&alreadyToday)
		if alreadyToday > 0 {
			continue
		}

		inserted := generateForUser(ctx, cfg, userID, isMonday)
		if inserted == 0 {
			slog.Warn("[quest-generation-worker] no quests generated", "user_id", userID)
			continue
		}

		notifyQuestDrop(ctx, cfg, notifStore, userID, inserted)
		slog.Info("[quest-generation-worker] success", "user_id", userID, "count", inserted)
	}
}

func startOfDay(loc *time.Location) time.Time {
	now := time.Now().In(loc)
	return time.Date(now.Year(), now.Month(), now.Day(), 0, 0, 0, 0, loc)
}

// generateForUser tries the ML model first and falls back to seeded quests so
// the midnight drop never silently produces nothing.
func generateForUser(ctx context.Context, cfg QuestGenerationWorkerConfig, userID string, isMonday bool) int {
	if cfg.MLClient != nil {
		if n := generateViaML(ctx, cfg, userID, isMonday); n > 0 {
			return n
		}
		slog.Warn("[quest-generation-worker] ML path produced nothing, using seeded quests", "user_id", userID)
	}
	return insertSeededQuests(ctx, cfg, userID)
}

func generateViaML(ctx context.Context, cfg QuestGenerationWorkerConfig, userID string, isMonday bool) int {
	var userInterests []interests.UserInterest
	if cfg.InterestsStore != nil {
		userInterests, _ = cfg.InterestsStore.GetByUser(ctx, userID)
	}

	profile := mlservice.UserProfile{RequestedDaily: 5}
	if isMonday {
		profile.RequestedWeekly = 2
	}

	_ = cfg.DB.QueryRow(ctx, `SELECT level FROM users WHERE id=$1`, userID).Scan(&profile.Level)
	_ = cfg.DB.QueryRow(ctx, `SELECT COUNT(*) FROM quests WHERE user_id=$1 AND status='completed'`, userID).Scan(&profile.QuestsCompleted)
	_ = cfg.DB.QueryRow(ctx, `SELECT COALESCE(MAX(current_streak), 0) FROM habits WHERE user_id=$1 AND is_active=true`, userID).Scan(&profile.CurrentStreak)
	_ = cfg.DB.QueryRow(ctx, `SELECT rank, archetype FROM users WHERE id=$1`, userID).Scan(&profile.Rank, &profile.Archetype)
	_ = cfg.DB.QueryRow(ctx, `SELECT title FROM goals WHERE user_id=$1 AND status='active' LIMIT 1`, userID).Scan(&profile.Goal)

	for _, ui := range userInterests {
		profile.Interests = append(profile.Interests, mlservice.Interest{
			Category:    ui.Category,
			Subcategory: ui.Subcategory,
			Priority:    strconv.Itoa(ui.Priority),
		})
	}

	// The HF Space can cold-start at midnight; bound the call.
	mlCtx, cancel := context.WithTimeout(ctx, 10*time.Minute)
	defer cancel()
	mlQuests, err := cfg.MLClient.GenerateQuests(mlCtx, profile)
	if err != nil {
		slog.Error("[quest-generation-worker] ml generation failed", "user_id", userID, "error", err)
		return 0
	}

	existing := existingTitles(ctx, cfg.DB, userID)
	inserted := 0
	for _, q := range mlQuests {
		key := strings.ToLower(strings.TrimSpace(q.Title))
		if existing[key] {
			continue
		}
		existing[key] = true

		questType := strings.ToLower(q.QuestType)
		if questType != "daily" && questType != "weekly" {
			questType = "daily"
		}
		expires := time.Now().Add(24 * time.Hour)
		if questType == "weekly" {
			expires = time.Now().Add(7 * 24 * time.Hour)
		}

		if insertQuest(ctx, cfg.DB, userID, q.Title, q.Description, questType,
			mlservice.MapDifficulty(q.Difficulty), q.XPReward, q.SkillArea, expires) {
			inserted++
		}
	}
	return inserted
}

func insertSeededQuests(ctx context.Context, cfg QuestGenerationWorkerConfig, userID string) int {
	existing := existingTitles(ctx, cfg.DB, userID)
	expires := time.Now().Add(24 * time.Hour)
	inserted := 0
	for _, s := range seededDailyQuests {
		if existing[strings.ToLower(s.Title)] {
			continue
		}
		if insertQuest(ctx, cfg.DB, userID, s.Title, s.Description, "daily",
			s.Difficulty, s.XPReward, s.SkillArea, expires) {
			inserted++
		}
	}
	return inserted
}

func existingTitles(ctx context.Context, db *pgxpool.Pool, userID string) map[string]bool {
	titles := make(map[string]bool)
	rows, err := db.Query(ctx, `SELECT title FROM quests WHERE user_id=$1`, userID)
	if err != nil {
		return titles
	}
	defer rows.Close()
	for rows.Next() {
		var t string
		if rows.Scan(&t) == nil {
			titles[strings.ToLower(strings.TrimSpace(t))] = true
		}
	}
	return titles
}

func insertQuest(ctx context.Context, db *pgxpool.Pool, userID, title, description, questType string,
	difficulty, xpReward int, skillArea string, expires time.Time) bool {
	_, err := db.Exec(ctx,
		`INSERT INTO quests
		   (id, user_id, title, description, type, difficulty, xp_reward,
		    status, is_ai_generated, skill_area, expires_at, created_at)
		 VALUES (gen_random_uuid(),$1,$2,$3,$4,$5,$6,'active',true,$7,$8,NOW())`,
		userID, title, description, questType, difficulty, xpReward, skillArea, expires,
	)
	if err != nil {
		slog.Error("[quest-generation-worker] insert failed", "user_id", userID, "error", err)
		return false
	}
	return true
}

// notifyQuestDrop announces the midnight drop via the shared delivery path
// (in-app notification row + FCM; the app refreshes its quest cache when it
// receives the DAILY_QUEST data message).
func notifyQuestDrop(ctx context.Context, cfg QuestGenerationWorkerConfig, store *pgstore.NotificationStore, userID string, count int) {
	notifications.Deliver(ctx, store, cfg.Notifier, userID,
		"DAILY_QUEST",
		"⚔ Daily Quests Dropped!",
		fmt.Sprintf("The System has issued %d new missions. Complete them to earn XP.", count),
		nil, "dashboard",
		map[string]string{"count": strconv.Itoa(count)})
}
