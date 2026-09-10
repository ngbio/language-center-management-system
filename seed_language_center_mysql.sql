/*
  Seed data for LanguageCenterDB

  Run database_language_center_mysql.sql first, then run this file once.
  All passwords below are BCrypt hashes compatible with Spring Security.

  Demo accounts:
  - Admin:      admin@languagecenter.local      / Admin@123
  - Consultant: consultant@languagecenter.local / Consultant@123
  - Teachers:   teacher1@languagecenter.local    / Teacher@123
                teacher2@languagecenter.local    / Teacher@123
  - Students:   student1@languagecenter.local    / Student@123
                student2@languagecenter.local    / Student@123
                student3@languagecenter.local    / Student@123
*/

USE LanguageCenterDB;

START TRANSACTION;

-- Existing master data created by database_language_center_mysql.sql.
SELECT id INTO @role_admin FROM role WHERE role_code = 'ADMIN';
SELECT id INTO @role_consultant FROM role WHERE role_code = 'CONSULTANT';
SELECT id INTO @role_teacher FROM role WHERE role_code = 'TEACHER';
SELECT id INTO @role_student FROM role WHERE role_code = 'STUDENT';

-- Users
INSERT INTO `user` (
    role_id, username, password_hash, full_name, email,
    phone_number, address, status, created_at, updated_at
) VALUES
(@role_admin, 'admin',
 '$2a$10$IeBzUZhHCSRIaf2P0vphn./OwiH1udqBunnClSPDkyIV5qQQAZcue',
 'Quản trị hệ thống', 'admin@languagecenter.local', '0901000001',
 'Quận 1, Thành phố Hồ Chí Minh', 'ACTIVE', NOW(), NOW()),
(@role_consultant, 'consultant01',
 '$2a$10$dh0kyNlDcR.LRiMJhiqR3.Xe73g4TZewzj2Epnx1S2sdO8vFTB8na',
 'Nguyễn Minh Tư', 'consultant@languagecenter.local', '0901000002',
 'Quận 3, Thành phố Hồ Chí Minh', 'ACTIVE', NOW(), NOW()),
(@role_teacher, 'teacher01',
 '$2a$10$3To.3pnbjyyn35PmqTRxYOHdUfgNUVcJvOnXDWy9ePTmLRvz35eAK',
 'Trần Thu Hà', 'teacher1@languagecenter.local', '0902000001',
 'Quận Bình Thạnh, Thành phố Hồ Chí Minh', 'ACTIVE', NOW(), NOW()),
(@role_teacher, 'teacher02',
 '$2a$10$3To.3pnbjyyn35PmqTRxYOHdUfgNUVcJvOnXDWy9ePTmLRvz35eAK',
 'Lê Hoàng Nam', 'teacher2@languagecenter.local', '0902000002',
 'Thành phố Thủ Đức, Thành phố Hồ Chí Minh', 'ACTIVE', NOW(), NOW()),
(@role_student, 'student01',
 '$2a$10$anp/7RELhqf0QWLImK7rCeS1o5H.CSFQGPOr9gbvWquBmIFZtEDj6',
 'Phạm Gia Huy', 'student1@languagecenter.local', '0903000001',
 'Quận 7, Thành phố Hồ Chí Minh', 'ACTIVE', NOW(), NOW()),
(@role_student, 'student02',
 '$2a$10$anp/7RELhqf0QWLImK7rCeS1o5H.CSFQGPOr9gbvWquBmIFZtEDj6',
 'Võ Ngọc Anh', 'student2@languagecenter.local', '0903000002',
 'Quận 10, Thành phố Hồ Chí Minh', 'ACTIVE', NOW(), NOW()),
(@role_student, 'student03',
 '$2a$10$anp/7RELhqf0QWLImK7rCeS1o5H.CSFQGPOr9gbvWquBmIFZtEDj6',
 'Đặng Quốc Bảo', 'student3@languagecenter.local', '0903000003',
 'Quận Tân Bình, Thành phố Hồ Chí Minh', 'INACTIVE', NOW(), NOW());

SELECT id INTO @user_teacher_1 FROM `user` WHERE email = 'teacher1@languagecenter.local';
SELECT id INTO @user_teacher_2 FROM `user` WHERE email = 'teacher2@languagecenter.local';
SELECT id INTO @user_student_1 FROM `user` WHERE email = 'student1@languagecenter.local';
SELECT id INTO @user_student_2 FROM `user` WHERE email = 'student2@languagecenter.local';
SELECT id INTO @user_student_3 FROM `user` WHERE email = 'student3@languagecenter.local';

