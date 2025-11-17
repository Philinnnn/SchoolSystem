-- Create users table
CREATE TABLE users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(255) NOT NULL UNIQUE,
    password NVARCHAR(255) NOT NULL,
    full_name NVARCHAR(255)
);

-- Create roles table
CREATE TABLE roles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL UNIQUE
);

-- Create user_roles junction table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Create students table
CREATE TABLE students (
    id BIGINT PRIMARY KEY,
    class_name NVARCHAR(255),
    FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create teachers table
CREATE TABLE teachers (
    id BIGINT PRIMARY KEY,
    FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create directors table
CREATE TABLE directors (
    id BIGINT PRIMARY KEY,
    FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create subjects table
CREATE TABLE subjects (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL UNIQUE,
    description NVARCHAR(MAX)
);

-- Create teacher_subjects junction table
CREATE TABLE teacher_subjects (
    teacher_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    PRIMARY KEY (teacher_id, subject_id),
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

-- Insert default roles
INSERT INTO roles (name) VALUES ('ROLE_ADMIN');
INSERT INTO roles (name) VALUES ('ROLE_DIRECTOR');
INSERT INTO roles (name) VALUES ('ROLE_TEACHER');
INSERT INTO roles (name) VALUES ('ROLE_STUDENT');

