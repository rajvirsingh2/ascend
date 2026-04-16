package ingestion

import (
	"context"
	"encoding/json"
	"log"

	"github.com/redis/go-redis/v9"
)

const QueueKey = "ingestion_queue"

type EventType string

const (
	EventQuestCompleted EventType = "quest_completed"
	EventQuestSkipped   EventType = "quest_skipped"
	EventGoalCreated    EventType = "goal_created"
	EventHabitMilestone EventType = "habit_milestone"
)

type Job struct {
	EventType EventType      `json:"event_type"`
	UserID    string         `json:"user_id"`
	Payload   map[string]any `json:"payload"`
}

// Publish pushes a job onto the Redis ingestion queue.
// It is fire-and-forget — errors are logged but never returned to the caller.
func Publish(ctx context.Context, rdb *redis.Client, job Job) {
	data, err := json.Marshal(job)
	if err != nil {
		log.Printf("ingestion: failed to marshal job %s: %v", job.EventType, err)
		return
	}

	if err := rdb.RPush(ctx, QueueKey, data).Err(); err != nil {
		log.Printf("ingestion: failed to publish job %s: %v", job.EventType, err)
	}
}
