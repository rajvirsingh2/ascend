package kafka

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"os"
	"time"

	"github.com/segmentio/kafka-go"
)

type Producer struct {
	writers map[string]*kafka.Writer
}

func NewProducer(brokers []string) *Producer {
	isProd := os.Getenv("APP_ENV") == "production"
	make_writer := func(topic string) *kafka.Writer {
		return &kafka.Writer{
			Addr:                   kafka.TCP(brokers...),
			Topic:                  topic,
			Balancer:               &kafka.Hash{}, // key-based — same user → same partition
			RequiredAcks:           kafka.RequireAll,
			AllowAutoTopicCreation: !isProd, // auto-create in dev; pre-create in prod
			WriteTimeout:           5 * time.Second,
			BatchSize:              100,
			BatchTimeout:           5 * time.Millisecond,
		}
	}

	return &Producer{
		writers: map[string]*kafka.Writer{
			TopicQuestCompleted: make_writer(TopicQuestCompleted),
			TopicHabitCompleted: make_writer(TopicHabitCompleted),
			TopicUserLeveledUp:  make_writer(TopicUserLeveledUp),
			TopicGuildAction:    make_writer(TopicGuildAction),
		},
	}
}

const (
	TopicQuestCompleted = "ascend.quest.completed"
	TopicHabitCompleted = "ascend.habit.completed"
	TopicUserLeveledUp  = "ascend.user.leveled_up"
	TopicGuildAction    = "ascend.guild.action"
)

type Event struct {
	EventID   string         `json:"event_id"`
	UserID    string         `json:"user_id"`
	Type      string         `json:"type"`
	Timestamp time.Time      `json:"timestamp"`
	Payload   map[string]any `json:"payload"`
}

func (p *Producer) Publish(ctx context.Context, topic string, e Event) error {
	w, ok := p.writers[topic]
	if !ok {
		return fmt.Errorf("unknown topic: %s", topic)
	}

	data, err := json.Marshal(e)
	if err != nil {
		return fmt.Errorf("marshalling event: %w", err)
	}

	err = w.WriteMessages(ctx, kafka.Message{
		Key:   []byte(e.UserID), // partition key — orders events per user
		Value: data,
		Time:  e.Timestamp,
	})
	if err != nil {
		slog.Error("kafka publish failed",
			"topic", topic, "user_id", e.UserID, "error", err)
		return err
	}

	slog.Info("kafka event published", "topic", topic,
		"user_id", e.UserID, "type", e.Type)
	return nil
}

func (p *Producer) Close() {
	for _, w := range p.writers {
		_ = w.Close()
	}
}
