package events

import (
	"context"
	"encoding/json"
	"log/slog"
	"time"

	"github.com/redis/go-redis/v9"
)

type HandlerFunc func(ctx context.Context, e Event) error

type Consumer struct {
	rdb     *redis.Client
	stream  string
	group   string
	name    string // consumer name within group
	handler HandlerFunc
}

func NewConsumer(
	rdb *redis.Client,
	stream, group, consumerName string,
	handler HandlerFunc,
) *Consumer {
	return &Consumer{
		rdb:     rdb,
		stream:  stream,
		group:   group,
		name:    consumerName,
		handler: handler,
	}
}

// EnsureGroup creates the consumer group if it does not exist.
func (c *Consumer) EnsureGroup(ctx context.Context) error {
	err := c.rdb.XGroupCreateMkStream(ctx, c.stream, c.group, "0").Err()
	if err != nil && err.Error() != "BUSYGROUP Consumer Group name already exists" {
		return err
	}
	return nil
}

// Run blocks and processes messages. Retries on transient failures.
func (c *Consumer) Run(ctx context.Context) {
	slog.Info("consumer started", "stream", c.stream, "group", c.group)

	for {
		if ctx.Err() != nil {
			return
		}

		msgs, err := c.rdb.XReadGroup(ctx, &redis.XReadGroupArgs{
			Group:    c.group,
			Consumer: c.name,
			Streams:  []string{c.stream, ">"},
			Count:    10,
			Block:    5 * time.Second,
		}).Result()

		if err != nil {
			if err == redis.Nil {
				continue // timeout — no messages, loop again
			}
			if ctx.Err() != nil {
				return
			}
			slog.Error("consumer read error", "stream", c.stream, "error", err)
			time.Sleep(2 * time.Second)
			continue
		}

		for _, stream := range msgs {
			for _, msg := range stream.Messages {
				c.processMessage(ctx, msg)
			}
		}
	}
}

func (c *Consumer) processMessage(ctx context.Context, msg redis.XMessage) {
	raw, ok := msg.Values["data"].(string)
	if !ok {
		c.rdb.XAck(ctx, c.stream, c.group, msg.ID)
		return
	}

	var e Event
	if err := json.Unmarshal([]byte(raw), &e); err != nil {
		slog.Error("unmarshal error", "msg_id", msg.ID, "error", err)
		c.rdb.XAck(ctx, c.stream, c.group, msg.ID)
		return
	}

	if err := c.handler(ctx, e); err != nil {
		slog.Error("handler error — will retry",
			"stream", c.stream, "msg_id", msg.ID, "error", err)
		return // do NOT ack — Redis will redeliver
	}

	c.rdb.XAck(ctx, c.stream, c.group, msg.ID)
}
