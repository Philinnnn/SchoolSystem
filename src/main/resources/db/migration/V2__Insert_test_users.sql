-- Insert test users with bcrypt encoded password "password" for all users
-- Password: password (encoded with BCrypt)
-- You can use online BCrypt generator or Spring's BCryptPasswordEncoder

-- Admin user
INSERT INTO users (username, password, full_name)
VALUES ('admin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Администратор Системы');

-- Director user
INSERT INTO users (username, password, full_name)
VALUES ('director', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Директор Школы');

-- Teacher users
INSERT INTO users (username, password, full_name)
VALUES ('teacher1', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Учитель Математики');

INSERT INTO users (username, password, full_name)
VALUES ('teacher2', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Учитель Физики');

-- Student users
INSERT INTO users (username, password, full_name)
VALUES ('student1', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Иван Петров');

INSERT INTO users (username, password, full_name)
VALUES ('student2', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Мария Сидорова');

-- Assign roles to users
-- Admin role (assuming role IDs: 1=ADMIN, 2=DIRECTOR, 3=TEACHER, 4=STUDENT)
INSERT INTO user_roles (user_id, role_id)
SELECT id, 1 FROM users WHERE username = 'admin';

-- Director role
INSERT INTO user_roles (user_id, role_id)
SELECT id, 2 FROM users WHERE username = 'director';

-- Create director profile
INSERT INTO directors (id)
SELECT id FROM users WHERE username = 'director';

-- Teacher roles
INSERT INTO user_roles (user_id, role_id)
SELECT id, 3 FROM users WHERE username = 'teacher1';

INSERT INTO user_roles (user_id, role_id)
SELECT id, 3 FROM users WHERE username = 'teacher2';

-- Create teacher profiles
INSERT INTO teachers (id)
SELECT id FROM users WHERE username = 'teacher1';

INSERT INTO teachers (id)
SELECT id FROM users WHERE username = 'teacher2';

-- Student roles
INSERT INTO user_roles (user_id, role_id)
SELECT id, 4 FROM users WHERE username = 'student1';

INSERT INTO user_roles (user_id, role_id)
SELECT id, 4 FROM users WHERE username = 'student2';

-- Create student profiles
INSERT INTO students (id, class_name)
SELECT id, '10А' FROM users WHERE username = 'student1';

INSERT INTO students (id, class_name)
SELECT id, '10Б' FROM users WHERE username = 'student2';

-- Insert some test subjects
INSERT INTO subjects (name, description) VALUES ('Математика', 'Алгебра и геометрия');
INSERT INTO subjects (name, description) VALUES ('Физика', 'Механика и термодинамика');
INSERT INTO subjects (name, description) VALUES ('Информатика', 'Программирование и алгоритмы');
INSERT INTO subjects (name, description) VALUES ('Русский язык', 'Грамматика и литература');

-- Assign subjects to teachers
INSERT INTO teacher_subjects (teacher_id, subject_id)
SELECT t.id, s.id
FROM teachers t, subjects s
WHERE t.id = (SELECT id FROM users WHERE username = 'teacher1')
  AND s.name = 'Математика';

INSERT INTO teacher_subjects (teacher_id, subject_id)
SELECT t.id, s.id
FROM teachers t, subjects s
WHERE t.id = (SELECT id FROM users WHERE username = 'teacher1')
  AND s.name = 'Информатика';

INSERT INTO teacher_subjects (teacher_id, subject_id)
SELECT t.id, s.id
FROM teachers t, subjects s
WHERE t.id = (SELECT id FROM users WHERE username = 'teacher2')
  AND s.name = 'Физика';

