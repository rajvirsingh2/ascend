package realtime

import "sync"

// Hub tracks live WebSocket connections per user and fans messages out to
// every device a user has connected.
type Hub struct {
	mu    sync.RWMutex
	conns map[string]map[*Conn]struct{}
}

func NewHub() *Hub {
	return &Hub{conns: make(map[string]map[*Conn]struct{})}
}

func (h *Hub) add(userID string, c *Conn) {
	h.mu.Lock()
	defer h.mu.Unlock()
	set, ok := h.conns[userID]
	if !ok {
		set = make(map[*Conn]struct{})
		h.conns[userID] = set
	}
	set[c] = struct{}{}
}

func (h *Hub) remove(userID string, c *Conn) {
	h.mu.Lock()
	defer h.mu.Unlock()
	if set, ok := h.conns[userID]; ok {
		delete(set, c)
		if len(set) == 0 {
			delete(h.conns, userID)
		}
	}
}

// SendToUser delivers payload as a text frame to every connection the user
// has. Dead connections are closed and pruned. Returns delivered count.
func (h *Hub) SendToUser(userID string, payload []byte) int {
	h.mu.RLock()
	targets := make([]*Conn, 0, len(h.conns[userID]))
	for c := range h.conns[userID] {
		targets = append(targets, c)
	}
	h.mu.RUnlock()

	delivered := 0
	for _, c := range targets {
		if err := c.WriteText(payload); err != nil {
			c.Close()
			h.remove(userID, c)
			continue
		}
		delivered++
	}
	return delivered
}

// ConnCount reports the number of live connections (all users).
func (h *Hub) ConnCount() int {
	h.mu.RLock()
	defer h.mu.RUnlock()
	n := 0
	for _, set := range h.conns {
		n += len(set)
	}
	return n
}
