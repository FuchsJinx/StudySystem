-- Demo seed (password for all: Demo123!)
-- BCrypt: $2b$10$0H.cPjmwNpT8mV9UlqcjmeREUtFEJd7AhsuFJDb30ybNREY3C05bK
INSERT INTO app_settings (key, value) VALUES
    ('semesterBonusAR', '200'),
    ('arMax', '1000'),
    ('arStartOnRegister', '1000'),
    ('arDeductIntervalSec', '60'),
    ('arDeductAmount', '20'),
    ('teacherMonthlyBudgetPerGroup', '1000'),
    ('deadlineReminderHours', '24');

INSERT INTO faculties (id, name) VALUES
    (1, 'ФИТ'),
    (2, 'ЭФ');
SELECT setval('faculties_id_seq', 2);

INSERT INTO departments (id, name) VALUES
    (1, 'Кафедра ПО'),
    (2, 'Кафедра ИС');
SELECT setval('departments_id_seq', 2);

INSERT INTO groups (id, faculty_id, name) VALUES
    (1, 1, 'ИВТ-21'),
    (2, 1, 'ИВТ-22'),
    (3, 2, 'ЭК-21');
SELECT setval('groups_id_seq', 3);

INSERT INTO users (id, email, password_hash, full_name, role, email_verified) VALUES
    (1, 'admin@demo.local', '$2b$10$0H.cPjmwNpT8mV9UlqcjmeREUtFEJd7AhsuFJDb30ybNREY3C05bK', 'Админ Системы', 'ADMIN', TRUE),
    (2, 'teacher@demo.local', '$2b$10$0H.cPjmwNpT8mV9UlqcjmeREUtFEJd7AhsuFJDb30ybNREY3C05bK', 'Пётр Преподавателев', 'TEACHER', TRUE),
    (3, 'teacher2@demo.local', '$2b$10$0H.cPjmwNpT8mV9UlqcjmeREUtFEJd7AhsuFJDb30ybNREY3C05bK', 'Анна Наумова', 'TEACHER', TRUE),
    (4, 'student@demo.local', '$2b$10$0H.cPjmwNpT8mV9UlqcjmeREUtFEJd7AhsuFJDb30ybNREY3C05bK', 'Иван Иванов', 'STUDENT', TRUE),
    (5, 'pending@demo.local', '$2b$10$0H.cPjmwNpT8mV9UlqcjmeREUtFEJd7AhsuFJDb30ybNREY3C05bK', 'Олег Ожидаев', 'STUDENT', TRUE),
    (6, 'lead@demo.local', '$2b$10$0H.cPjmwNpT8mV9UlqcjmeREUtFEJd7AhsuFJDb30ybNREY3C05bK', 'Мария Старостина', 'STUDENT', TRUE),
    (7, 'student2@demo.local', '$2b$10$0H.cPjmwNpT8mV9UlqcjmeREUtFEJd7AhsuFJDb30ybNREY3C05bK', 'Сергей Петров', 'STUDENT', TRUE),
    (8, 'student3@demo.local', '$2b$10$0H.cPjmwNpT8mV9UlqcjmeREUtFEJd7AhsuFJDb30ybNREY3C05bK', 'Елена Сидорова', 'STUDENT', TRUE);
SELECT setval('users_id_seq', 8);

INSERT INTO teacher_profiles (id, user_id, department_id) VALUES
    (1, 2, 1),
    (2, 3, 2);
SELECT setval('teacher_profiles_id_seq', 2);

INSERT INTO teacher_group_links (teacher_profile_id, group_id) VALUES
    (1, 1), (1, 2), (2, 3);

INSERT INTO student_profiles (id, user_id, group_id, membership_status, is_group_lead, academic_rating) VALUES
    (1, 4, 1, 'CONFIRMED', FALSE, 1000),
    (2, 5, 1, 'PENDING', FALSE, 1000),
    (3, 6, 1, 'CONFIRMED', TRUE, 1000),
    (4, 7, 2, 'CONFIRMED', FALSE, 1000),
    (5, 8, 3, 'CONFIRMED', FALSE, 1000);
SELECT setval('student_profiles_id_seq', 5);

INSERT INTO notification_settings (user_id, email_enabled, telegram_enabled) VALUES
    (4, TRUE, FALSE), (5, TRUE, FALSE), (6, TRUE, FALSE),
    (7, TRUE, FALSE), (8, TRUE, FALSE), (2, TRUE, FALSE), (3, TRUE, FALSE);

INSERT INTO semesters (name, start_at, end_at, is_active) VALUES
    ('Осень 2026', '2026-09-01', '2026-12-31', FALSE);
