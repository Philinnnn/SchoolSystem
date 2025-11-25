-- App settings table for runtime-configurable settings
CREATE TABLE app_settings (
    [key] NVARCHAR(128) NOT NULL PRIMARY KEY,
    [value] NVARCHAR(1024) NULL
);

-- Example default could be inserted later via UI

