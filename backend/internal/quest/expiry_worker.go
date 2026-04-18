package quest

import (
	"context"
	"log"
	"time"

	"ascend-backend/internal/store"
)

// StartExpiryWorker runs in a goroutine and marks overdue quests expired every hour.
func StartExpiryWorker(ctx context.Context, qs store.QuestStore) {
	ticker := time.NewTicker(1 * time.Hour)
	defer ticker.Stop()

	log.Println("quest expiry worker started")

	for {
		select {
		case <-ticker.C:
			if err := qs.ExpireOld(ctx); err != nil {
				log.Printf("expiry worker error: %v", err)
			}
		case <-ctx.Done():
			log.Println("quest expiry worker stopped")
			return
		}
	}
}
