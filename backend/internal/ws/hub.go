// internal/ws/hub.go
package ws

import (
	"context"
	"encoding/json"
	"log/slog"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	kafkapkg "github.com/rajvirsingh2/ascend-backend/internal/kafka"
	"github.com/rajvirsingh2/ascend-backend/internal/middleware"
	"github.com/redis/go-redis/v9"
	"github.com/segmentio/kafka-go"
)

type Frame struct {
	Type    string         `json:"type"`
	Payload map[string]any `json:"payload"`
}

type client struct {
	conn   *websocket.Conn
	send   chan []byte
	userID string
}

type Hub struct {
	mu      sync.RWMutex
	clients map[string]*client // userID → client
	rdb     *redis.Client
	podID   string // unique per Render instance
}

var upgrader = websocket.Upgrader{
	HandshakeTimeout: 10 * time.Second,
	CheckOrigin:      func(r *http.Request) bool { return true },
}

func NewHub(rdb *redis.Client, podID string) *Hub {
	return &Hub{
		clients: make(map[string]*client),
		rdb:     rdb,
		podID:   podID,
	}
}

// ServeWS upgrades an HTTP connection to WebSocket.
// Called from the Go router — JWT guard runs before this.
func (h *Hub) ServeWS(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		slog.Error("ws upgrade failed", "user_id", userID, "error", err)
		return
	}

	c := &client{conn: conn, send: make(chan []byte, 64), userID: userID}

	h.mu.Lock()
	h.clients[userID] = c
	h.mu.Unlock()

	// register in Redis so other pods know which pod holds this user's connection
	h.rdb.Set(r.Context(),
		"ws:"+userID, h.podID,
		30*time.Minute,
	)

	slog.Info("ws client connected", "user_id", userID)

	go h.writePump(c)
	h.readPump(c) // blocks until disconnect

	// cleanup on disconnect
	h.mu.Lock()
	delete(h.clients, userID)
	h.mu.Unlock()
	h.rdb.Del(r.Context(), "ws:"+userID)
	close(c.send)
	slog.Info("ws client disconnected", "user_id", userID)
}

// Push sends a frame to a specific user if they are connected to this pod.
func (h *Hub) Push(userID string, frame Frame) bool {
	h.mu.RLock()
	c, ok := h.clients[userID]
	h.mu.RUnlock()
	if !ok {
		return false
	}
	data, _ := json.Marshal(frame)
	select {
	case c.send <- data:
		return true
	default:
		// client send buffer full — drop
		return false
	}
}

func (h *Hub) writePump(c *client) {
	ticker := time.NewTicker(25 * time.Second)
	defer ticker.Stop()
	for {
		select {
		case msg, ok := <-c.send:
			if !ok {
				c.conn.WriteMessage(websocket.CloseMessage, nil)
				return
			}
			c.conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := c.conn.WriteMessage(websocket.TextMessage, msg); err != nil {
				return
			}
		case <-ticker.C:
			c.conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := c.conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

func (h *Hub) readPump(c *client) {
	c.conn.SetReadDeadline(time.Now().Add(60 * time.Second))
	c.conn.SetPongHandler(func(string) error {
		c.conn.SetReadDeadline(time.Now().Add(60 * time.Second))
		return nil
	})
	for {
		_, _, err := c.conn.ReadMessage()
		if err != nil {
			return
		}
	}
}

// RunKafkaConsumer consumes Kafka events and pushes them to connected clients.
// kafka-go v0.4.47 ReaderConfig has no Topics (plural) field — one reader per topic.
func (h *Hub) RunKafkaConsumer(ctx context.Context, brokers []string) {
	makeReader := func(topic string) *kafka.Reader {
		return kafka.NewReader(kafka.ReaderConfig{
			Brokers:  brokers,
			Topic:    topic,
			GroupID:  "websocket-broadcaster",
			MinBytes: 1,
			MaxBytes: 1e6,
		})
	}

	consume := func(reader *kafka.Reader) {
		defer reader.Close()
		for {
			msg, err := reader.FetchMessage(ctx)
			if err != nil {
				if ctx.Err() != nil {
					return
				}
				continue
			}

			var e kafkapkg.Event
			if err := json.Unmarshal(msg.Value, &e); err != nil {
				reader.CommitMessages(ctx, msg)
				continue
			}

			frameType := map[string]string{
				"UserLeveledUp":       "LEVEL_UP",
				"GuildQuestCompleted": "GUILD_QUEST",
			}[e.Type]

			if frameType != "" {
				h.Push(e.UserID, Frame{Type: frameType, Payload: e.Payload})
			}

			reader.CommitMessages(ctx, msg)
		}
	}

	slog.Info("ws kafka consumer started")
	go consume(makeReader(kafkapkg.TopicUserLeveledUp))
	consume(makeReader(kafkapkg.TopicGuildAction)) // blocks until ctx is done
}
