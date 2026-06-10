// XP worker: consumes XP / punishment events from Redis and performs the
// async side effects — achievements, history logging, notifications, and
// realtime WebSocket pushes.
//
// IMPORTANT: XP, levels and HP are applied SYNCHRONOUSLY by game.AwardXP at
// request time. This worker must never write XP or HP — it only reacts to
// events that carry the already-applied result (events.XPEvent).
package workers

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"time"

	"ascend-backend/internal/achievements"
	"ascend-backend/internal/events"
	"ascend-backend/internal/notifications"
	"ascend-backend/internal/realtime"
	pgstore "ascend-backend/internal/store/postgres"
	"ascend-backend/pkg/config"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/redis/go-redis/v9"
)

const (
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

// workerStores bundles the store singletons used per event — constructed
// once at worker start, not per message.
type workerStores struct {
	xpEvents      *pgstore.XPEventStoreImpl
	notifications *pgstore.NotificationStore
}

// RunXPWorker blocks forever, draining the XP and Punishment queues.
// Call as: go RunXPWorker(ctx, cfg)
func RunXPWorker(ctx context.Context, cfg XPWorkerConfig) {
	log.Println("[xp-worker] starting")

	stores := &workerStores{
		xpEvents:      pgstore.NewXPEventStore(cfg.DB),
		notifications: pgstore.NewNotificationStore(cfg.DB),
	}

	// Run XP queue and punishment queue in parallel.
	go runPunishmentWorker(ctx, cfg, stores)

	for {
		select {
		case <-ctx.Done():
			log.Println("[xp-worker] shutting down")
			return
		default:
		}

		// BLPOP blocks up to workerBlockTimeout waiting for an event.
		results, err := cfg.Redis.BLPop(ctx, workerBlockTimeout, events.XPQueueKey).Result()
		if err != nil {
			if err == redis.Nil {
				continue // timeout — no events, loop
			}
			log.Printf("[xp-worker] BLPop error: %v", err)
			time.Sleep(2 * time.Second)
			continue
		}
		if len(results) < 2 {
			continue
		}

		var event events.XPEvent
		if err := json.Unmarshal([]byte(results[1]), &event); err != nil {
			log.Printf("[xp-worker] bad XP event JSON: %v", err)
			continue
		}

		if err := processXPEvent(ctx, cfg, stores, event); err != nil {
			log.Printf("[xp-worker] processXPEvent error (user=%s): %v", event.UserID, err)
			// Re-queue with backoff instead of dropping.
			requeueWithDelay(ctx, cfg.Redis, events.XPQueueKey, results[1], 30*time.Second)
		}
	}
}

// processXPEvent performs side effects for XP that game.AwardXP already
// applied: history logging, achievement evaluation, notification, realtime.
func processXPEvent(ctx context.Context, cfg XPWorkerConfig, stores *workerStores, event events.XPEvent) error {
	// Current totals for logging and achievement thresholds.
	var totalXP, level int
	if err := cfg.DB.QueryRow(ctx,
		`SELECT total_xp, level FROM users WHERE id = $1`, event.UserID,
	).Scan(&totalXP, &level); err != nil {
		return fmt.Errorf("load user totals: %w", err)
	}

	// 1. Capped recent-history list in Redis (fast reads for history screen).
	logEntry, _ := json.Marshal(map[string]any{
		"amount":     event.Amount,
		"source":     event.Source,
		"source_id":  event.SourceID,
		"total_xp":   totalXP,
		"level":      level,
		"created_at": event.CreatedAt,
	})
	pipe := cfg.Redis.Pipeline()
	pipe.LPush(ctx, xpLogKeyPrefix+event.UserID, string(logEntry))
	pipe.LTrim(ctx, xpLogKeyPrefix+event.UserID, 0, 99)
	if _, err := pipe.Exec(ctx); err != nil {
		log.Printf("[xp-worker] redis log error: %v", err) // non-fatal
	}

	// 2. Long-term history in postgres — also drives activity streaks.
	if err := stores.xpEvents.Insert(ctx, pgstore.XPEventRow{
		UserID:    event.UserID,
		Amount:    event.Amount,
		Source:    event.Source,
		SourceID:  event.SourceID,
		CreatedAt: event.CreatedAt,
	}); err != nil {
		log.Printf("[xp-worker] xp_events insert error: %v", err)
	}

	// 3. Achievements.
	awarded, err := achievements.CheckAndAward(ctx, cfg.DB, event.UserID, totalXP, level)
	if err != nil {
		log.Printf("[xp-worker] achievement check error: %v", err)
	}

	// 4. Notifications (in-app row + FCM push).
	xp := event.Amount
	if event.DidLevelUp {
		notifications.Deliver(ctx, stores.notifications, cfg.Notifier, event.UserID,
			"LEVEL_UP",
			"⬆ LEVEL UP!",
			fmt.Sprintf("You reached Level %d. Keep ascending!", event.NewLevel),
			&xp, "dashboard",
			map[string]string{"level": fmt.Sprint(event.NewLevel)})
	} else {
		notifications.Deliver(ctx, stores.notifications, cfg.Notifier, event.UserID,
			"QUEST_COMPLETE",
			fmt.Sprintf("+%d XP", event.Amount),
			fmt.Sprintf("%s complete. Total XP: %d", titleCase(event.Source), totalXP),
			&xp, "dashboard", nil)
	}
	for _, ach := range awarded {
		notifications.Deliver(ctx, stores.notifications, cfg.Notifier, event.UserID,
			"GOAL_MILESTONE",
			"🏆 Achievement Unlocked!",
			fmt.Sprintf("%s — %s", ach.Title, ach.Description),
			nil, "profile",
			map[string]string{"achievement_key": ach.Key})
	}

	// 5. Realtime push over WebSocket (Redis Pub/Sub → API hub → device).
	publishRealtime(ctx, cfg.Redis, event.UserID, event.DidLevelUp, event.NewLevel, event.Amount)

	log.Printf("[xp-worker] user=%s +%d XP → total=%d level=%d level_up=%v achievements=%d",
		event.UserID, event.Amount, totalXP, level, event.DidLevelUp, len(awarded))
	return nil
}

func titleCase(s string) string {
	if s == "" {
		return "Quest"
	}
	b := []byte(s)
	if b[0] >= 'a' && b[0] <= 'z' {
		b[0] -= 'a' - 'A'
	}
	return string(b)
}

// publishRealtime emits the WS frame the Android client expects:
// {"type":"LEVEL_UP","payload":{"new_level":N,"xp_awarded":N}} or
// {"type":"XP_AWARDED","payload":{"amount":N}}.
func publishRealtime(ctx context.Context, rdb *redis.Client, userID string, didLevelUp bool, newLevel, amount int) {
	var frame []byte
	if didLevelUp {
		frame, _ = json.Marshal(map[string]any{
			"type":    "LEVEL_UP",
			"payload": map[string]any{"new_level": newLevel, "xp_awarded": amount},
		})
	} else {
		frame, _ = json.Marshal(map[string]any{
			"type":    "XP_AWARDED",
			"payload": map[string]any{"amount": amount},
		})
	}
	if err := rdb.Publish(ctx, realtime.ChannelPrefix+userID, frame).Err(); err != nil {
		log.Printf("[xp-worker] realtime publish error: %v", err)
	}
}

// runPunishmentWorker notifies users about HP loss. The HP itself was
// already deducted synchronously (game.AwardXP / quest skip path).
func runPunishmentWorker(ctx context.Context, cfg XPWorkerConfig, stores *workerStores) {
	log.Println("[punishment-worker] starting")
	for {
		select {
		case <-ctx.Done():
			return
		default:
		}

		results, err := cfg.Redis.BLPop(ctx, workerBlockTimeout, events.PunishmentQueueKey).Result()
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

		var event events.PunishmentEvent
		if err := json.Unmarshal([]byte(results[1]), &event); err != nil {
			log.Printf("[punishment-worker] bad JSON: %v", err)
			continue
		}

		body := fmt.Sprintf("-%d HP for: %s. Current HP: %d/100", event.HPLoss, event.Reason, event.HPAfter)
		if event.HPAfter <= 20 {
			body += " ⚠ Critical HP! Complete quests to recover."
		}
		notifications.Deliver(ctx, stores.notifications, cfg.Notifier, event.UserID,
			"STREAK_BROKEN",
			"❤ HP Deducted", body, nil, "dashboard",
			map[string]string{"hp": fmt.Sprint(event.HPAfter)})

		log.Printf("[punishment-worker] user=%s -%dHP → HP=%d reason=%s",
			event.UserID, event.HPLoss, event.HPAfter, event.Reason)
	}
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
