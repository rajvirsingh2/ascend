package workers

import (
	"context"
	"log/slog"

	"ascend-backend/internal/events"
	"ascend-backend/internal/game"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/redis/go-redis/v9"
)

func RunXPWorker(ctx context.Context, rdb *redis.Client, db *pgxpool.Pool, pub *events.Publisher) {
	questConsumer := events.NewConsumer(
		rdb,
		events.StreamQuestCompleted,
		events.GroupXPWorker,
		"xp-worker-1",
		func(ctx context.Context, e events.Event) error {
			return handleQuestXP(ctx, db, pub, e)
		},
	)

	habitConsumer := events.NewConsumer(
		rdb,
		events.StreamHabitCompleted,
		events.GroupXPWorker,
		"xp-worker-1",
		func(ctx context.Context, e events.Event) error {
			return handleHabitXP(ctx, db, pub, e)
		},
	)

	if err := questConsumer.EnsureGroup(ctx); err != nil {
		slog.Error("ensure group failed", "error", err)
		return
	}
	if err := habitConsumer.EnsureGroup(ctx); err != nil {
		slog.Error("ensure group failed", "error", err)
		return
	}

	go habitConsumer.Run(ctx)
	questConsumer.Run(ctx) // blocks
}

func handleQuestXP(ctx context.Context, db *pgxpool.Pool, pub *events.Publisher, e events.Event) error {
	xpReward, _ := e.Payload["xp_reward"].(float64)
	questID, _ := e.Payload["quest_id"].(string)

	result, err := game.AwardXP(
		ctx, db, e.UserID, "quest", questID, "quest_completed", int(xpReward),
	)
	if err != nil {
		return err
	}

	slog.Info("XP awarded", "user_id", e.UserID, "xp", result.XPAwarded,
		"leveled_up", result.LeveledUp)

	if result.LeveledUp {
		pub.Publish(ctx, events.StreamUserLeveledUp, events.Event{
			UserID: e.UserID,
			Type:   "UserLeveledUp",
			Payload: map[string]any{
				"new_level":  result.LevelAfter,
				"xp_awarded": result.XPAwarded,
				"xp_after":   result.XPAfter,
			},
		})
	}
	return nil
}

func handleHabitXP(ctx context.Context, db *pgxpool.Pool, pub *events.Publisher, e events.Event) error {
	xpReward, _ := e.Payload["xp_reward"].(float64)
	habitID, _ := e.Payload["habit_id"].(string)

	result, err := game.AwardXP(
		ctx, db, e.UserID, "habit", habitID, "habit_completed", int(xpReward),
	)
	if err != nil {
		return err
	}

	slog.Info("habit XP awarded", "user_id", e.UserID, "xp", result.XPAwarded)
	return nil
}
