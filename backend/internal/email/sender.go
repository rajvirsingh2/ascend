package email

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/smtp"
	"time"

	"ascend-backend/pkg/config"
)

// Sender sends transactional emails.
// It uses SMTP if configured, falls back to Resend if configured,
// and logs to stdout if neither is available.
type Sender struct {
	cfg     *config.Config
	devMode bool
}

// NewSender creates a Sender.
func NewSender(cfg *config.Config) *Sender {
	devMode := cfg.ResendAPIKey == "" && cfg.SMTPHost == ""
	if devMode {
		log.Println("[email] WARNING: no RESEND_API_KEY or SMTP_HOST set — emails will be logged to console only")
	}
	return &Sender{
		cfg:     cfg,
		devMode: devMode,
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

	// Try SMTP first if configured
	if s.cfg.SMTPHost != "" && s.cfg.SMTPUser != "" && s.cfg.SMTPPassword != "" {
		return s.sendSMTP(toEmail, subject, html)
	}

	// Fallback to Resend
	if s.cfg.ResendAPIKey != "" {
		return s.sendResend(ctx, toEmail, subject, html)
	}

	return fmt.Errorf("no email provider configured")
}

func (s *Sender) sendSMTP(toEmail, subject, html string) error {
	auth := smtp.PlainAuth("", s.cfg.SMTPUser, s.cfg.SMTPPassword, s.cfg.SMTPHost)

	msg := fmt.Sprintf("From: %s\r\nTo: %s\r\nSubject: %s\r\nContent-Type: text/html; charset=UTF-8\r\n\r\n%s",
		s.cfg.EmailFrom, toEmail, subject, html)

	addr := fmt.Sprintf("%s:%s", s.cfg.SMTPHost, s.cfg.SMTPPort)
	err := smtp.SendMail(addr, auth, s.cfg.SMTPUser, []string{toEmail}, []byte(msg))
	if err != nil {
		return fmt.Errorf("smtp send: %w", err)
	}

	log.Printf("[email] sent %q to %s via SMTP", subject, toEmail)
	return nil
}

func (s *Sender) sendResend(ctx context.Context, toEmail, subject, html string) error {
	payload := resendPayload{
		From:    s.cfg.EmailFrom,
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
	req.Header.Set("Authorization", "Bearer "+s.cfg.ResendAPIKey)
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

	log.Printf("[email] sent %q to %s via Resend", subject, toEmail)
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
