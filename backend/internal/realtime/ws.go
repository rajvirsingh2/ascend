// Package realtime implements the /api/v1/ws WebSocket endpoint that pushes
// XP_AWARDED / LEVEL_UP events to connected clients.
//
// The WebSocket protocol (RFC 6455) is implemented directly on top of the
// standard library so the package adds no module dependencies. Only the
// server side of the protocol is implemented: small text frames out,
// ping/pong/close handling in. Clients never send application data.
package realtime

import (
	"bufio"
	"crypto/sha1"
	"encoding/base64"
	"encoding/binary"
	"errors"
	"fmt"
	"io"
	"net"
	"net/http"
	"strings"
	"sync"
	"time"
)

// ChannelPrefix is the Redis Pub/Sub channel prefix used to fan realtime
// events out to whichever API instance holds the user's socket.
// Full channel name: ChannelPrefix + <userID>.
const ChannelPrefix = "ascend:rt:"

const (
	opText  byte = 0x1
	opClose byte = 0x8
	opPing  byte = 0x9
	opPong  byte = 0xA

	writeTimeout = 10 * time.Second
	// readTimeout must exceed the server ping interval so an idle but
	// healthy connection (client answers pings) never expires.
	readTimeout  = 90 * time.Second
	pingInterval = 30 * time.Second

	// maxFramePayload guards against malicious length headers. Clients only
	// ever send control frames and tiny pongs.
	maxFramePayload = 1 << 16
)

var errClosed = errors.New("websocket: connection closed")

// Conn is a server-side WebSocket connection.
type Conn struct {
	raw net.Conn
	br  *bufio.Reader

	writeMu sync.Mutex
	closed  bool
}

// Upgrade performs the RFC 6455 opening handshake and hijacks the underlying
// TCP connection. On error it writes an HTTP error response and returns it.
func Upgrade(w http.ResponseWriter, r *http.Request) (*Conn, error) {
	if r.Method != http.MethodGet {
		http.Error(w, "websocket: method must be GET", http.StatusMethodNotAllowed)
		return nil, errors.New("websocket: method not GET")
	}
	if !headerContainsToken(r.Header, "Connection", "upgrade") ||
		!strings.EqualFold(r.Header.Get("Upgrade"), "websocket") {
		http.Error(w, "websocket: not a websocket handshake", http.StatusBadRequest)
		return nil, errors.New("websocket: missing upgrade headers")
	}
	if r.Header.Get("Sec-WebSocket-Version") != "13" {
		w.Header().Set("Sec-WebSocket-Version", "13")
		http.Error(w, "websocket: unsupported version", http.StatusBadRequest)
		return nil, errors.New("websocket: bad version")
	}
	key := r.Header.Get("Sec-WebSocket-Key")
	if key == "" {
		http.Error(w, "websocket: missing Sec-WebSocket-Key", http.StatusBadRequest)
		return nil, errors.New("websocket: missing key")
	}

	hj, ok := w.(http.Hijacker)
	if !ok {
		http.Error(w, "websocket: server does not support hijacking", http.StatusInternalServerError)
		return nil, errors.New("websocket: no hijacker")
	}
	raw, rw, err := hj.Hijack()
	if err != nil {
		return nil, fmt.Errorf("websocket: hijack: %w", err)
	}
	// net/http arms read/write deadlines from the server's Read/WriteTimeout
	// before the handler runs; they survive the hijack and would kill the
	// long-lived socket. Clear them — frame I/O sets its own deadlines.
	_ = raw.SetDeadline(time.Time{})

	resp := "HTTP/1.1 101 Switching Protocols\r\n" +
		"Upgrade: websocket\r\n" +
		"Connection: Upgrade\r\n" +
		"Sec-WebSocket-Accept: " + AcceptKey(key) + "\r\n\r\n"
	if _, err := rw.WriteString(resp); err != nil {
		raw.Close()
		return nil, fmt.Errorf("websocket: handshake write: %w", err)
	}
	if err := rw.Flush(); err != nil {
		raw.Close()
		return nil, fmt.Errorf("websocket: handshake flush: %w", err)
	}

	return &Conn{raw: raw, br: rw.Reader}, nil
}