-- Teacher and student profiles
INSERT INTO teacher (
    user_id, teacher_code, specialization, degree, experience_years
) VALUES
(@user_teacher_1, 'GV000001', 'English communication and IELTS',
 'Master of TESOL', 7),
(@user_teacher_2, 'GV000002', 'Japanese language and JLPT',
 'Bachelor of Japanese Studies', 5);

INSERT INTO student (
    user_id, student_code, date_of_birth, gender, avatar
) VALUES
(@user_student_1, 'HV000001', '2002-04-15', 'MALE', NULL),
(@user_student_2, 'HV000002', '2003-09-21', 'FEMALE', NULL),
(@user_student_3, 'HV000003', '2001-12-03', 'OTHER', NULL);

SELECT id INTO @teacher_1 FROM teacher WHERE teacher_code = 'GV000001';
SELECT id INTO @teacher_2 FROM teacher WHERE teacher_code = 'GV000002';
SELECT id INTO @student_1 FROM student WHERE student_code = 'HV000001';
SELECT id INTO @student_2 FROM student WHERE student_code = 'HV000002';
SELECT id INTO @student_3 FROM student WHERE student_code = 'HV000003';

-- Courses use level IDs resolved by language and level codes.
SELECT lv.id INTO @level_en_a1
FROM level lv JOIN language lg ON lg.id = lv.language_id
WHERE lg.language_code = 'EN' AND lv.level_code = 'A1';

SELECT lv.id INTO @level_en_b1
FROM level lv JOIN language lg ON lg.id = lv.language_id
WHERE lg.language_code = 'EN' AND lv.level_code = 'B1';

SELECT lv.id INTO @level_en_c1
FROM level lv JOIN language lg ON lg.id = lv.language_id
WHERE lg.language_code = 'EN' AND lv.level_code = 'C1';

SELECT lv.id INTO @level_ja_n5
FROM level lv JOIN language lg ON lg.id = lv.language_id
WHERE lg.language_code = 'JA' AND lv.level_code = 'N5';

SELECT lv.id INTO @level_ja_n4
FROM level lv JOIN language lg ON lg.id = lv.language_id
WHERE lg.language_code = 'JA' AND lv.level_code = 'N4';

SELECT lv.id INTO @level_zh_hsk1
FROM level lv JOIN language lg ON lg.id = lv.language_id
WHERE lg.language_code = 'ZH' AND lv.level_code = 'HSK1';

INSERT INTO course (
    level_id, course_code, course_name, slug, description, tuition_fee,
    total_sessions, duration_hours, status, publication_status,
    published_at, is_featured, created_at, updated_at
) VALUES
(@level_en_a1, 'EN-A1-COM', 'English Communication A1', 'english-communication-a1',
 'Khóa giao tiếp tiếng Anh cơ bản dành cho người mới bắt đầu.',
 3200000, 24, 48, 'ACTIVE', 'PUBLISHED', NOW(), TRUE, NOW(), NOW()),
(@level_en_b1, 'EN-B1-GEN', 'General English B1', 'general-english-b1',
 'Phát triển đồng đều nghe, nói, đọc và viết ở trình độ B1.',
 4500000, 30, 60, 'ACTIVE', 'PUBLISHED', NOW(), FALSE, NOW(), NOW()),
(@level_en_c1, 'EN-C1-ADV', 'Advanced English C1', 'advanced-english-c1',
 'Tiếng Anh học thuật và giao tiếp nâng cao.',
 6200000, 32, 64, 'INACTIVE', 'DRAFT', NULL, FALSE, NOW(), NOW()),
(@level_ja_n5, 'JA-N5-STD', 'Japanese Foundation N5', 'japanese-foundation-n5',
 'Nhập môn tiếng Nhật, bảng chữ cái và ngữ pháp N5.',
 3800000, 28, 56, 'ACTIVE', 'PUBLISHED', NOW(), TRUE, NOW(), NOW()),
(@level_ja_n4, 'JA-N4-STD', 'Japanese Intermediate N4', 'japanese-intermediate-n4',
 'Củng cố từ vựng, ngữ pháp và luyện thi JLPT N4.',
 4800000, 30, 60, 'ACTIVE', 'PUBLISHED', NOW(), FALSE, NOW(), NOW()),
