-- Add auth-related columns to users
ALTER TABLE users ADD telegram_id BIGINT NULL;
ALTER TABLE users ADD telegram_link_code NVARCHAR(64) NULL;
ALTER TABLE users ADD totp_secret NVARCHAR(128) NULL;
ALTER TABLE users ADD totp_enabled BIT NOT NULL DEFAULT 0;
ALTER TABLE users ADD password_reset_token NVARCHAR(128) NULL;
ALTER TABLE users ADD password_reset_expiry DATETIME2 NULL;


