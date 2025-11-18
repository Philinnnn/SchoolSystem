-- Add auth-related columns to users
IF COL_LENGTH('users', 'telegram_id') IS NULL
    ALTER TABLE users ADD telegram_id BIGINT NULL;
IF COL_LENGTH('users', 'telegram_link_code') IS NULL
    ALTER TABLE users ADD telegram_link_code NVARCHAR(64) NULL;
IF COL_LENGTH('users', 'totp_secret') IS NULL
    ALTER TABLE users ADD totp_secret NVARCHAR(128) NULL;
IF COL_LENGTH('users', 'totp_enabled') IS NULL
    ALTER TABLE users ADD totp_enabled BIT NOT NULL CONSTRAINT DF_users_totp_enabled DEFAULT(0);
IF COL_LENGTH('users', 'password_reset_token') IS NULL
    ALTER TABLE users ADD password_reset_token NVARCHAR(128) NULL;
IF COL_LENGTH('users', 'password_reset_expiry') IS NULL
    ALTER TABLE users ADD password_reset_expiry DATETIME2 NULL;

-- Ensure non-null for existing rows
UPDATE users SET totp_enabled = 0 WHERE totp_enabled IS NULL;

