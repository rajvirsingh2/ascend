package game

import "testing"

func TestXPForLevel(t *testing.T) {
	if got := XPForLevel(1); got != 100 {
		t.Errorf("XPForLevel(1) = %d, want 100", got)
	}
	// Monotonically increasing.
	prev := 0
	for lvl := 1; lvl <= 50; lvl++ {
		cur := XPForLevel(lvl)
		if cur <= prev {
			t.Fatalf("XPForLevel not increasing at level %d: %d <= %d", lvl, cur, prev)
		}
		prev = cur
	}
}

func TestQuestXPReward(t *testing.T) {
	// Level 1: no penalty.
	if got := QuestXPReward(2, 1); got != 50 {
		t.Errorf("QuestXPReward(2,1) = %d, want 50", got)
	}
	// Higher level shrinks reward, but never below the 0.4 floor.
	base := QuestXPReward(4, 1)
	mid := QuestXPReward(4, 10)
	floor := QuestXPReward(4, 100)
	if !(base > mid && mid > floor) {
		t.Errorf("penalty not applied: base=%d mid=%d floor=%d", base, mid, floor)
	}
	if floor != int(float64(4*25)*0.4) {
		t.Errorf("floor = %d, want %d", floor, int(float64(4*25)*0.4))
	}
	// Reward never zero or negative for valid difficulties.
	for d := 1; d <= 5; d++ {
		for lvl := 1; lvl <= 99; lvl++ {
			if QuestXPReward(d, lvl) <= 0 {
				t.Fatalf("non-positive reward d=%d lvl=%d", d, lvl)
			}
		}
	}
}
