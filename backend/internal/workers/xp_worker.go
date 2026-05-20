package workers

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"time"

	"ascend-backend/internal/achievements"
	"ascend-backend/internal/notifications"
	pgstore "ascend-backend/internal/store/postgres"
	"ascend-backend/pkg/config"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/redis/go-redis/v9"
)

// XPEvent is pushed onto the Redis queue by quest/habit completion handlers.
type XPEvent struct {
	UserID    string    `json:"user_id"`
	Amount    int       `json:"amount"`
	Source    string    `json:"source"` // "quest" | "habit" | "physique"
	SourceID  string    `json:"source_id"`
	CreatedAt time.Time `json:"created_at"`
}

// PunishmentEvent is pushed when a compulsory quest/habit expires unfinished.
type PunishmentEvent struct {
	UserID string `json:"user_id"`
	HPLoss int    `json:"hp_loss"`
	Reason string `json:"reason"`
}

const (
	xpQueueKey         = "ascend:queue:xp"
	punishmentQueueKey = "ascend:queue:punishment"
	xpLogKeyPrefix     = "ascend:xplog:" // ascend:xplog:<user_id> → list of last 100 events
	workerBlockTimeout = 5 * time.Second
)

// XPWorkerConfig holds all deps the worker needs.
type XPWorkerConfig struct {
	Redis    *redis.Client
	DB       *pgxpool.Pool
	Notifier *notifications.FCMNotifier // may be nil in dev
	Config   *config.Config
}

// RunXPWorker blocks forever, draining the XP and Punishment queues.
// Call as: go RunXPWorker(ctx, cfg)
func RunXPWorker(ctx context.Context, cfg XPWorkerConfig) {
	log.Println("[xp-worker] starting")

	// Run XP queue and punishment queue in parallel.
	go runPunishmentWorker(ctx, cfg)

	for {
		select {
		case <-ctx.Done():
			log.Println("[xp-worker] shutting down")
			return
		default:
		}

		// BLPOP blocks up to workerBlockTimeout waiting for an event.
		results, err := cfg.Redis.BLPop(ctx, workerBlockTimeout, xpQueueKey).Result()
		if err != nil {
			if err == redis.Nil {
				// Timeout — no events, loop.
				continue
			}
			log.Printf("[xp-worker] BLPop error: %v", err)
			time.Sleep(2 * time.Second)
			continue
		}

		// results[0] = key name, results[1] = payload
		if len(results) < 2 {
			continue
		}

		var event XPEvent
		if err := json.Unmarshal([]byte(results[1]), &event); err != nil {
			log.Printf("[xp-worker] bad XP event JSON: %v", err)
			continue
		}

		if err := processXPEvent(ctx, cfg, event); err != nil {
			log.Printf("[xp-worker] processXPEvent error (user=%s): %v", event.UserID, err)
			// Re-queue with backoff instead of dropping.
			requeueWithDelay(ctx, cfg.Redis, xpQueueKey, results[1], 30*time.Second)
		}
	}
}

// processXPEvent applies XP to the user, persists an XP log entry,
// checks for level-up, evaluates achievements, and sends a push notification.
func processXPEvent(ctx context.Context, cfg XPWorkerConfig, event XPEvent) error {
	// 1. Award XP in DB (atomic UPDATE … RETURNING).
	userStore := pgstore.NewUserStore(cfg.DB)
	newXP, newLevel, didLevelUp, err := userStore.AwardXP(ctx, event.UserID, event.Amount)
	if err != nil {
		return fmt.Errorf("AwardXP: %w", err)
	}

	// 2. Persist XP log entry (capped list in Redis — fast reads for history screen).
	logEntry, _ := json.Marshal(map[string]any{
		"amount":     event.Amount,
		"source":     event.Source,
		"source_id":  event.SourceID,
		"total_xp":   newXP,
		"level":      newLevel,
		"created_at": event.CreatedAt,
	})
	pipe := cfg.Redis.Pipeline()
	pipe.LPush(ctx, xpLogKeyPrefix+event.UserID, string(logEntry))
	pipe.LTrim(ctx, xpLogKeyPrefix+event.UserID, 0, 99) // keep last 100
	if _, err := pipe.Exec(ctx); err != nil {
		// Non-fatal — log and continue.
		log.Printf("[xp-worker] redis log error: %v", err)
	}

	// 3. Also persist to postgres xp_events table for long-term history.
	xpEventStore := pgstore.NewXPEventStore(cfg.DB)
	if err := xpEventStore.Insert(ctx, pgstore.XPEventRow{
		UserID:    event.UserID,
		Amount:    event.Amount,
		Source:    event.Source,
		SourceID:  event.SourceID,
		CreatedAt: event.CreatedAt,
	}); err != nil {
		log.Printf("[xp-worker] xp_events insert error: %v", err)
	}

	// 4. Check and award achievements.
	awarded, err := achievements.CheckAndAward(ctx, cfg.DB, event.UserID, newXP, newLevel)
	if err != nil {
		log.Printf("[xp-worker] achievement check error: %v", err)
	}

	// 5. Push notification — level-up or XP gain.
	if cfg.Notifier != nil {
		if didLevelUp {
			_ = cfg.Notifier.SendToUser(ctx, event.UserID, notifications.Notification{
				Title: "⬆ LEVEL UP!",
				Body:  fmt.Sprintf("You reached Level %d. Keep ascending!", newLevel),
				Data:  map[string]string{"type": "level_up", "level": fmt.Sprint(newLevel)},
			})
		} else {
			_ = cfg.Notifier.SendToUser(ctx, event.UserID, notifications.Notification{
				Title: fmt.Sprintf("+%d XP", event.Amount),
				Body:  fmt.Sprintf("Quest complete. Total XP: %d", newXP),
				Data:  map[string]string{"type": "xp_gain"},
			})
		}

		// Notify each newly awarded achievement.
		for _, ach := range awarded {
			_ = cfg.Notifier.SendToUser(ctx, event.UserID, notifications.Notification{
				Title: "🏆 Achievement Unlocked!",
				Body:  fmt.Sprintf("%s — %s", ach.Title, ach.Description),
				Data:  map[string]string{"type": "achievement", "id": ach.Key},
			})
		}
	}

	log.Printf("[xp-worker] user=%s +%d XP → total=%d level=%d level_up=%v achievements=%d",
		event.UserID, event.Amount, newXP, newLevel, didLevelUp, len(awarded))
	return nil
}

