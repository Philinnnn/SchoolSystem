-- Add log_uuid column to audit_logs with default value
ALTER TABLE audit_logs ADD log_uuid NVARCHAR(36) NOT NULL DEFAULT CAST(NEWID() AS NVARCHAR(36));

-- Add unique constraint
ALTER TABLE audit_logs ADD CONSTRAINT UQ_audit_logs_log_uuid UNIQUE (log_uuid);

