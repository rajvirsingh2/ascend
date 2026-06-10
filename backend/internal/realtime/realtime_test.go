package realtime

import (
	"bufio"
	"context"
	"encoding/binary"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"
)

// RFC 6455 §1.3 sample handshake vector.
func TestAcceptKey(t *testing.T) {
	got := AcceptKey("dGhlIHNhbXBsZSBub25jZQ==")
	want := "s3pPLMBiTxaQ9kYGzzhZRbK+xOo="
	if got != want {
		t.Fatalf("AcceptKey = %q, want %q", got, want)
	}
}

func TestUpgradeRejectsNonWebSocket(t *testing.T) {
	hub := NewHub()
	srv := httptest.NewServer(ServeWS(hub, func(*http.Request) string { return "u1" }))
	defer srv.Close()

	req, _ := http.NewRequestWithContext(context.Background(), "GET", srv.URL, nil)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusBadRequest {
		t.Fatalf("status = %d, want 400", resp.StatusCode)
	}
}

func TestUnauthenticatedRejected(t *testing.T) {
	hub := NewHub()
	srv := httptest.NewServer(ServeWS(hub, func(*http.Request) string { return "" }))
	defer srv.Close()

	req, _ := http.NewRequestWithContext(context.Background(), "GET", srv.URL, nil)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusUnauthorized {
		t.Fatalf("status = %d, want 401", resp.StatusCode)
	}
}

// dial performs a raw client handshake and returns the TCP conn.
func dial(t *testing.T, srv *httptest.Server) net.Conn {
	t.Helper()
	var d net.Dialer
	conn, err := d.DialContext(context.Background(), "tcp", strings.TrimPrefix(srv.URL, "http://"))
	if err != nil {
		t.Fatal(err)
	}
	req := "GET / HTTP/1.1\r\n" +
		"Host: x\r\n" +
		"Upgrade: websocket\r\n" +
		"Connection: Upgrade\r\n" +
		"Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
		"Sec-WebSocket-Version: 13\r\n\r\n"
	if _, err := conn.Write([]byte(req)); err != nil {
		t.Fatal(err)
	}
	br := bufio.NewReader(conn)
	status, err := br.ReadString('\n')
	if err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(status, "101") {
		t.Fatalf("handshake status line = %q", status)
	}
	sawAccept := false
	for {
		line, err := br.ReadString('\n')
		if err != nil {
			t.Fatal(err)
		}
		if strings.HasPrefix(strings.ToLower(line), "sec-websocket-accept:") {
			if !strings.Contains(line, "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=") {
				t.Fatalf("bad accept header: %q", line)
			}
			sawAccept = true
		}
		if line == "\r\n" {
			break
		}
	}
	if !sawAccept {
		t.Fatal("no Sec-WebSocket-Accept header")
	}
	if br.Buffered() > 0 {
		t.Fatalf("unexpected buffered bytes after handshake: %d", br.Buffered())
	}
	return conn
}

// readServerFrame parses one unmasked server frame.
func readServerFrame(t *testing.T, conn net.Conn) (opcode byte, payload []byte) {
	t.Helper()
	_ = conn.SetReadDeadline(time.Now().Add(5 * time.Second))
	var hdr [2]byte
	if _, err := io.ReadFull(conn, hdr[:]); err != nil {
		t.Fatalf("read frame header: %v", err)
	}
	if hdr[1]&0x80 != 0 {
		t.Fatal("server frame must not be masked")
	}
	length := uint64(hdr[1] & 0x7F)
	if length == 126 {
		var ext [2]byte
		if _, err := io.ReadFull(conn, ext[:]); err != nil {
			t.Fatal(err)
		}
		length = uint64(binary.BigEndian.Uint16(ext[:]))
	}
	payload = make([]byte, length)
	if _, err := io.ReadFull(conn, payload); err != nil {
		t.Fatal(err)
	}
	return hdr[0] & 0x0F, payload
}

// writeClientFrame writes a masked client frame (clients must mask).
func writeClientFrame(t *testing.T, conn net.Conn, opcode byte, payload []byte) {
	t.Helper()
	if len(payload) >= 126 {
		t.Fatal("test helper supports short payloads only")
	}
	mask := [4]byte{0x12, 0x34, 0x56, 0x78}
	frame := []byte{0x80 | opcode, 0x80 | byte(len(payload))}
	frame = append(frame, mask[:]...)
	for i, b := range payload {
		frame = append(frame, b^mask[i%4])
	}
	if _, err := conn.Write(frame); err != nil {
		t.Fatal(err)
	}
}

func waitFor(t *testing.T, what string, cond func() bool) {
	t.Helper()
	deadline := time.Now().Add(3 * time.Second)
	for time.Now().Before(deadline) {
		if cond() {
			return
		}
		time.Sleep(10 * time.Millisecond)
	}
	t.Fatalf("timeout waiting for %s", what)
}

