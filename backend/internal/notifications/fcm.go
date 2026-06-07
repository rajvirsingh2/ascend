package notifications

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"time"

	"ascend-backend/pkg/config"

	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/oauth2"
	"golang.org/x/oauth2/google"
)

// FCMNotifier sends push notifications via Firebase Cloud Messaging HTTP v1 API.
// Free forever — no credit card needed.
//
// Setup (one time, 5 minutes):
//  1. Go to console.firebase.google.com
//  2. Create a project (or use existing one)
//  3. Add Android app → package name: com.ascend.app
//  4. Download google-services.json → place in android/app/
//  5. Project Settings → Service Accounts → Generate new private key
//     → Download the JSON → save as firebase-service-account.json in backend root
//  6. Add to .env:
//     FCM_CREDENTIALS_JSON=./firebase-service-account.json
//     FCM_PROJECT_ID=your-firebase-project-id
type FCMNotifier struct {
	projectID   string
	tokenSource oauth2.TokenSource
	db          *pgxpool.Pool
	httpClient  *http.Client
}

// Notification is the payload to send.
type Notification struct {
	Title string
	Body  string
	Data  map[string]string // arbitrary key-value pairs delivered to the app
}

// NewFCMNotifier creates a notifier. Returns nil-safely if credentials are missing.
func NewFCMNotifier(ctx context.Context, cfg *config.Config, db *pgxpool.Pool) (*FCMNotifier, error) {
	if cfg.FCMCredentialsJSON == "" {
		log.Println("[fcm] WARNING: FCM_CREDENTIALS_JSON not set — push notifications disabled")
		return nil, nil
	}

	// The credentials are provided directly as a JSON string
	credBytes := []byte(cfg.FCMCredentialsJSON)

	// Create OAuth2 token source using the service account.
	//nolint:staticcheck // we control the source of the credentials file
	creds, err := google.CredentialsFromJSONWithParams(ctx, credBytes, google.CredentialsParams{
		Scopes: []string{"https://www.googleapis.com/auth/firebase.messaging"},
	})
	if err != nil {
		return nil, fmt.Errorf("parse FCM credentials: %w", err)
	}

	return &FCMNotifier{
		projectID:   cfg.FCMProjectID,
		tokenSource: creds.TokenSource,
		db:          db,
		httpClient:  &http.Client{Timeout: 15 * time.Second},
	}, nil
}

// SendToUser looks up all FCM tokens for the user and sends the notification.
// Silently removes expired/invalid tokens from the database.
func (n *FCMNotifier) SendToUser(ctx context.Context, userID string, notif Notification) error {
	if n == nil {
		return nil // dev mode — no-op
	}

	rows, err := n.db.Query(ctx, `SELECT token FROM fcm_tokens WHERE user_id = $1`, userID)
	if err != nil {
		return fmt.Errorf("get FCM tokens: %w", err)
	}
	defer rows.Close()

	var tokens []string
	for rows.Next() {
		var t string
		if err := rows.Scan(&t); err == nil {
			tokens = append(tokens, t)
		}
	}

	if len(tokens) == 0 {
		return nil // user has no registered device
	}

	var errs []error
	for _, token := range tokens {
		if err := n.sendToToken(ctx, token, notif); err != nil {
			log.Printf("[fcm] send to token error (user=%s): %v", userID, err)
			// Remove stale tokens (FCM returns 404 for invalid tokens).
			if isInvalidTokenError(err) {
				_, _ = n.db.Exec(ctx, `DELETE FROM fcm_tokens WHERE token = $1`, token)
			}
			errs = append(errs, err)
		}
	}

	if len(errs) == len(tokens) {
		return fmt.Errorf("all FCM sends failed: %v", errs[0])
	}
	return nil
}

// RegisterToken registers or refreshes a device token for a user.
func (n *FCMNotifier) RegisterToken(ctx context.Context, userID, token string) error {
	if n == nil {
		return nil
	}
	_, err := n.db.Exec(ctx,
		`INSERT INTO fcm_tokens (user_id, token, updated_at) VALUES ($1, $2, NOW())
		 ON CONFLICT (token) DO UPDATE SET user_id = EXCLUDED.user_id, updated_at = NOW()`,
		userID, token,
	)
	return err
}

// ── Internal ──────────────────────────────────────────────────────────────

type fcmMessage struct {
	Message struct {
		Token        string            `json:"token"`
		Notification *fcmNotification  `json:"notification,omitempty"`
		Data         map[string]string `json:"data,omitempty"`
		Android      *fcmAndroid       `json:"android,omitempty"`
	} `json:"message"`
}

type fcmNotification struct {
	Title string `json:"title"`
	Body  string `json:"body"`
}

type fcmAndroid struct {
	Priority     string           `json:"priority"`
	Notification *fcmAndroidNotif `json:"notification,omitempty"`
}

type fcmAndroidNotif struct {
	Sound       string `json:"sound"`
	ClickAction string `json:"click_action"`
}

func (n *FCMNotifier) sendToToken(ctx context.Context, token string, notif Notification) error {
	var msg fcmMessage
	msg.Message.Token = token
	msg.Message.Notification = &fcmNotification{
		Title: notif.Title,
		Body:  notif.Body,
	}
	msg.Message.Data = notif.Data
	msg.Message.Android = &fcmAndroid{
		Priority: "high",
		Notification: &fcmAndroidNotif{
			Sound:       "default",
			ClickAction: "FLUTTER_NOTIFICATION_CLICK",
		},
	}

	payload, err := json.Marshal(msg)
	if err != nil {
		return err
	}

	url := fmt.Sprintf("https://fcm.googleapis.com/v1/projects/%s/messages:send", n.projectID)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, url, bytes.NewReader(payload))
	if err != nil {
		return err
	}

	// Get fresh OAuth2 token.
	token2, err := n.tokenSource.Token()
	if err != nil {
		return fmt.Errorf("get oauth token: %w", err)
	}
	req.Header.Set("Authorization", "Bearer "+token2.AccessToken)
	req.Header.Set("Content-Type", "application/json")

	resp, err := n.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("FCM http: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 400 {
		body, _ := io.ReadAll(resp.Body)
		return &fcmError{status: resp.StatusCode, body: string(body)}
	}
	return nil
}

type fcmError struct {
	status int
	body   string
}

func (e *fcmError) Error() string {
	return fmt.Sprintf("FCM error %d: %s", e.status, e.body)
}

func isInvalidTokenError(err error) bool {
	if e, ok := err.(*fcmError); ok {
		return e.status == 404 || e.status == 410
	}
	return false
}
