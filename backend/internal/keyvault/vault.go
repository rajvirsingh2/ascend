package keyvault

import (
	"context"
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"errors"
	"fmt"
	"io"

	"github.com/jackc/pgx/v5/pgxpool"
)

type Vault struct {
	db  *pgxpool.Pool
	mek []byte // master encryption key — 32 bytes from env
}

func New(db *pgxpool.Pool, masterKey []byte) (*Vault, error) {
	if len(masterKey) != 32 {
		return nil, errors.New("master key must be exactly 32 bytes")
	}
	return &Vault{db: db, mek: masterKey}, nil
}

// Store encrypts and saves a user's API key using envelope encryption.
func (v *Vault) Store(ctx context.Context, userID, plaintext string) error {
	// generate a unique DEK for this user
	dek := make([]byte, 32)
	if _, err := io.ReadFull(rand.Reader, dek); err != nil {
		return fmt.Errorf("generating DEK: %w", err)
	}

	// encrypt the plaintext API key with DEK
	ciphertext, err := aesgcmEncrypt(dek, []byte(plaintext))
	if err != nil {
		return fmt.Errorf("encrypting key: %w", err)
	}

	// wrap (encrypt) the DEK with the MEK
	wrappedDEK, err := aesgcmEncrypt(v.mek, dek)
	if err != nil {
		return fmt.Errorf("wrapping DEK: %w", err)
	}

	// zero the plaintext DEK from memory
	ZeroBytes(dek)

	_, err = v.db.Exec(ctx,
		`INSERT INTO user_api_keys (user_id, wrapped_dek, ciphertext, created_at)
         VALUES ($1, $2, $3, NOW())
         ON CONFLICT (user_id) DO UPDATE
         SET wrapped_dek=$2, ciphertext=$3, created_at=NOW()`,
		userID, wrappedDEK, ciphertext,
	)
	return err
}

// Decrypt returns the plaintext key for one request. Caller must call
// ZeroBytes() on the returned slice when done.
func (v *Vault) Decrypt(ctx context.Context, userID string) ([]byte, error) {
	var wrappedDEK, ciphertext []byte
	err := v.db.QueryRow(ctx,
		`SELECT wrapped_dek, ciphertext FROM user_api_keys WHERE user_id=$1`,
		userID,
	).Scan(&wrappedDEK, &ciphertext)
	if err != nil {
		return nil, fmt.Errorf("key not found for user: %w", err)
	}

	// unwrap DEK using MEK
	dek, err := aesgcmDecrypt(v.mek, wrappedDEK)
	if err != nil {
		return nil, fmt.Errorf("unwrapping DEK: %w", err)
	}
	defer ZeroBytes(dek)

	// decrypt API key using DEK
	plaintext, err := aesgcmDecrypt(dek, ciphertext)
	if err != nil {
		return nil, fmt.Errorf("decrypting key: %w", err)
	}

	return plaintext, nil
}

func aesgcmEncrypt(key, plaintext []byte) ([]byte, error) {
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
	// nonce is prepended to ciphertext
	return gcm.Seal(nonce, nonce, plaintext, nil), nil
}

func aesgcmDecrypt(key, data []byte) ([]byte, error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}
	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}
	nonceSize := gcm.NonceSize()
	if len(data) < nonceSize {
		return nil, errors.New("ciphertext too short")
	}
	nonce, ciphertext := data[:nonceSize], data[nonceSize:]
	return gcm.Open(nil, nonce, ciphertext, nil)
}

// ZeroBytes overwrites a byte slice with zeros.
// Call this on any plaintext key material before it goes out of scope.
func ZeroBytes(b []byte) {
	for i := range b {
		b[i] = 0
	}
}
