package user

import (
	"testing"
	"time"
)

func d(daysAgo int) time.Time {
	base := time.Date(2026, 6, 10, 0, 0, 0, 0, time.UTC)
	return base.AddDate(0, 0, -daysAgo)
}

var today = d(0)

func TestCalcStreaksEmpty(t *testing.T) {
	cur, best := CalcStreaks(nil, today)
	if cur != 0 || best != 0 {
		t.Fatalf("empty: cur=%d best=%d, want 0,0", cur, best)
	}
}

func TestCalcStreaksActiveToday(t *testing.T) {
	// Activity today, yesterday, day before → streak of 3.
	cur, best := CalcStreaks([]time.Time{d(0), d(1), d(2)}, today)
	if cur != 3 || best != 3 {
		t.Fatalf("cur=%d best=%d, want 3,3", cur, best)
	}
}

func TestCalcStreaksSingleCompletionToday(t *testing.T) {
	// The reported bug: complete one quest today → streak must be 1, not 0.
	cur, best := CalcStreaks([]time.Time{d(0)}, today)
	if cur != 1 || best != 1 {
		t.Fatalf("cur=%d best=%d, want 1,1", cur, best)
	}
}

func TestCalcStreaksEndedYesterdayStillCounts(t *testing.T) {
	// No activity yet today (it's morning) — streak ending yesterday holds.
	cur, best := CalcStreaks([]time.Time{d(1), d(2), d(3), d(4)}, today)
	if cur != 4 || best != 4 {
		t.Fatalf("cur=%d best=%d, want 4,4", cur, best)
	}
}

func TestCalcStreaksBrokenChain(t *testing.T) {
	// Last activity 3 days ago → current streak dead, best preserved.
	cur, best := CalcStreaks([]time.Time{d(3), d(4), d(5), d(6), d(7)}, today)
	if cur != 0 {
		t.Fatalf("cur=%d, want 0 (chain broken)", cur)
	}
	if best != 5 {
		t.Fatalf("best=%d, want 5", best)
	}
}

func TestCalcStreaksGapInMiddle(t *testing.T) {
	// today+yesterday (run of 2), gap, then an older run of 4.
	days := []time.Time{d(0), d(1), d(3), d(4), d(5), d(6)}
	cur, best := CalcStreaks(days, today)
	if cur != 2 {
		t.Fatalf("cur=%d, want 2", cur)
	}
	if best != 4 {
		t.Fatalf("best=%d, want 4", best)
	}
}

func TestCalcStreaksAcrossMonthBoundary(t *testing.T) {
	// June 1 back through May 29 — consecutive across the boundary.
	days := []time.Time{
		time.Date(2026, 6, 1, 0, 0, 0, 0, time.UTC),
		time.Date(2026, 5, 31, 0, 0, 0, 0, time.UTC),
		time.Date(2026, 5, 30, 0, 0, 0, 0, time.UTC),
		time.Date(2026, 5, 29, 0, 0, 0, 0, time.UTC),
	}
	ref := time.Date(2026, 6, 1, 0, 0, 0, 0, time.UTC)
	cur, best := CalcStreaks(days, ref)
	if cur != 4 || best != 4 {
		t.Fatalf("cur=%d best=%d, want 4,4", cur, best)
	}
}
