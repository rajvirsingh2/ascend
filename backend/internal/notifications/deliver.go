package notifications

import (
	"context"
	"log/slog"
	"strconv"
	"time"

	"ascend-backend/internal/models"
	"ascend-backend/internal/store/postgres"

	"github.com/google/uuid"
)

// Deliver is the single path for user-facing notifications: it persists the
// row that feeds the in-app notification screen AND pushes it via FCM (data-
// only message; the Android client renders the tray notification itself).
//
// notifType must be a valid Android NotifType enum name (e.g. "LEVEL_UP",
// "QUEST_COMPLETE", "DAILY_QUEST"). notifier may be nil (push disabled).
func Deliver(
	ctx context.Context,
	store *postgres.NotificationStore,
	notifier *FCMNotifier,
	userID, notifType, title, body string,
	xpDelta *int,
	actionRoute string,
	extra map[string]string,
) {
	row := &models.Notification{
		ID:          uuid.NewString(),
		UserID:      userID,
		Type:        notifType,
		Title:       title,
		Body:        body,
		XPDelta:     xpDelta,
		ActionRoute: &actionRoute,
		IsRead:      false,
		CreatedAt:   time.Now().UTC(),
	}
	if err := store.Insert(ctx, row); err != nil {
		slog.Error("notification insert failed", "user_id", userID, "type", notifType, "error", err)
	}

	if notifier == nil {
		return
	}
	data := map[string]string{
		"id":           row.ID,
		"type":         notifType,
		"action_route": actionRoute,
	}
	if xpDelta != nil {
		data["xp_delta"] = strconv.Itoa(*xpDelta)
	}
	for k, v := range extra {
		data[k] = v
	}
	_ = notifier.SendToUser(ctx, userID, Notification{
		Title: title,
		Body:  body,
		Data:  data,
	})
}
