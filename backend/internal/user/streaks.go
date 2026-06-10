package user

import "time"

// CalcStreaks computes the user's current and best daily activity streaks.
//
// days must be DISTINCT activity dates (midnight-truncated, any location),
// sorted DESCENDING. today is the current date (midnight-truncated, same
// location).
//
// The current streak counts consecutive days ending today — or ending
// yesterday, so the streak isn't reported as 0 in the morning before the
// user has done anything yet.
func CalcStreaks(days []time.Time, today time.Time) (current, best int) {
	if len(days) == 0 {
		return 0, 0
	}

	sameDay := func(a, b time.Time) bool {
		return a.Year() == b.Year() && a.YearDay() == b.YearDay()
	}
	prevDay := func(t time.Time) time.Time { return t.AddDate(0, 0, -1) }

	// Walk runs of consecutive days (input is descending).
	run := 1
	firstRun := 1 // length of the run that starts at days[0]
	inFirstRun := true
	for i := 1; i < len(days); i++ {
		if sameDay(days[i], prevDay(days[i-1])) {
			run++
			if inFirstRun {
				firstRun = run
			}
		} else {
			if run > best {
				best = run
			}
			run = 1
			inFirstRun = false
		}
	}
	if run > best {
		best = run
	}

	// Current streak only counts if the most recent activity was today or
	// yesterday; otherwise the chain is broken.
	if sameDay(days[0], today) || sameDay(days[0], prevDay(today)) {
		current = firstRun
	}
	return current, best
}
