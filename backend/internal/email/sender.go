package email

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"time"
)

// Sender sends transactional emails.
// In dev (no API key) it logs the email to stdout instead of sending.
type Sender struct {
	apiKey   string
	fromAddr string
	devMode  bool
}

// NewSender creates a Sender.
//
//	apiKey   — Resend API key (re_xxx). Get free at resend.com — no credit card.
//	fromAddr — verified sender, e.g. "Ascend <noreply@yourdomain.com>"
//	           In dev use "Ascend <onboarding@resend.dev>" (Resend's test address).
func NewSender(apiKey, fromAddr string) *Sender {
	if apiKey == "" {
		log.Println("[email] WARNING: no RESEND_API_KEY set — emails will be logged to console only")
	}
	return &Sender{
		apiKey:   apiKey,
		fromAddr: fromAddr,
		devMode:  apiKey == "",
	}
}

// ── Public send methods ───────────────────────────────────────────────────

// SendOTP sends a 6-digit OTP verification email.
func (s *Sender) SendOTP(ctx context.Context, toEmail, username, otp string) error {
	subject := "Your Ascend verification code"
	html := buildOTPEmail(username, otp)
	return s.send(ctx, toEmail, subject, html)
}

// SendPasswordReset sends a password reset link.
func (s *Sender) SendPasswordReset(ctx context.Context, toEmail, username, resetLink string) error {
	subject := "Reset your Ascend password"
	html := buildResetEmail(username, resetLink)
	return s.send(ctx, toEmail, subject, html)
}

// SendWelcome sends a welcome email after the user verifies their account.
func (s *Sender) SendWelcome(ctx context.Context, toEmail, username string) error {
	subject := "Welcome to Ascend — your journey begins"
	html := buildWelcomeEmail(username)
	return s.send(ctx, toEmail, subject, html)
}

// ── Core send ─────────────────────────────────────────────────────────────

type resendPayload struct {
	From    string   `json:"from"`
	To      []string `json:"to"`
	Subject string   `json:"subject"`
	HTML    string   `json:"html"`
}