(@level_zh_hsk1, 'ZH-HSK1-STD', 'Chinese Foundation HSK1', 'chinese-foundation-hsk1',
 'Tiếng Trung nền tảng theo chuẩn HSK1.',
 3500000, 24, 48, 'ACTIVE', 'PUBLISHED', NOW(), FALSE, NOW(), NOW());

SELECT id INTO @course_en_a1 FROM course WHERE course_code = 'EN-A1-COM';
SELECT id INTO @course_en_b1 FROM course WHERE course_code = 'EN-B1-GEN';
SELECT id INTO @course_ja_n5 FROM course WHERE course_code = 'JA-N5-STD';
SELECT id INTO @course_zh_hsk1 FROM course WHERE course_code = 'ZH-HSK1-STD';

-- Public curriculum for the Japanese N5 course.
INSERT INTO course_section (course_id, title, description, display_order) VALUES
(@course_ja_n5, 'Nhập môn tiếng Nhật', 'Làm quen với chữ viết và cách phát âm.', 1),
(@course_ja_n5, 'Ngữ pháp và giao tiếp N5', 'Các mẫu câu và tình huống giao tiếp cơ bản.', 2);

SELECT id INTO @section_ja_n5_intro
FROM course_section WHERE course_id = @course_ja_n5 AND display_order = 1;
SELECT id INTO @section_ja_n5_grammar
FROM course_section WHERE course_id = @course_ja_n5 AND display_order = 2;

INSERT INTO course_content (
    section_id, title, summary, content_html, content_type,
    display_order, is_preview, publication_status
) VALUES
(@section_ja_n5_intro, 'Bài 1: Hiragana', 'Nhận biết và luyện viết bảng chữ Hiragana.',
 '<p>Giới thiệu bảng chữ Hiragana và quy tắc phát âm cơ bản.</p>',
 'LESSON', 1, TRUE, 'PUBLISHED'),
(@section_ja_n5_intro, 'Bài 2: Katakana', 'Nhận biết và luyện viết bảng chữ Katakana.',
 '<p>Giới thiệu bảng chữ Katakana và các từ mượn thông dụng.</p>',
 'LESSON', 2, FALSE, 'PUBLISHED'),
(@section_ja_n5_grammar, 'Bài 3: Câu giới thiệu bản thân',
 'Sử dụng mẫu câu danh từ và trợ từ は.',
 '<p>Luyện tập giới thiệu tên, nghề nghiệp và quốc tịch.</p>',
 'GRAMMAR', 1, FALSE, 'PUBLISHED');

-- Rooms
INSERT INTO room (room_code, room_name, capacity, location, status) VALUES
('P101', 'Phòng 101', 24, 'Tầng 1 - Cơ sở chính', 'ACTIVE'),
('P102', 'Phòng 102', 20, 'Tầng 1 - Cơ sở chính', 'ACTIVE'),
('P201', 'Phòng 201', 30, 'Tầng 2 - Cơ sở chính', 'ACTIVE'),
('LAB01', 'Phòng Lab 01', 18, 'Tầng 3 - Cơ sở chính', 'MAINTENANCE');

SELECT id INTO @room_101 FROM room WHERE room_code = 'P101';
SELECT id INTO @room_102 FROM room WHERE room_code = 'P102';
SELECT id INTO @room_201 FROM room WHERE room_code = 'P201';

-- Course classes with dates relative to the day the seed is executed.
INSERT INTO courseclass (
    course_id, teacher_id, class_code, class_name, start_date, end_date,
    max_students, applied_tuition_fee, status, created_at, updated_at
) VALUES
(@course_en_a1, @teacher_1, 'EN-A1-OPEN-01', 'English A1 Evening 01',
 DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 91 DAY),
 20, 3200000, 'OPEN', NOW(), NOW()),
(@course_ja_n5, @teacher_2, 'JA-N5-DRAFT-01', 'Japanese N5 Weekend 01',
 DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 112 DAY),
 18, 3800000, 'DRAFT', NOW(), NOW()),
(@course_en_b1, @teacher_1, 'EN-B1-RUN-01', 'General English B1 Morning 01',
 DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 45 DAY),
 22, 4500000, 'IN_PROGRESS', NOW(), NOW()),
