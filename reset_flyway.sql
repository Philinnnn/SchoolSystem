-- Скрипт для полной очистки базы данных School
-- Используйте это для курсача, чтобы быстро пересоздать всё

-- Удаление всех таблиц в правильном порядке (учитывая foreign keys)
DROP TABLE IF EXISTS grades;
DROP TABLE IF EXISTS teacher_subjects;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS teachers;
DROP TABLE IF EXISTS directors;
DROP TABLE IF EXISTS subjects;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS flyway_schema_history;

-- Теперь перезапустите приложение, и Flyway создаст всё заново!
