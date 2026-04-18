package events

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"time"

	"github.com/google/uuid"
	"github.com/redis/go-redis/v9"
)

// Stream names replace Kafka topics
const (
	StreamQuestCompleted = "ascend:quest:completed"
	StreamHabitCompleted = "ascend:habit:completed"
	StreamUserLeveledUp  = "ascend:user:leveled_up"
	StreamGuildAction    = "ascend:guild:action"
)

// Consumer group names
const (
	GroupXPWorker      = "xp-calculator"
	GroupRAGWorker     = "rag-embedder"
	GroupWSBroadcaster = "ws-broadcaster"
)

type Event struct {
	ID        string         `json:"id"`
	UserID    string         `json:"user_id"`
	Type      string         `json:"type"`
	Timestamp time.Time      `json:"timestamp"`
	Payload   map[string]any `json:"payload"`
}

type Publisher struct {
	rdb *redis.Client
}

func NewPublisher(rdb *redis.Client) *Publisher {
	return &Publisher{rdb: rdb}
}

// Publish adds an event to a Redis Stream.
// Fire-and-forget — caller does not wait for consumer processing.
func (p *Publisher) Publish(ctx context.Context, stream string, e Event) error {
	if e.ID == "" {
		e.ID = uuid.NewString()
	}
	if e.Timestamp.IsZero() {
		e.Timestamp = time.Now()
	}

	data, err := json.Marshal(e)
	if err != nil {
		return fmt.Errorf("marshalling event: %w", err)
	}

	err = p.rdb.XAdd(ctx, &redis.XAddArgs{
		Stream: stream,
		MaxLen: 10000, // cap stream length to ~10k events
		Approx: true,
		Values: map[string]any{
			"user_id": e.UserID,
			"type":    e.Type,
			"data":    string(data),
		},
	}).Err()

	if err != nil {
		slog.Error("stream publish failed",
			"stream", stream, "user_id", e.UserID, "error", err)
		return err
	}

	slog.Info("event published", "stream", stream, "type", e.Type, "user_id", e.UserID)
	return nil
}
