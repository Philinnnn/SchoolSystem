-- Изменяем структуру для одной роли на пользователя
-- Добавляем таблицу классов

-- 1. Создаем таблицу классов (если не существует)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'classes')
BEGIN
    CREATE TABLE classes (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(50) NOT NULL UNIQUE,
        created_at DATETIME2 DEFAULT GETDATE()
    );

    -- Вставляем существующие классы из students
    INSERT INTO classes (name)
    SELECT DISTINCT class_name
    FROM students
    WHERE class_name IS NOT NULL;
END;
GO

-- 2. Добавляем новую колонку role_id в users (если её ещё нет)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('users') AND name = 'role_id')
BEGIN
    ALTER TABLE users ADD role_id BIGINT NULL;
END;
GO

-- 3. Мигрируем данные из user_roles в users (берем первую роль), если ещё не мигрировано
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'user_roles')
BEGIN
    UPDATE users
    SET role_id = (
        SELECT TOP 1 role_id
        FROM user_roles
        WHERE user_roles.user_id = users.id
    )
    WHERE role_id IS NULL AND EXISTS (
        SELECT 1 FROM user_roles WHERE user_roles.user_id = users.id
    );

    -- Устанавливаем дефолтную роль ROLE_STUDENT для пользователей без роли
    UPDATE users
    SET role_id = (SELECT id FROM roles WHERE name = 'ROLE_STUDENT')
    WHERE role_id IS NULL;
END;
GO

-- 4. Удаляем старую таблицу user_roles (если существует)
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'user_roles')
BEGIN
    DROP TABLE user_roles;
END;
GO

-- 5. Делаем role_id обязательным (если ещё не обязательный)
IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('users') AND name = 'role_id' AND is_nullable = 1)
BEGIN
    ALTER TABLE users ALTER COLUMN role_id BIGINT NOT NULL;
END;
GO

-- 6. Добавляем внешний ключ (если ещё не существует)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_users_role')
BEGIN
    ALTER TABLE users ADD CONSTRAINT FK_users_role
        FOREIGN KEY (role_id) REFERENCES roles(id);
END;
