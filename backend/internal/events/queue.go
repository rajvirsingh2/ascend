// Queue definitions for async side-effect processing. XP and HP are applied
// SYNCHRONOUSLY by game.AwardXP — these events only carry the result to the
// XP worker for achievements, history logging, notifications, and realtime
// pushes. This package is the single owner of the event shapes and queue
// names so producers (stores) and the consumer (worker) can't drift.
package events

import (
	"context"
	"encoding/json"
	"time"

	"github.com/redis/go-redis/v9"
)

const (
	XPQueueKey         = "ascend:queue:xp"
	PunishmentQueueKey = "ascend:queue:punishment"
)

// XPEvent describes XP that was already awarded (by game.AwardXP).
type XPEvent struct {
	UserID     string    `json:"user_id"`
	Amount     int       `json:"amount"`
	Source     string    `json:"source"` // "quest" | "habit" | "physique"
	SourceID   string    `json:"source_id"`
	SkillArea  string    `json:"skill_area,omitempty"`
	NewLevel   int       `json:"new_level"`
	DidLevelUp bool      `json:"did_level_up"`
	CreatedAt  time.Time `json:"created_at"`
}

// PunishmentEvent describes HP that was already deducted.
type PunishmentEvent struct {
	UserID  string `json:"user_id"`
	HPLoss  int    `json:"hp_loss"`
	HPAfter int    `json:"hp_after"`
	Reason  string `json:"reason"`
}

// EnqueueXP pushes an awarded-XP event for async side-effect processing.
func EnqueueXP(ctx context.Context, rdb *redis.Client, event XPEvent) error {
	if event.CreatedAt.IsZero() {
		event.CreatedAt = time.Now().UTC()
	}
	b, err := json.Marshal(event)
	if err != nil {
		return err
	}
	return rdb.RPush(ctx, XPQueueKey, string(b)).Err()
}

// EnqueuePunishment pushes an HP-loss event for async notification.
func EnqueuePunishment(ctx context.Context, rdb *redis.Client, event PunishmentEvent) error {
	b, err := json.Marshal(event)
	if err != nil {
		return err
	}
	return rdb.RPush(ctx, PunishmentQueueKey, string(b)).Err()
}
