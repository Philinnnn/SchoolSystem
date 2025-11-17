-- Create grades table
CREATE TABLE grades (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    grade INT NOT NULL,
    grade_date DATE,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
);

-- Insert test grades for student1
INSERT INTO grades (student_id, subject_id, teacher_id, grade, grade_date)
SELECT
    u1.id,
    s.id,
    u2.id,
    5,
    '2025-01-15'
FROM users u1
CROSS JOIN subjects s
CROSS JOIN users u2
WHERE u1.username = 'student1'
  AND s.name = N'Математика'
  AND u2.username = 'teacher1';

INSERT INTO grades (student_id, subject_id, teacher_id, grade, grade_date)
SELECT u1.id, s.id, u2.id, 4, '2025-01-10'
FROM users u1, subjects s, users u2
WHERE u1.username = 'student1' AND s.name = N'Математика' AND u2.username = 'teacher1';

INSERT INTO grades (student_id, subject_id, teacher_id, grade, grade_date)
SELECT u1.id, s.id, u2.id, 5, '2025-01-12'
FROM users u1, subjects s, users u2
WHERE u1.username = 'student1' AND s.name = N'Информатика' AND u2.username = 'teacher1';

INSERT INTO grades (student_id, subject_id, teacher_id, grade, grade_date)
SELECT u1.id, s.id, u2.id, 4, '2025-01-14'
FROM users u1, subjects s, users u2
WHERE u1.username = 'student1' AND s.name = N'Физика' AND u2.username = 'teacher2';

INSERT INTO grades (student_id, subject_id, teacher_id, grade, grade_date)
SELECT u1.id, s.id, u2.id, 5, '2025-01-16'
FROM users u1, subjects s, users u2
WHERE u1.username = 'student1' AND s.name = N'Физика' AND u2.username = 'teacher2';

INSERT INTO grades (student_id, subject_id, teacher_id, grade, grade_date)
SELECT u1.id, s.id, u2.id, 4, '2025-01-11'
FROM users u1, subjects s, users u2
WHERE u1.username = 'student1' AND s.name = N'Русский язык' AND u2.username = 'teacher1';

-- Insert test grades for student2
INSERT INTO grades (student_id, subject_id, teacher_id, grade, grade_date)
SELECT u1.id, s.id, u2.id, 4, '2025-01-15'
FROM users u1, subjects s, users u2
WHERE u1.username = 'student2' AND s.name = N'Математика' AND u2.username = 'teacher1';

INSERT INTO grades (student_id, subject_id, teacher_id, grade, grade_date)
SELECT u1.id, s.id, u2.id, 5, '2025-01-10'
FROM users u1, subjects s, users u2
WHERE u1.username = 'student2' AND s.name = N'Математика' AND u2.username = 'teacher1';

INSERT INTO grades (student_id, subject_id, teacher_id, grade, grade_date)
SELECT u1.id, s.id, u2.id, 4, '2025-01-12'
FROM users u1, subjects s, users u2
WHERE u1.username = 'student2' AND s.name = N'Информатика' AND u2.username = 'teacher1';

INSERT INTO grades (student_id, subject_id, teacher_id, grade, grade_date)
SELECT u1.id, s.id, u2.id, 3, '2025-01-14'
FROM users u1, subjects s, users u2
WHERE u1.username = 'student2' AND s.name = N'Физика' AND u2.username = 'teacher2';

INSERT INTO grades (student_id, subject_id, teacher_id, grade, grade_date)
SELECT u1.id, s.id, u2.id, 4, '2025-01-16'
FROM users u1, subjects s, users u2
WHERE u1.username = 'student2' AND s.name = N'Физика' AND u2.username = 'teacher2';

INSERT INTO grades (student_id, subject_id, teacher_id, grade, grade_date)
SELECT u1.id, s.id, u2.id, 5, '2025-01-11'
FROM users u1, subjects s, users u2
WHERE u1.username = 'student2' AND s.name = N'Русский язык' AND u2.username = 'teacher1';
