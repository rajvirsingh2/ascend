package main

import (
	"context"
	"fmt"
	"os"
	"time"

	"ascend-backend/internal/mlservice"
	"ascend-backend/pkg/config"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		fmt.Println("Error loading config:", err)
		return
	}

	mlClient := mlservice.NewClient(mlservice.Config{
		SpaceURL: cfg.MLServiceURL,
		HFToken:  cfg.HFToken,
	})

	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	profile := mlservice.UserProfile{
		Level:     1,
		Interests: []mlservice.Interest{{Category: "productivity", Subcategory: "coding", Priority: "1"}},
	}

	quests, err := mlClient.GenerateQuests(ctx, profile)
	if err != nil {
		fmt.Println("Error generating quests:", err)
		os.Exit(1)
	}

	fmt.Printf("Success! Generated %d quests\n", len(quests))
	for _, q := range quests {
		fmt.Printf("- %s: %s\n", q.Title, q.Description)
	}
}