func (s *Sender) send(ctx context.Context, toEmail, subject, html string) error {
	if s.devMode {
		log.Printf("[email-dev] TO=%s SUBJECT=%s\n--- HTML ---\n%s\n------------", toEmail, subject, html)
		return nil
	}

	payload := resendPayload{
		From:    s.fromAddr,
		To:      []string{toEmail},
		Subject: subject,
		HTML:    html,
	}
	body, err := json.Marshal(payload)
	if err != nil {
		return fmt.Errorf("marshal email payload: %w", err)
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, "https://api.resend.com/emails", bytes.NewReader(body))
	if err != nil {
		return fmt.Errorf("build resend request: %w", err)
	}
	req.Header.Set("Authorization", "Bearer "+s.apiKey)
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{Timeout: 10 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return fmt.Errorf("resend http: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 400 {
		respBody, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("resend API error %d: %s", resp.StatusCode, string(respBody))
	}

	log.Printf("[email] sent %q to %s", subject, toEmail)
	return nil
}

// ── HTML templates ────────────────────────────────────────────────────────

func buildOTPEmail(username, otp string) string {
	return fmt.Sprintf(`
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { background: #0A0A0F; color: #F1F5F9; font-family: 'Segoe UI', sans-serif; margin: 0; padding: 0; }
    .container { max-width: 480px; margin: 40px auto; background: #1A1A2E; border: 1px solid #2D2D4E; border-radius: 12px; padding: 40px; }
    .logo { font-size: 28px; font-weight: 900; letter-spacing: 6px; color: #F1F5F9; margin-bottom: 8px; }
    .tagline { font-size: 11px; letter-spacing: 3px; color: #06B6D4; margin-bottom: 32px; }
    .label { font-size: 11px; letter-spacing: 2px; color: #94A3B8; margin-bottom: 12px; }
    .otp-box { background: #0A0A0F; border: 1px solid #7C3AED; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0; }
    .otp { font-size: 40px; font-weight: 900; letter-spacing: 12px; color: #A855F7; }
    .note { font-size: 12px; color: #475569; margin-top: 24px; line-height: 1.6; }
    .divider { border: none; border-top: 1px solid #2D2D4E; margin: 24px 0; }
  </style>
</head>
<body>
  <div class="container">
    <div class="logo">ASCEND</div>
    <div class="tagline">LEVEL UP IN REAL LIFE</div>
    <p style="color:#94A3B8; font-size:14px;">Hunter <strong style="color:#F1F5F9;">%s</strong>, your verification code:</p>
    <div class="label">◈ VERIFICATION CODE</div>
    <div class="otp-box">
      <div class="otp">%s</div>
    </div>
    <hr class="divider">
    <p class="note">This code expires in <strong>10 minutes</strong>. Do not share it with anyone.<br>If you did not request this, you can safely ignore this email.</p>
  </div>
</body>
</html>`, username, otp)
}

func buildResetEmail(username, resetLink string) string {
	return fmt.Sprintf(`
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { background: #0A0A0F; color: #F1F5F9; font-family: 'Segoe UI', sans-serif; margin: 0; padding: 0; }
    .container { max-width: 480px; margin: 40px auto; background: #1A1A2E; border: 1px solid #2D2D4E; border-radius: 12px; padding: 40px; }
    .logo { font-size: 28px; font-weight: 900; letter-spacing: 6px; color: #F1F5F9; }
    .tagline { font-size: 11px; letter-spacing: 3px; color: #06B6D4; margin-bottom: 32px; }
    .btn { display: inline-block; background: linear-gradient(90deg, #7C3AED, #06B6D4); color: #fff; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 900; letter-spacing: 2px; font-size: 13px; margin: 24px 0; }
    .note { font-size: 12px; color: #475569; line-height: 1.6; }
    .link { word-break: break-all; font-size: 11px; color: #475569; margin-top: 8px; }
  </style>
</head>
<body>
  <div class="container">
    <div class="logo">ASCEND</div>
    <div class="tagline">LEVEL UP IN REAL LIFE</div>
    <p style="color:#94A3B8; font-size:14px;">Hunter <strong style="color:#F1F5F9;">%s</strong>, reset your password:</p>
    <a class="btn" href="%s">RESET PASSWORD</a>
    <p class="note">This link expires in <strong>1 hour</strong>.<br>If you didn't request a reset, ignore this email — your account is safe.</p>
    <p class="link">%s</p>
  </div>
</body>
</html>`, username, resetLink, resetLink)
}

func buildWelcomeEmail(username string) string {
	return fmt.Sprintf(`
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { background: #0A0A0F; color: #F1F5F9; font-family: 'Segoe UI', sans-serif; margin: 0; padding: 0; }
    .container { max-width: 480px; margin: 40px auto; background: #1A1A2E; border: 1px solid #7C3AED; border-radius: 12px; padding: 40px; }
    .logo { font-size: 28px; font-weight: 900; letter-spacing: 6px; color: #F1F5F9; }
    .tagline { font-size: 11px; letter-spacing: 3px; color: #06B6D4; margin-bottom: 32px; }
    .rank { display: inline-block; background: rgba(149,165,166,0.1); border: 1px solid #95A5A6; border-radius: 4px; padding: 4px 10px; font-size: 11px; font-weight: 900; letter-spacing: 2px; color: #95A5A6; }
  </style>
</head>
<body>
  <div class="container">
    <div class="logo">ASCEND</div>
    <div class="tagline">LEVEL UP IN REAL LIFE</div>
    <span class="rank">RANK E</span>
    <p style="color:#F1F5F9; font-size:16px; font-weight:700; margin-top:16px;">Hunter %s has entered the System.</p>
    <p style="color:#94A3B8; font-size:14px; line-height:1.6;">Your journey to rank S begins now. Complete daily quests, build habits, and watch your stats ascend.<br><br>The System is watching.</p>
  </div>
</body>
</html>`, username)
}
