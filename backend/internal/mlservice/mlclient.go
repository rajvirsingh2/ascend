package mlservice

import (
	"bufio"
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/redis/go-redis/v9"
)

type Interest struct {
	Category    string `json:"category"`
	Subcategory string `json:"subcategory"`
	Priority    string `json:"priority"`
}

type Physique struct {
	BMI           *float64 `json:"bmi,omitempty"`
	Goal          string   `json:"goal,omitempty"`
	ActivityLevel string   `json:"activity_level,omitempty"`
}

type UserProfile struct {
	Level           int        `json:"level"`
	Rank            string     `json:"rank"`
	Archetype       string     `json:"archetype"`
	Interests       []Interest `json:"interests"`
	Goal            string     `json:"goal"`
	CurrentStreak   int        `json:"current_streak"`
	QuestsCompleted int        `json:"quests_completed"`
	CompletionRate  float64    `json:"completion_rate"`
	Physique        *Physique  `json:"physique,omitempty"`
	RequestedDaily  int        `json:"requested_daily"`
	RequestedWeekly int        `json:"requested_weekly"`
}

func cleanJSONString(s string) string {
	s = strings.TrimSpace(s)
	if strings.HasPrefix(s, "```") {
		idx := strings.Index(s, "\n")
		if idx != -1 {
			s = s[idx+1:]
		}
		s = strings.TrimSpace(s)
		s = strings.TrimSuffix(s, "```")
	}
	return strings.TrimSpace(s)
}

func parseTruncatedJSONList(raw string) ([]Quest, error) {
	raw = cleanJSONString(raw)

	var quests []Quest
	if err := json.Unmarshal([]byte(raw), &quests); err == nil && len(quests) > 0 {
		return quests, nil
	}

	// Try to salvage truncated JSON by finding the last closing brace
	lastBrace := strings.LastIndex(raw, "}")
	if lastBrace == -1 {
		return nil, errors.New("no valid objects found in truncated JSON")
	}

	truncated := raw[:lastBrace+1] + "]"

	var truncatedQuests []Quest
	if err := json.Unmarshal([]byte(truncated), &truncatedQuests); err == nil && len(truncatedQuests) > 0 {
		return truncatedQuests, nil
	}

	return nil, errors.New("failed to salvage truncated JSON")
}

type Quest struct {
	Title           string `json:"title"`
	Description     string `json:"description"`
	Difficulty      string `json:"difficulty"`
	XPReward        int    `json:"xp_reward"`
	SkillArea       string `json:"skill_area"`
	Subcategory     string `json:"subcategory"`
	QuestType       string `json:"quest_type"`
	DurationMinutes int    `json:"duration_minutes"`
}

type gradioRequest struct {
	Data []string `json:"data"`
}

type gradioResponse struct {
	Data     []string `json:"data"`
	Duration float64  `json:"duration,omitempty"`
}

type eventResponse struct {
	EventID string `json:"event_id"`
}

type Client struct {
	spaceURL   string
	httpClient *http.Client
	redis      *redis.Client
	cacheTTL   time.Duration
	hfToken    string
}

type Config struct {
	SpaceURL string
	Redis    *redis.Client
	HFToken  string
}

func NewClient(cfg Config) *Client {
	return &Client{
		spaceURL: cfg.SpaceURL,
		httpClient: &http.Client{
			Timeout: 0,
		},
		redis:    cfg.Redis,
		cacheTTL: 24 * time.Hour,
		hfToken:  cfg.HFToken,
	}
}

func (c *Client) GenerateQuests(ctx context.Context, profile UserProfile) ([]Quest, error) {

	quests, err := c.callSpace(ctx, profile)
	if err != nil {
		return nil, err
	}

	return quests, nil
}

var (
	ErrSpaceSleeping = errors.New("hf space is sleeping or unavailable")
	ErrInvalidOutput = errors.New("model returned invalid JSON")
)

