-- Rename column [key] to setting_key to avoid reserved keyword conflict
EXEC sp_rename 'app_settings.[key]', 'setting_key', 'COLUMN';

