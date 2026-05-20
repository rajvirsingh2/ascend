package user

import (
	"bytes"
	"context"
	"crypto/hmac"
	"crypto/sha1"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"sort"
	"strconv"
	"strings"
	"time"

	"ascend-backend/internal/middleware"
	"ascend-backend/pkg/response"

	"github.com/jackc/pgx/v5/pgxpool"
)

type AvatarUploader struct {
	db        *pgxpool.Pool
	cloudName string
	apiKey    string
	apiSecret string
}

func NewAvatarUploader(db *pgxpool.Pool, cloudName, apiKey, apiSecret string) *AvatarUploader {
	return &AvatarUploader{db, cloudName, apiKey, apiSecret}
}

func (a *AvatarUploader) Upload(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)

	// limit: 5 MB
	r.Body = http.MaxBytesReader(w, r.Body, 5<<20)

	var req struct {
		ImageBase64 string `json:"image_base64"` // data:image/jpeg;base64,...
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		response.Error(w, http.StatusBadRequest, "invalid request body")
		return
	}
	if req.ImageBase64 == "" {
		response.Error(w, http.StatusBadRequest, "image_base64 is required")
		return
	}
	if len(req.ImageBase64) > 7<<20 { // base64 is ~33% larger
		response.Error(w, http.StatusRequestEntityTooLarge, "image too large (max 5MB)")
		return
	}

	// upload to Cloudinary
	avatarURL, err := a.uploadToCloudinary(r.Context(), userID, req.ImageBase64)
	if err != nil {
		response.Error(w, http.StatusInternalServerError, "upload failed: "+err.Error())
		return
	}

	// save URL to DB
	_, err = a.db.Exec(r.Context(),
		`UPDATE users SET avatar_url=$1 WHERE id=$2`,
		avatarURL, userID,
	)
	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to save avatar")
		return
	}

	response.JSON(w, http.StatusOK, map[string]string{"avatar_url": avatarURL})
}

func (a *AvatarUploader) uploadToCloudinary(ctx context.Context, userID, base64Data string) (string, error) {
	timestamp := strconv.FormatInt(time.Now().Unix(), 10)
	publicID := "avatars/" + userID

	// build signature
	params := map[string]string{
		"public_id":      publicID,
		"timestamp":      timestamp,
		"overwrite":      "true",
		"transformation": "w_256,h_256,c_fill,g_face,r_max,f_webp",
	}
	sig := a.sign(params)

	// multipart form upload
	var buf bytes.Buffer
	mw := multipart.NewWriter(&buf)
	mw.WriteField("file", base64Data)
	mw.WriteField("public_id", publicID)
	mw.WriteField("timestamp", timestamp)
	mw.WriteField("api_key", a.apiKey)
	mw.WriteField("signature", sig)
	mw.WriteField("overwrite", "true")
	mw.WriteField("transformation", "w_256,h_256,c_fill,g_face,r_max,f_webp")
	mw.Close()

	uploadURL := fmt.Sprintf("https://api.cloudinary.com/v1_1/%s/image/upload", a.cloudName)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, uploadURL, &buf)
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", mw.FormDataContentType())

	client := &http.Client{Timeout: 30 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	body, _ := io.ReadAll(resp.Body)
	var result map[string]any
	if err := json.Unmarshal(body, &result); err != nil {
		return "", fmt.Errorf("cloudinary response parse error")
	}
	if errMsg, ok := result["error"].(map[string]any); ok {
		return "", fmt.Errorf("cloudinary: %v", errMsg["message"])
	}

	secureURL, _ := result["secure_url"].(string)
	return secureURL, nil
}

func (a *AvatarUploader) sign(params map[string]string) string {
	keys := make([]string, 0, len(params))
	for k := range params {
		keys = append(keys, k)
	}
	sort.Strings(keys)

	var parts []string
	for _, k := range keys {
		parts = append(parts, k+"="+params[k])
	}
	raw := strings.Join(parts, "&") + a.apiSecret

	h := hmac.New(sha1.New, []byte(a.apiSecret))
	h.Write([]byte(raw))
	return hex.EncodeToString(h.Sum(nil))
}
