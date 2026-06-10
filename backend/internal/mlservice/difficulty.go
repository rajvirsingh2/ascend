package mlservice

import "strings"

// MapDifficulty converts a model-produced difficulty label to the 1-5 scale
// used by the quests table. It is the single mapping for every consumer
// (HTTP generate handler, midnight generation worker) and tolerates every
// label convention the model has emitted: "easy"/"Easy", "expert"/"Epic",
// numeric strings, stray whitespace.
func MapDifficulty(s string) int {
	switch strings.ToLower(strings.TrimSpace(s)) {
	case "easy", "1":
		return 1
	case "medium", "2":
		return 2
	case "hard", "3":
		return 3
	case "expert", "epic", "4":
		return 4
	case "legendary", "5":
		return 5
	default:
		return 1
	}
}