(@course_zh_hsk1, @teacher_2, 'ZH-HSK1-OPEN-01', 'Chinese HSK1 Online 01',
 DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 94 DAY),
 25, 3500000, 'OPEN', NOW(), NOW());

SELECT id INTO @class_en_a1 FROM courseclass WHERE class_code = 'EN-A1-OPEN-01';
SELECT id INTO @class_ja_n5 FROM courseclass WHERE class_code = 'JA-N5-DRAFT-01';
SELECT id INTO @class_en_b1 FROM courseclass WHERE class_code = 'EN-B1-RUN-01';
SELECT id INTO @class_zh_hsk1 FROM courseclass WHERE class_code = 'ZH-HSK1-OPEN-01';

-- day_of_week: 1 = Monday, ..., 7 = Sunday.
INSERT INTO classschedule (
    course_class_id, room_id, day_of_week, start_time, end_time,
    delivery_mode, meeting_url
) VALUES
(@class_en_a1, @room_101, 2, '18:00:00', '20:00:00', 'IN_PERSON', NULL),
(@class_en_a1, @room_101, 4, '18:00:00', '20:00:00', 'IN_PERSON', NULL),
(@class_en_a1, @room_101, 6, '18:00:00', '20:00:00', 'IN_PERSON', NULL),
(@class_ja_n5, @room_102, 3, '18:00:00', '20:00:00', 'IN_PERSON', NULL),
(@class_ja_n5, @room_102, 5, '18:00:00', '20:00:00', 'IN_PERSON', NULL),
(@class_ja_n5, @room_102, 7, '08:00:00', '12:00:00', 'IN_PERSON', NULL),
(@class_en_b1, @room_201, 1, '08:00:00', '10:00:00', 'IN_PERSON', NULL),
(@class_en_b1, @room_201, 3, '08:00:00', '10:00:00', 'IN_PERSON', NULL),
(@class_en_b1, @room_201, 5, '08:00:00', '10:00:00', 'IN_PERSON', NULL),
(@class_zh_hsk1, NULL, 2, '19:00:00', '21:00:00', 'ONLINE',
 'https://meet.example.com/zh-hsk1-open-01'),
(@class_zh_hsk1, NULL, 4, '19:00:00', '21:00:00', 'ONLINE',
 'https://meet.example.com/zh-hsk1-open-01'),
(@class_zh_hsk1, NULL, 6, '19:00:00', '21:00:00', 'ONLINE',
 'https://meet.example.com/zh-hsk1-open-01');

SELECT id INTO @schedule_en_b1_mon
FROM classschedule
WHERE course_class_id = @class_en_b1 AND day_of_week = 1;

SELECT id INTO @schedule_en_b1_wed
FROM classschedule
WHERE course_class_id = @class_en_b1 AND day_of_week = 3;

-- Lessons for the class currently in progress.
INSERT INTO lesson (
    class_schedule_id, topic, lesson_date, status
) VALUES
(@schedule_en_b1_mon, 'Review of present and past tenses',
 DATE_SUB(CURRENT_DATE, INTERVAL 14 DAY), 'COMPLETED'),
(@schedule_en_b1_wed, 'Everyday communication practice',
 DATE_SUB(CURRENT_DATE, INTERVAL 12 DAY), 'COMPLETED'),
(@schedule_en_b1_mon, 'Future plans and arrangements',
 DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), 'SCHEDULED');

SELECT id INTO @lesson_en_b1_1
FROM lesson
WHERE class_schedule_id = @schedule_en_b1_mon
  AND lesson_date = DATE_SUB(CURRENT_DATE, INTERVAL 14 DAY);

SELECT id INTO @lesson_en_b1_2
FROM lesson
WHERE class_schedule_id = @schedule_en_b1_wed
  AND lesson_date = DATE_SUB(CURRENT_DATE, INTERVAL 12 DAY);

-- Enrollments
INSERT INTO enrollment (
    student_id, course_class_id, enrollment_date, payment_deadline, amount_due,
    enrollment_status, payment_status, confirmed_at,
    cancelled_at, cancellation_reason
) VALUES
(@student_1, @class_en_a1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 3200000,
 'CONFIRMED', 'PAID', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL),
(@student_2, @class_en_a1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), 3200000,
 'CONFIRMED', 'PENDING', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL),
