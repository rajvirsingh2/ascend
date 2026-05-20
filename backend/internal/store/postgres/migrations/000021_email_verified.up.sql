ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;
 
-- Mark existing users as verified so existing accounts aren't broken.
UPDATE users SET email_verified = TRUE WHERE created_at < NOW();