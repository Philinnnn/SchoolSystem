@echo off
echo Очистка базы данных School для курсача...
echo.

sqlcmd -S 192.168.0.102\MSSQL$EXPRESS -d School -U Arman -P "" -Q "DROP TABLE IF EXISTS grades; DROP TABLE IF EXISTS teacher_subjects; DROP TABLE IF EXISTS user_roles; DROP TABLE IF EXISTS students; DROP TABLE IF EXISTS teachers; DROP TABLE IF EXISTS directors; DROP TABLE IF EXISTS subjects; DROP TABLE IF EXISTS roles; DROP TABLE IF EXISTS users; DROP TABLE IF EXISTS flyway_schema_history;"

echo.
echo База данных очищена! Теперь перезапустите приложение.
pause

