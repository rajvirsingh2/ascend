package validator

import (
	"fmt"
	"regexp"
	"strings"
)

var (
	emailRegex    = regexp.MustCompile(`^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$`)
	usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]{3,30}$`)
	sqlInjection  = regexp.MustCompile(`(?i)(SELECT|INSERT|UPDATE|DELETE|DROP|UNION|--|;|\/\*)`)
)

func Email(email string) error {
	email = strings.TrimSpace(email)
	if len(email) > 254 {
		return fmt.Errorf("email too long")
	}
	if !emailRegex.MatchString(email) {
		return fmt.Errorf("invalid email format")
	}
	return nil
}

func Username(username string) error {
	if !usernameRegex.MatchString(username) {
		return fmt.Errorf("username must be 3-30 chars, letters/numbers/underscores only")
	}
	return nil
}

func Password(password string) error {
	if len(password) < 8 {
		return fmt.Errorf("password must be at least 8 characters")
	}
	if len(password) > 128 {
		return fmt.Errorf("password too long")
	}
	hasUpper := regexp.MustCompile(`[A-Z]`).MatchString(password)
	hasDigit := regexp.MustCompile(`[0-9]`).MatchString(password)
	if !hasUpper || !hasDigit {
		return fmt.Errorf("password must contain at least one uppercase letter and one number")
	}
	return nil
}

func SafeString(s string) error {
	if sqlInjection.MatchString(s) {
		return fmt.Errorf("input contains invalid characters")
	}
	return nil
}