(@student_1, @class_en_b1, DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 38 DAY), 4500000,
 'CONFIRMED', 'PAID', DATE_SUB(NOW(), INTERVAL 39 DAY), NULL, NULL),
(@student_3, @class_en_b1, DATE_SUB(NOW(), INTERVAL 38 DAY), DATE_SUB(NOW(), INTERVAL 36 DAY), 4500000,
 'CANCELLED', 'CANCELLED', NULL, DATE_SUB(NOW(), INTERVAL 35 DAY),
 'Học viên thay đổi lịch cá nhân');

SELECT id INTO @enrollment_student_1_a1
FROM enrollment
WHERE student_id = @student_1 AND course_class_id = @class_en_a1;

SELECT id INTO @enrollment_student_2_a1
FROM enrollment
WHERE student_id = @student_2 AND course_class_id = @class_en_a1;

SELECT id INTO @enrollment_student_1_b1
FROM enrollment
WHERE student_id = @student_1 AND course_class_id = @class_en_b1;

-- Payments. successful_enrollment_id is generated automatically by MySQL.
INSERT INTO payment (
    enrollment_id, transaction_code, method, amount, status,
    created_at, completed_at, reference_code, error_message
) VALUES
(@enrollment_student_1_a1, 'PAY-SEED-A1-0001', 'ZALOPAY', 3200000,
 'PAID', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY),
 'ZALOPAY-A1-0001', NULL),
(@enrollment_student_2_a1, 'PAY-SEED-A1-0002', 'MOMO', 3200000,
 'PENDING', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, 'MOMO-A1-0002', NULL),
(@enrollment_student_1_b1, 'PAY-SEED-B1-0001', 'MOMO', 4500000,
 'PAID', DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 39 DAY),
 'MOMO-B1-0001', NULL);

-- Attendance belongs to lessons and enrollments of the same class.
INSERT INTO attendance (
    lesson_id, enrollment_id, status, note, attendance_time
) VALUES
(@lesson_en_b1_1, @enrollment_student_1_b1, 'PRESENT', NULL,
 DATE_SUB(NOW(), INTERVAL 14 DAY)),
(@lesson_en_b1_2, @enrollment_student_1_b1, 'LATE',
 'Học viên đến trễ 10 phút', DATE_SUB(NOW(), INTERVAL 12 DAY));

-- Notifications
INSERT INTO notification (
    user_id, title, content, notification_type, is_read, created_at, read_at
) VALUES
(@user_student_1, 'Đăng ký lớp học thành công',
 'Bạn đã đăng ký thành công lớp English A1 Evening 01.',
 'ENROLLMENT', TRUE, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(@user_student_1, 'Thanh toán thành công',
 'Khoản thanh toán 3.200.000 VND đã được xác nhận.',
 'PAYMENT', TRUE, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(@user_student_2, 'Chờ hoàn tất thanh toán',
 'Vui lòng hoàn tất thanh toán để xác nhận đăng ký lớp học.',
 'PAYMENT', FALSE, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
(@user_teacher_1, 'Lịch giảng dạy được cập nhật',
 'Lớp English A1 Evening 01 đã được thêm vào lịch giảng dạy.',
 'SCHEDULE', FALSE, NOW(), NULL);

COMMIT;

-- Quick verification
SELECT 'role' AS table_name, COUNT(*) AS row_count FROM role
UNION ALL SELECT 'user', COUNT(*) FROM `user`
UNION ALL SELECT 'teacher', COUNT(*) FROM teacher
UNION ALL SELECT 'student', COUNT(*) FROM student
UNION ALL SELECT 'language', COUNT(*) FROM language
UNION ALL SELECT 'level', COUNT(*) FROM level
UNION ALL SELECT 'course', COUNT(*) FROM course
UNION ALL SELECT 'room', COUNT(*) FROM room
UNION ALL SELECT 'courseclass', COUNT(*) FROM courseclass
UNION ALL SELECT 'classschedule', COUNT(*) FROM classschedule
UNION ALL SELECT 'lesson', COUNT(*) FROM lesson
UNION ALL SELECT 'enrollment', COUNT(*) FROM enrollment
UNION ALL SELECT 'payment', COUNT(*) FROM payment
UNION ALL SELECT 'attendance', COUNT(*) FROM attendance
UNION ALL SELECT 'notification', COUNT(*) FROM notification;
