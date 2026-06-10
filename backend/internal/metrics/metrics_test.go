package metrics

import (
	"context"
	"io"
	"net/http/httptest"
	"strings"
	"testing"
)

func scrape(t *testing.T, m *HTTPMetrics) string {
	t.Helper()
	rec := httptest.NewRecorder()
	m.Handler().ServeHTTP(rec, httptest.NewRequestWithContext(context.Background(), "GET", "/metrics", nil))
	body, _ := io.ReadAll(rec.Body)
	return string(body)
}

func TestCounterAndHistogram(t *testing.T) {
	m := New()
	m.Observe("GET", "/api/v1/me", 200, 0.003)
	m.Observe("GET", "/api/v1/me", 200, 0.030)
	m.Observe("POST", "/api/v1/quests/{id}/complete", 500, 2.0)

	out := scrape(t, m)

	for _, want := range []string{
		`ascend_http_requests_total{method="GET",path="/api/v1/me",status="200"} 2`,
		`ascend_http_requests_total{method="POST",path="/api/v1/quests/{id}/complete",status="500"} 1`,
		`ascend_http_request_duration_seconds_count{method="GET",path="/api/v1/me",status="200"} 2`,
		// 0.003 ≤ 0.005 → first bucket has 1
		`ascend_http_request_duration_seconds_bucket{method="GET",path="/api/v1/me",status="200",le="0.005"} 1`,
		// cumulative: by le=0.05 both observations counted
		`ascend_http_request_duration_seconds_bucket{method="GET",path="/api/v1/me",status="200",le="0.05"} 2`,
		`le="+Inf"} 2`,
	} {
		if !strings.Contains(out, want) {
			t.Errorf("scrape output missing %q\n---\n%s", want, out)
		}
	}
}

func TestEmptyPatternBecomesUnmatched(t *testing.T) {
	m := New()
	m.Observe("GET", "", 404, 0.001)
	if !strings.Contains(scrape(t, m), `path="unmatched"`) {
		t.Error("empty pattern not normalized")
	}
}

func TestGauge(t *testing.T) {
	m := New()
	v := 0.0
	m.RegisterGauge("ascend_ws_connections", func() float64 { return v })

	if !strings.Contains(scrape(t, m), "ascend_ws_connections 0") {
		t.Error("gauge missing at 0")
	}
	v = 7
	if !strings.Contains(scrape(t, m), "ascend_ws_connections 7") {
		t.Error("gauge not live")
	}
}

func TestContentType(t *testing.T) {
	m := New()
	rec := httptest.NewRecorder()
	m.Handler().ServeHTTP(rec, httptest.NewRequestWithContext(context.Background(), "GET", "/metrics", nil))
	if ct := rec.Header().Get("Content-Type"); !strings.HasPrefix(ct, "text/plain; version=0.0.4") {
		t.Errorf("content type = %q", ct)
	}
}
