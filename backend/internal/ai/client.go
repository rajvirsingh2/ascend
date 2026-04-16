package ai

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

type GenerateRequest struct {
	UserID      string `json:"user_id"`
	GenerateFor string `json:"generate_for"`
}

type GeneratedQuest struct {
	Title       string `json:"title"`
	Description string `json:"description"`
	Type        string `json:"type"`
	Difficulty  int    `json:"difficulty"`
	SkillArea   string `json:"skill_area"`
	XPReward    int    `json:"xp_reward"`
	Rationale   string `json:"rationale"`
}

type GenerateResponse struct {
	Quests []GeneratedQuest `json:"quests"`
}

type Client struct {
	baseURL    string
	httpClient *http.Client
}

func NewClient(ragServiceURL string) *Client {
	return &Client{
		baseURL: ragServiceURL,
		httpClient: &http.Client{
			Timeout: 45 * time.Second,
		},
	}
}

func (c *Client) GenerateQuests(ctx context.Context, req GenerateRequest) (*GenerateResponse, error) {
	body, err := json.Marshal(req)
	if err != nil {
		return nil, fmt.Errorf("marshalling request: %w", err)
	}

	httpReq, err := http.NewRequestWithContext(
		ctx, http.MethodPost,
		c.baseURL+"/generate",
		bytes.NewReader(body),
	)
	if err != nil {
		return nil, fmt.Errorf("creating request: %w", err)
	}
	httpReq.Header.Set("Content-Type", "application/json")

	resp, err := c.httpClient.Do(httpReq)
	if err != nil {
		return nil, fmt.Errorf("calling rag service: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("rag service returned %d", resp.StatusCode)
	}

	var result GenerateResponse
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("decoding response: %w", err)
	}

	return &result, nil
}
