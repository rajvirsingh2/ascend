package avatar

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/json"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"sort"
	"strconv"
	"strings"
	"time"

	"ascend-backend/pkg/config"
)

// Service handles avatar uploads to Cloudinary.
// Free tier: 25 GB storage, 25 GB bandwidth/month — no credit card needed.
//
// Setup (one time, 2 minutes):
//  1. Go to cloudinary.com → Sign up free (no credit card)
//  2. Dashboard shows: Cloud Name, API Key, API Secret
//  3. Add to .env:
//     CLOUDINARY_CLOUD_NAME=your_cloud_name
//     CLOUDINARY_API_KEY=your_api_key
//     CLOUDINARY_API_SECRET=your_api_secret
type Service struct {
	cloudName string
	apiKey    string
	apiSecret string
	devMode   bool
}

func NewService(cfg *config.Config) *Service {
	if cfg.CloudinaryCloudName == "" || cfg.CloudinaryAPIKey == "" {
		return &Service{devMode: true}
	}
	return &Service{
		cloudName: cfg.CloudinaryCloudName,
		apiKey:    cfg.CloudinaryAPIKey,
		apiSecret: cfg.CloudinaryAPISecret,
	}
}

// UploadResult contains the CDN URL and public ID after a successful upload.
type UploadResult struct {
	URL      string `json:"secure_url"`
	PublicID string `json:"public_id"`
}

// UploadFromReader uploads an image from a multipart file reader.
// Returns the secure CDN URL to store in the users table.
func (s *Service) UploadFromReader(ctx context.Context, userID string, reader io.Reader, contentType string) (*UploadResult, error) {
	if s.devMode {
		// Dev fallback — return a deterministic placeholder avatar.
		return &UploadResult{
			URL:      fmt.Sprintf("https://api.dicebear.com/8.x/bottts/svg?seed=%s", userID),
			PublicID: "dev_avatar_" + userID,
		}, nil
	}

	// Build signed upload parameters.
	timestamp := strconv.FormatInt(time.Now().Unix(), 10)
	publicID := fmt.Sprintf("ascend/avatars/%s", userID)
	params := map[string]string{
		"folder":              "ascend/avatars",
		"overwrite":           "true",
		"public_id":           publicID,
		"signature_algorithm": "sha256",
		"timestamp":           timestamp,
		"transformation":      "c_fill,g_face,h_256,w_256,r_max,q_auto,f_auto",
	}

	signature := s.sign(params)

	// Build multipart form.
	var buf bytes.Buffer
	mw := multipart.NewWriter(&buf)

	for k, v := range params {
		_ = mw.WriteField(k, v)
	}
	_ = mw.WriteField("api_key", s.apiKey)
	_ = mw.WriteField("signature", signature)

	// Determine file extension from content type.
	ext := contentTypeToExt(contentType)
	fw, err := mw.CreateFormFile("file", "avatar"+ext)
	if err != nil {
		return nil, fmt.Errorf("create form file: %w", err)
	}
	if _, err := io.Copy(fw, reader); err != nil {
		return nil, fmt.Errorf("copy image: %w", err)
	}
	mw.Close()

	// POST to Cloudinary upload API.
	uploadURL := fmt.Sprintf("https://api.cloudinary.com/v1_1/%s/image/upload", s.cloudName)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, uploadURL, &buf)
	if err != nil {
		return nil, fmt.Errorf("build request: %w", err)
	}
	req.Header.Set("Content-Type", mw.FormDataContentType())

	client := &http.Client{Timeout: 30 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("cloudinary upload: %w", err)
	}
	defer resp.Body.Close()

	respBody, _ := io.ReadAll(resp.Body)
	if resp.StatusCode >= 400 {
		return nil, fmt.Errorf("cloudinary error %d: %s", resp.StatusCode, string(respBody))
	}

	var result UploadResult
	if err := json.Unmarshal(respBody, &result); err != nil {
		return nil, fmt.Errorf("unmarshal cloudinary response: %w", err)
	}
	return &result, nil
}

// DeleteAvatar removes an avatar from Cloudinary when a user deletes their account.
func (s *Service) DeleteAvatar(ctx context.Context, publicID string) error {
	if s.devMode || publicID == "" || strings.HasPrefix(publicID, "dev_") {
		return nil
	}

	timestamp := strconv.FormatInt(time.Now().Unix(), 10)
	params := map[string]string{
		"public_id":           publicID,
		"signature_algorithm": "sha256",
		"timestamp":           timestamp,
	}
	signature := s.sign(params)

	payload := fmt.Sprintf("public_id=%s&signature_algorithm=sha256&timestamp=%s&api_key=%s&signature=%s",
		publicID, timestamp, s.apiKey, signature)

	destroyURL := fmt.Sprintf("https://api.cloudinary.com/v1_1/%s/image/destroy", s.cloudName)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, destroyURL,
		strings.NewReader(payload))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")

	client := &http.Client{Timeout: 10 * time.Second}
	resp, err := client.Do(req)
	if err == nil {
		_ = resp.Body.Close()
	}
	return err
}

// sign generates the Cloudinary request signature.
// See: https://cloudinary.com/documentation/upload_images#generating_authentication_signatures
func (s *Service) sign(params map[string]string) string {
	// Sort keys.
	keys := make([]string, 0, len(params))
	for k := range params {
		keys = append(keys, k)
	}
	sort.Strings(keys)

	var parts []string
	for _, k := range keys {
		parts = append(parts, fmt.Sprintf("%s=%s", k, params[k]))
	}
	toSign := strings.Join(parts, "&") + s.apiSecret

	h := sha256.New()
	h.Write([]byte(toSign))
	return fmt.Sprintf("%x", h.Sum(nil))
}

func contentTypeToExt(ct string) string {
	switch {
	case strings.Contains(ct, "jpeg") || strings.Contains(ct, "jpg"):
		return ".jpg"
	case strings.Contains(ct, "png"):
		return ".png"
	case strings.Contains(ct, "webp"):
		return ".webp"
	default:
		return ".jpg"
	}
}
