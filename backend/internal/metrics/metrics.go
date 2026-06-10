// Package metrics provides a minimal, dependency-free Prometheus exposition
// endpoint: HTTP request counters + latency histograms and arbitrary gauge
// callbacks. The text format (version 0.0.4) is simple enough that pulling in
// client_golang is not worth a new dependency tree.
package metrics

import (
	"fmt"
	"net/http"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"
)

// Histogram bucket upper bounds in seconds.
var buckets = []float64{0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10}

type routeStats struct {
	count       uint64
	durSum      float64
	bucketCount []uint64 // cumulative-on-render; stored per-bucket here
}

// HTTPMetrics records per-route request counts and latencies.
type HTTPMetrics struct {
	mu      sync.Mutex
	started time.Time
	routes  map[string]*routeStats // key: method + " " + pattern + " " + status
	gauges  map[string]func() float64
}

func New() *HTTPMetrics {
	return &HTTPMetrics{
		started: time.Now(),
		routes:  make(map[string]*routeStats),
		gauges:  make(map[string]func() float64),
	}
}

// Observe records one served request.
func (m *HTTPMetrics) Observe(method, pattern string, status int, seconds float64) {
	if pattern == "" {
		pattern = "unmatched"
	}
	key := method + " " + pattern + " " + strconv.Itoa(status)

	m.mu.Lock()
	defer m.mu.Unlock()
	rs, ok := m.routes[key]
	if !ok {
		rs = &routeStats{bucketCount: make([]uint64, len(buckets))}
		m.routes[key] = rs
	}
	rs.count++
	rs.durSum += seconds
	for i, ub := range buckets {
		if seconds <= ub {
			rs.bucketCount[i]++
			break
		}
	}
}

// RegisterGauge exposes name (must be a valid Prometheus metric name) whose
// value is read from fn at scrape time.
func (m *HTTPMetrics) RegisterGauge(name string, fn func() float64) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.gauges[name] = fn
}

// Handler serves the Prometheus text exposition format.
func (m *HTTPMetrics) Handler() http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "text/plain; version=0.0.4; charset=utf-8")

		m.mu.Lock()
		defer m.mu.Unlock()

		var b strings.Builder

		b.WriteString("# HELP ascend_uptime_seconds Time since process start.\n")
		b.WriteString("# TYPE ascend_uptime_seconds gauge\n")
		fmt.Fprintf(&b, "ascend_uptime_seconds %g\n", time.Since(m.started).Seconds())

		gaugeNames := make([]string, 0, len(m.gauges))
		for name := range m.gauges {
			gaugeNames = append(gaugeNames, name)
		}
		sort.Strings(gaugeNames)
		for _, name := range gaugeNames {
			fmt.Fprintf(&b, "# TYPE %s gauge\n%s %g\n", name, name, m.gauges[name]())
		}

		keys := make([]string, 0, len(m.routes))
		for k := range m.routes {
			keys = append(keys, k)
		}
		sort.Strings(keys)

		b.WriteString("# HELP ascend_http_requests_total Total HTTP requests served.\n")
		b.WriteString("# TYPE ascend_http_requests_total counter\n")
		for _, k := range keys {
			method, pattern, status := splitKey(k)
			fmt.Fprintf(&b, "ascend_http_requests_total{method=%q,path=%q,status=%q} %d\n",
				method, pattern, status, m.routes[k].count)
		}

		b.WriteString("# HELP ascend_http_request_duration_seconds HTTP request latency.\n")
		b.WriteString("# TYPE ascend_http_request_duration_seconds histogram\n")
		for _, k := range keys {
			method, pattern, status := splitKey(k)
			rs := m.routes[k]
			cumulative := uint64(0)
			for i, ub := range buckets {
				cumulative += rs.bucketCount[i]
				fmt.Fprintf(&b, "ascend_http_request_duration_seconds_bucket{method=%q,path=%q,status=%q,le=%q} %d\n",
					method, pattern, status, formatFloat(ub), cumulative)
			}
			fmt.Fprintf(&b, "ascend_http_request_duration_seconds_bucket{method=%q,path=%q,status=%q,le=\"+Inf\"} %d\n",
				method, pattern, status, rs.count)
			fmt.Fprintf(&b, "ascend_http_request_duration_seconds_sum{method=%q,path=%q,status=%q} %g\n",
				method, pattern, status, rs.durSum)
			fmt.Fprintf(&b, "ascend_http_request_duration_seconds_count{method=%q,path=%q,status=%q} %d\n",
				method, pattern, status, rs.count)
		}

		_, _ = w.Write([]byte(b.String()))
	})
}

func splitKey(k string) (method, pattern, status string) {
	first := strings.Index(k, " ")
	last := strings.LastIndex(k, " ")
	return k[:first], k[first+1 : last], k[last+1:]
}

func formatFloat(f float64) string {
	return strconv.FormatFloat(f, 'g', -1, 64)
}
