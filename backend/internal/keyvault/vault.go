package keyvault

import (
	"context"
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"encoding/hex"
	"errors"
	"fmt"
	"io"

	"github.com/jackc/pgx/v5/pgxpool"
)

type Vault struct {
	db  *pgxpool.Pool
	mek []byte
}

func New(db *pgxpool.Pool, masterKeyHex string) (*Vault, error) {
	mek, err := hex.DecodeString(masterKeyHex)
	if err != nil || len(mek) != 32 {
		return nil, errors.New("MASTER_ENCRYPTION_KEY must be 64 hex chars (32 bytes)")
	}
	return &Vault{db: db, mek: mek}, nil
}

type KeyRecord struct {
	Provider      string
	ModelOverride string
}

// Store encrypts and persists a user's API key.
func (v *Vault) Store(ctx context.Context, userID, provider, model, plaintext string) error {
	dek := make([]byte, 32)
	if _, err := io.ReadFull(rand.Reader, dek); err != nil {
		return fmt.Errorf("generating DEK: %w", err)
	}
	defer ZeroBytes(dek)

	ciphertext, err := seal(dek, []byte(plaintext))
	if err != nil {
		return fmt.Errorf("encrypting key: %w", err)
	}

	wrappedDEK, err := seal(v.mek, dek)
	if err != nil {
		return fmt.Errorf("wrapping DEK: %w", err)
	}

	_, err = v.db.Exec(ctx,
		`INSERT INTO user_api_keys
		   (user_id, provider, model_override, wrapped_dek, ciphertext, updated_at)
		 VALUES ($1, $2, $3, $4, $5, NOW())
		 ON CONFLICT (user_id) DO UPDATE
		 SET provider=$2, model_override=$3, wrapped_dek=$4,
		     ciphertext=$5, updated_at=NOW()`,
		userID, provider, model, wrappedDEK, ciphertext,
	)
	return err
}

// Decrypt returns the plaintext API key and provider info.
// Caller MUST call ZeroBytes(key) when done.
func (v *Vault) Decrypt(ctx context.Context, userID string) (key []byte, rec KeyRecord, err error) {
	var wrappedDEK, ciphertext []byte
	err = v.db.QueryRow(ctx,
		`SELECT provider, model_override, wrapped_dek, ciphertext
		 FROM user_api_keys WHERE user_id=$1`,
		userID,
	).Scan(&rec.Provider, &rec.ModelOverride, &wrappedDEK, &ciphertext)
	if err != nil {
		return nil, rec, fmt.Errorf("key not found: %w", err)
	}

	dek, err := open(v.mek, wrappedDEK)
	if err != nil {
		return nil, rec, fmt.Errorf("unwrap DEK: %w", err)
	}
	defer ZeroBytes(dek)

	key, err = open(dek, ciphertext)
	return key, rec, err
}

// HasKey returns true if the user has stored an API key.
func (v *Vault) HasKey(ctx context.Context, userID string) bool {
	var count int
	v.db.QueryRow(ctx,
		`SELECT COUNT(*) FROM user_api_keys WHERE user_id=$1`, userID,
	).Scan(&count)
	return count > 0
}

// Delete removes a user's stored API key.
func (v *Vault) Delete(ctx context.Context, userID string) error {
	_, err := v.db.Exec(ctx,
		`DELETE FROM user_api_keys WHERE user_id=$1`, userID)
	return err
}

func seal(key, plaintext []byte) ([]byte, error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}
	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}
	nonce := make([]byte, gcm.NonceSize())
	if _, err := io.ReadFull(rand.Reader, nonce); err != nil {
		return nil, err
	}
	return gcm.Seal(nonce, nonce, plaintext, nil), nil
}

func open(key, data []byte) ([]byte, error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}
	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}
	ns := gcm.NonceSize()
	if len(data) < ns {
		return nil, errors.New("ciphertext too short")
	}
	return gcm.Open(nil, data[:ns], data[ns:], nil)
}

func ZeroBytes(b []byte) {
	for i := range b {
		b[i] = 0
	}
}
