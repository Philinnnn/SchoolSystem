-- V12: Enable xp_cmdshell for backup operations
-- This is an OPTIONAL migration - only apply if you trust your environment
-- xp_cmdshell is required for:
-- 1. Getting file size of backups on remote SQL Server
-- 2. Reading backup files via SQL
-- 3. Deleting backup files via SQL

-- WARNING: xp_cmdshell can be a security risk if not properly configured
-- Only enable in trusted environments or with proper security measures

-- Uncomment the lines below to enable xp_cmdshell automatically:

-- EXEC sp_configure 'show advanced options', 1;
-- RECONFIGURE;
-- GO

-- EXEC sp_configure 'xp_cmdshell', 1;
-- RECONFIGURE;
-- GO

-- If you prefer not to enable xp_cmdshell, consider alternative solutions:
-- 1. Copy backups to local directory accessible by the application
-- 2. Create a REST API on the database server for backup file operations
-- 3. Configure proper SMB/CIFS authentication for UNC path access