func TestEndToEndDelivery(t *testing.T) {
	hub := NewHub()
	srv := httptest.NewServer(ServeWS(hub, func(*http.Request) string { return "user-42" }))
	defer srv.Close()

	conn := dial(t, srv)
	defer conn.Close()

	waitFor(t, "registration", func() bool { return hub.ConnCount() == 1 })

	msg := []byte(`{"type":"XP_AWARDED","payload":{"amount":50}}`)
	if n := hub.SendToUser("user-42", msg); n != 1 {
		t.Fatalf("SendToUser delivered %d, want 1", n)
	}

	opcode, payload := readServerFrame(t, conn)
	if opcode != opText {
		t.Fatalf("opcode = %x, want text", opcode)
	}
	if string(payload) != string(msg) {
		t.Fatalf("payload = %s", payload)
	}

	// Wrong user gets nothing.
	if n := hub.SendToUser("someone-else", msg); n != 0 {
		t.Fatalf("delivered to wrong user: %d", n)
	}
}

func TestLargeFrameUses16BitLength(t *testing.T) {
	hub := NewHub()
	srv := httptest.NewServer(ServeWS(hub, func(*http.Request) string { return "u" }))
	defer srv.Close()

	conn := dial(t, srv)
	defer conn.Close()
	waitFor(t, "registration", func() bool { return hub.ConnCount() == 1 })

	big := []byte(strings.Repeat("x", 300))
	if n := hub.SendToUser("u", big); n != 1 {
		t.Fatal("send failed")
	}
	opcode, payload := readServerFrame(t, conn)
	if opcode != opText || len(payload) != 300 {
		t.Fatalf("opcode=%x len=%d", opcode, len(payload))
	}
}

func TestPingAnsweredWithPong(t *testing.T) {
	hub := NewHub()
	srv := httptest.NewServer(ServeWS(hub, func(*http.Request) string { return "u" }))
	defer srv.Close()

	conn := dial(t, srv)
	defer conn.Close()
	waitFor(t, "registration", func() bool { return hub.ConnCount() == 1 })

	writeClientFrame(t, conn, opPing, []byte("hi"))
	opcode, payload := readServerFrame(t, conn)
	if opcode != opPong || string(payload) != "hi" {
		t.Fatalf("opcode=%x payload=%q, want pong %q", opcode, payload, "hi")
	}
}

func TestClientCloseUnregisters(t *testing.T) {
	hub := NewHub()
	srv := httptest.NewServer(ServeWS(hub, func(*http.Request) string { return "u" }))
	defer srv.Close()

	conn := dial(t, srv)
	defer conn.Close()
	waitFor(t, "registration", func() bool { return hub.ConnCount() == 1 })

	writeClientFrame(t, conn, opClose, nil)
	waitFor(t, "unregister", func() bool { return hub.ConnCount() == 0 })

	// Server should have echoed a close frame.
	opcode, _ := readServerFrame(t, conn)
	if opcode != opClose {
		t.Fatalf("opcode = %x, want close", opcode)
	}
}

func TestDroppedConnPruned(t *testing.T) {
	hub := NewHub()
	srv := httptest.NewServer(ServeWS(hub, func(*http.Request) string { return "u" }))
	defer srv.Close()

	conn := dial(t, srv)
	waitFor(t, "registration", func() bool { return hub.ConnCount() == 1 })
	conn.Close() // hard drop, no close frame

	waitFor(t, "prune", func() bool { return hub.ConnCount() == 0 })
}

func TestHubFanOutMultipleDevices(t *testing.T) {
	hub := NewHub()
	srv := httptest.NewServer(ServeWS(hub, func(*http.Request) string { return "u" }))
	defer srv.Close()

	c1 := dial(t, srv)
	defer c1.Close()
	c2 := dial(t, srv)
	defer c2.Close()
	waitFor(t, "both registered", func() bool { return hub.ConnCount() == 2 })

	if n := hub.SendToUser("u", []byte("hello")); n != 2 {
		t.Fatalf("delivered %d, want 2", n)
	}
	for i, c := range []net.Conn{c1, c2} {
		_, payload := readServerFrame(t, c)
		if string(payload) != "hello" {
			t.Fatalf("conn %d payload = %q", i, payload)
		}
	}
}

func TestConcurrentSendsRaceFree(t *testing.T) {
	hub := NewHub()
	srv := httptest.NewServer(ServeWS(hub, func(*http.Request) string { return "u" }))
	defer srv.Close()

	conn := dial(t, srv)
	defer conn.Close()
	waitFor(t, "registration", func() bool { return hub.ConnCount() == 1 })

	done := make(chan struct{})
	for i := 0; i < 10; i++ {
		go func(i int) {
			defer func() { done <- struct{}{} }()
			hub.SendToUser("u", []byte(fmt.Sprintf("m%d", i)))
		}(i)
	}
	for i := 0; i < 10; i++ {
		<-done
	}
	// All 10 frames must arrive intact (order unspecified).
	seen := map[string]bool{}
	for i := 0; i < 10; i++ {
		_, payload := readServerFrame(t, conn)
		seen[string(payload)] = true
	}
	if len(seen) != 10 {
		t.Fatalf("got %d distinct frames, want 10", len(seen))
	}
}
