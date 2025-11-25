-- Audit logs table (idempotent)
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'audit_logs')
BEGIN
    CREATE TABLE audit_logs (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        event_time DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        username NVARCHAR(255) NULL,
        action NVARCHAR(64) NOT NULL,
        details NVARCHAR(MAX) NULL,
        ip NVARCHAR(64) NULL,
        session_id NVARCHAR(128) NULL
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_audit_logs_time' AND object_id = OBJECT_ID('audit_logs'))
    CREATE INDEX IX_audit_logs_time ON audit_logs(event_time);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_audit_logs_user' AND object_id = OBJECT_ID('audit_logs'))
    CREATE INDEX IX_audit_logs_user ON audit_logs(username);