func (c *Client) callSpace(ctx context.Context, profile UserProfile) ([]Quest, error) {
	profileJSON, err := json.Marshal(profile)
	if err != nil {
		return nil, fmt.Errorf("marshal profile: %w", err)
	}

	reqBody := gradioRequest{Data: []string{string(profileJSON)}}
	bodyBytes, err := json.Marshal(reqBody)
	if err != nil {
		return nil, fmt.Errorf("marshal request: %w", err)
	}

	endpoint := c.spaceURL + "/call/predict"
	req, err := http.NewRequestWithContext(ctx, "POST", endpoint, bytes.NewReader(bodyBytes))
	if err != nil {
		return nil, err
	}
	req.Header.Set("Content-Type", "application/json")
	if c.hfToken != "" {
		req.Header.Set("Authorization", "Bearer "+c.hfToken)
	}

	resp, err := c.sendWithRetry(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode == http.StatusServiceUnavailable {
		return nil, ErrSpaceSleeping
	}
	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("space returned %d: %s", resp.StatusCode, string(body))
	}

	var evResp eventResponse
	if err := json.NewDecoder(resp.Body).Decode(&evResp); err != nil {
		return nil, fmt.Errorf("decode event response: %w", err)
	}
	if evResp.EventID == "" {
		return nil, fmt.Errorf("no event_id returned from space")
	}

	// ── 2. Poll for SSE stream ──
	streamEndpoint := fmt.Sprintf("%s/%s", endpoint, evResp.EventID)
	streamReq, err := http.NewRequestWithContext(ctx, "GET", streamEndpoint, nil)
	if err != nil {
		return nil, err
	}
	if c.hfToken != "" {
		streamReq.Header.Set("Authorization", "Bearer "+c.hfToken)
	}
	streamReq.Header.Set("Accept", "text/event-stream")

	streamResp, err := c.httpClient.Do(streamReq)
	if err != nil {
		return nil, err
	}
	defer streamResp.Body.Close()

	scanner := bufio.NewScanner(streamResp.Body)
	// Some ML payloads can be large; increase scanner buffer if needed
	const maxCapacity = 1024 * 1024
	buf := make([]byte, maxCapacity)
	scanner.Buffer(buf, maxCapacity)

	var lastEvent string
	for scanner.Scan() {
		line := scanner.Text()
		if line == "" {
			continue
		}

		if strings.HasPrefix(line, "event: ") {
			lastEvent = strings.TrimPrefix(line, "event: ")
			continue
		}

		if strings.HasPrefix(line, "data: ") && lastEvent == "complete" {
			dataStr := strings.TrimPrefix(line, "data: ")

			// Gradio 4 complete event data is an array containing the outputs.
			// e.g. ["[\"json...\"]"]
			var outputs []string
			if err := json.Unmarshal([]byte(dataStr), &outputs); err == nil && len(outputs) > 0 {
				var quests []Quest
				cleaned := cleanJSONString(outputs[0])
				if err := json.Unmarshal([]byte(cleaned), &quests); err == nil && len(quests) > 0 {
					return quests, nil
				}
			}

			// Fallback: try parsing as raw string if Gradio wrapped it differently
			var rawStr string
			if err := json.Unmarshal([]byte(dataStr), &rawStr); err == nil {
				var quests []Quest
				cleaned := cleanJSONString(rawStr)
				if err := json.Unmarshal([]byte(cleaned), &quests); err == nil && len(quests) > 0 {
					return quests, nil
				}
			}

			// Fallback: gradioResponse struct
			var gradioResp gradioResponse
			if err := json.Unmarshal([]byte(dataStr), &gradioResp); err == nil && len(gradioResp.Data) > 0 {
				var quests []Quest
				cleaned := cleanJSONString(gradioResp.Data[0])
				if err := json.Unmarshal([]byte(cleaned), &quests); err == nil && len(quests) > 0 {
					return quests, nil
				}
			}

			return nil, fmt.Errorf("failed to parse complete event data: %s", dataStr)
		} else if strings.HasPrefix(line, "data: ") && lastEvent == "error" {
			dataStr := strings.TrimPrefix(line, "data: ")

			// Try to salvage partial JSON if the model generated some quests but errored out at the end
			var errResp struct {
				Error string `json:"error"`
				Raw   string `json:"raw"`
			}
			if err := json.Unmarshal([]byte(dataStr), &errResp); err == nil && errResp.Raw != "" {
				if quests, parseErr := parseTruncatedJSONList(errResp.Raw); parseErr == nil && len(quests) > 0 {
					return quests, nil
				}
			}

			return nil, fmt.Errorf("model error: %s", dataStr)
		}
	}

	if err := scanner.Err(); err != nil {
		return nil, fmt.Errorf("stream error: %w", err)
	}

	return nil, ErrInvalidOutput
}

func (c *Client) sendWithRetry(req *http.Request) (*http.Response, error) {
	resp, err := c.httpClient.Do(req)
	if err == nil && resp.StatusCode != http.StatusServiceUnavailable {
		return resp, nil
	}
	if resp != nil {
		resp.Body.Close()
	}

	select {
	case <-time.After(15 * time.Second):
	case <-req.Context().Done():
		return nil, req.Context().Err()
	}

	req2 := req.Clone(req.Context())
	return c.httpClient.Do(req2)
}

func (c *Client) Health(ctx context.Context) error {
	req, err := http.NewRequestWithContext(ctx, "GET", c.spaceURL+"/", nil)
	if err != nil {
		return err
	}
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("space unhealthy: %d", resp.StatusCode)
	}
	return nil
}
