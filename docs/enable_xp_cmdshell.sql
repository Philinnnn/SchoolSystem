-- Скрипт для включения xp_cmdshell на SQL Server
-- ВНИМАНИЕ: xp_cmdshell представляет потенциальную угрозу безопасности
-- Используйте только в доверенной среде или настройте proxy аккаунт

-- Шаг 1: Включить показ расширенных опций
PRINT 'Включение расширенных опций...';
EXEC sp_configure 'show advanced options', 1;
RECONFIGURE;
GO

-- Шаг 2: Показать текущую конфигурацию xp_cmdshell
PRINT 'Текущая конфигурация xp_cmdshell:';
EXEC sp_configure 'xp_cmdshell';
GO

-- Шаг 3: Включить xp_cmdshell
PRINT 'Включение xp_cmdshell...';
EXEC sp_configure 'xp_cmdshell', 1;
RECONFIGURE;
GO

-- Шаг 4: Проверить, что включено
PRINT 'Проверка конфигурации:';
EXEC sp_configure 'xp_cmdshell';
GO

-- Шаг 5: Тестовый запрос (проверка доступа к каталогу бэкапов)
PRINT 'Тестовый запрос:';
EXEC xp_cmdshell 'dir C:\Backups';
GO

-- Шаг 6: Тест PowerShell (требуется для получения размера файлов)
PRINT 'Проверка PowerShell:';
EXEC xp_cmdshell 'powershell -Command "Write-Output test"';
GO

PRINT 'Настройка завершена успешно!';
PRINT 'Теперь перезапустите приложение SchoolSystem';
GO