// runPunishmentWorker deducts HP when compulsory quests/habits expire unfinished.
func runPunishmentWorker(ctx context.Context, cfg XPWorkerConfig) {
	log.Println("[punishment-worker] starting")
	for {
		select {
		case <-ctx.Done():
			return
		default:
		}

		results, err := cfg.Redis.BLPop(ctx, workerBlockTimeout, punishmentQueueKey).Result()
		if err != nil {
			if err == redis.Nil {
				continue
			}
			log.Printf("[punishment-worker] BLPop error: %v", err)
			time.Sleep(2 * time.Second)
			continue
		}
		if len(results) < 2 {
			continue
		}

		var event PunishmentEvent
		if err := json.Unmarshal([]byte(results[1]), &event); err != nil {
			log.Printf("[punishment-worker] bad JSON: %v", err)
			continue
		}

		if err := processPunishment(ctx, cfg, event); err != nil {
			log.Printf("[punishment-worker] error: %v", err)
		}
	}
}

func processPunishment(ctx context.Context, cfg XPWorkerConfig, event PunishmentEvent) error {
	userStore := pgstore.NewUserStore(cfg.DB)
	newHP, err := userStore.DeductHP(ctx, event.UserID, event.HPLoss)
	if err != nil {
		return fmt.Errorf("DeductHP: %w", err)
	}

	if cfg.Notifier != nil {
		body := fmt.Sprintf("-%d HP for: %s. Current HP: %d/100", event.HPLoss, event.Reason, newHP)
		if newHP <= 20 {
			body += " ⚠ Critical HP! Complete quests to recover."
		}
		_ = cfg.Notifier.SendToUser(ctx, event.UserID, notifications.Notification{
			Title: "❤ HP Deducted",
			Body:  body,
			Data:  map[string]string{"type": "hp_loss", "hp": fmt.Sprint(newHP)},
		})
	}

	log.Printf("[punishment-worker] user=%s -%dHP → HP=%d reason=%s",
		event.UserID, event.HPLoss, newHP, event.Reason)
	return nil
}

// EnqueueXP pushes an XP event onto the Redis queue.
// Call this from quest and habit completion handlers.
func EnqueueXP(ctx context.Context, rdb *redis.Client, event XPEvent) error {
	event.CreatedAt = time.Now().UTC()
	b, err := json.Marshal(event)
	if err != nil {
		return err
	}
	return rdb.RPush(ctx, xpQueueKey, string(b)).Err()
}

// EnqueuePunishment pushes a punishment event onto the Redis queue.
// Call this from the expiry worker when a compulsory quest/habit expires.
func EnqueuePunishment(ctx context.Context, rdb *redis.Client, event PunishmentEvent) error {
	b, err := json.Marshal(event)
	if err != nil {
		return err
	}
	return rdb.RPush(ctx, punishmentQueueKey, string(b)).Err()
}

// requeueWithDelay re-pushes a failed event after a delay.
func requeueWithDelay(ctx context.Context, rdb *redis.Client, key, payload string, delay time.Duration) {
	go func() {
		time.Sleep(delay)
		if err := rdb.RPush(ctx, key, payload).Err(); err != nil {
			log.Printf("[xp-worker] requeue error: %v", err)
		}
	}()
}
