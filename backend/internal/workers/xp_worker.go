package workers

import (
	"context"
	"encoding/json"
	"log/slog"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/rajvirsingh2/ascend-backend/internal/game"
	kafkapkg "github.com/rajvirsingh2/ascend-backend/internal/kafka"
	"github.com/segmentio/kafka-go"
)

type XPWorker struct {
	reader   *kafka.Reader
	db       *pgxpool.Pool
	producer *kafkapkg.Producer
}

func NewXPWorker(brokers []string, db *pgxpool.Pool, p *kafkapkg.Producer) *XPWorker {
	return &XPWorker{
		db:       db,
		producer: p,
		reader: kafka.NewReader(kafka.ReaderConfig{
			Brokers:        brokers,
			Topic:          kafkapkg.TopicQuestCompleted,
			GroupID:        "xp-calculator",
			MinBytes:       1,
			MaxBytes:       1e6,
			CommitInterval: 0, // manual commit — only after XP write succeeds
		}),
	}
}

func (w *XPWorker) Run(ctx context.Context) {
	slog.Info("XP worker started")
	for {
		msg, err := w.reader.FetchMessage(ctx)
		if err != nil {
			if ctx.Err() != nil {
				return // shutdown
			}
			slog.Error("xp worker fetch error", "error", err)
			continue
		}

		var e kafkapkg.Event
		if err := json.Unmarshal(msg.Value, &e); err != nil {
			slog.Error("xp worker unmarshal error", "error", err)
			w.reader.CommitMessages(ctx, msg) // skip bad message
			continue
		}

		xpReward, _ := e.Payload["xp_reward"].(float64)
		questID, _ := e.Payload["quest_id"].(string)

		result, err := game.AwardXP(
			ctx, w.db, e.UserID, "quest", questID,
			"quest_completed", int(xpReward),
		)
		if err != nil {
			slog.Error("xp award failed", "user_id", e.UserID, "error", err)
			continue // do NOT commit — message will be redelivered
		}

		// commit only after successful XP write
		w.reader.CommitMessages(ctx, msg)

		slog.Info("XP awarded",
			"user_id", e.UserID,
			"xp", result.XPAwarded,
			"leveled_up", result.LeveledUp,
		)

		// if level-up, publish downstream event
		if result.LeveledUp {
			w.producer.Publish(ctx, kafkapkg.TopicUserLeveledUp, kafkapkg.Event{
				UserID: e.UserID,
				Type:   "UserLeveledUp",
				Payload: map[string]any{
					"new_level":  result.LevelAfter,
					"xp_awarded": result.XPAwarded,
					"xp_after":   result.XPAfter,
				},
			})
		}
	}
}