// AcceptKey computes the Sec-WebSocket-Accept value for a handshake key.
func AcceptKey(key string) string {
	h := sha1.Sum([]byte(key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"))
	return base64.StdEncoding.EncodeToString(h[:])
}

func headerContainsToken(h http.Header, name, token string) bool {
	for _, v := range h.Values(name) {
		for _, part := range strings.Split(v, ",") {
			if strings.EqualFold(strings.TrimSpace(part), token) {
				return true
			}
		}
	}
	return false
}

// WriteText sends a single unfragmented text frame.
func (c *Conn) WriteText(payload []byte) error {
	return c.writeFrame(opText, payload)
}

// Ping sends a ping control frame.
func (c *Conn) Ping() error {
	return c.writeFrame(opPing, nil)
}

func (c *Conn) writeFrame(opcode byte, payload []byte) error {
	c.writeMu.Lock()
	defer c.writeMu.Unlock()
	if c.closed {
		return errClosed
	}

	header := make([]byte, 0, 10)
	header = append(header, 0x80|opcode) // FIN set, no fragmentation
	switch n := len(payload); {
	case n < 126:
		header = append(header, byte(n))
	case n <= 0xFFFF:
		header = append(header, 126, byte(n>>8), byte(n))
	default:
		var ext [8]byte
		binary.BigEndian.PutUint64(ext[:], uint64(n))
		header = append(header, 127)
		header = append(header, ext[:]...)
	}

	if err := c.raw.SetWriteDeadline(time.Now().Add(writeTimeout)); err != nil {
		return err
	}
	if _, err := c.raw.Write(header); err != nil {
		return err
	}
	if len(payload) > 0 {
		if _, err := c.raw.Write(payload); err != nil {
			return err
		}
	}
	return nil
}

// readFrame reads one frame, unmasking the payload (clients must mask).
func (c *Conn) readFrame() (opcode byte, payload []byte, err error) {
	if err := c.raw.SetReadDeadline(time.Now().Add(readTimeout)); err != nil {
		return 0, nil, err
	}

	var hdr [2]byte
	if _, err := io.ReadFull(c.br, hdr[:]); err != nil {
		return 0, nil, err
	}
	opcode = hdr[0] & 0x0F
	masked := hdr[1]&0x80 != 0
	length := uint64(hdr[1] & 0x7F)

	switch length {
	case 126:
		var ext [2]byte
		if _, err := io.ReadFull(c.br, ext[:]); err != nil {
			return 0, nil, err
		}
		length = uint64(binary.BigEndian.Uint16(ext[:]))
	case 127:
		var ext [8]byte
		if _, err := io.ReadFull(c.br, ext[:]); err != nil {
			return 0, nil, err
		}
		length = binary.BigEndian.Uint64(ext[:])
	}
	if length > maxFramePayload {
		return 0, nil, errors.New("websocket: frame too large")
	}

	var maskKey [4]byte
	if masked {
		if _, err := io.ReadFull(c.br, maskKey[:]); err != nil {
			return 0, nil, err
		}
	}

	payload = make([]byte, length)
	if _, err := io.ReadFull(c.br, payload); err != nil {
		return 0, nil, err
	}
	if masked {
		for i := range payload {
			payload[i] ^= maskKey[i%4]
		}
	}
	return opcode, payload, nil
}

// ReadLoop consumes inbound frames until the connection dies or the client
// sends a close frame. It answers pings with pongs and discards everything
// else (clients never send application data).
func (c *Conn) ReadLoop() {
	for {
		opcode, payload, err := c.readFrame()
		if err != nil {
			return
		}
		switch opcode {
		case opPing:
			if err := c.writeFrame(opPong, payload); err != nil {
				return
			}
		case opClose:
			// Echo close per RFC 6455 §5.5.1, then drop.
			c.writeMu.Lock()
			if !c.closed {
				_ = c.raw.SetWriteDeadline(time.Now().Add(writeTimeout))
				_, _ = c.raw.Write([]byte{0x80 | opClose, 0})
			}
			c.writeMu.Unlock()
			return
		default:
			// Pongs and stray data frames: ignore.
		}
	}
}

// Close tears the connection down. Safe to call multiple times.
func (c *Conn) Close() {
	c.writeMu.Lock()
	defer c.writeMu.Unlock()
	if c.closed {
		return
	}
	c.closed = true
	_ = c.raw.Close()
}
