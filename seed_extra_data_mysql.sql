/*
  Dữ liệu bổ sung cho LanguageCenterDB
  ======================================
  Chạy seed_language_center_mysql.sql TRƯỚC, sau đó chạy file này.

  Nội dung file này:
    - 3 giáo viên mới  (GV000003 – GV000005)
    - 2 phòng học mới  (P202, P301)
    - 4 khóa học tiếng Việt mới
    - 10 lớp học trạng thái OPEN với lịch đa dạng

  Tài khoản giáo viên thêm mới (password: Teacher@123):
    - teacher3@languagecenter.local
    - teacher4@languagecenter.local
    - teacher5@languagecenter.local

  day_of_week: 1 = Thứ 2, 2 = Thứ 3, 3 = Thứ 4, 4 = Thứ 5,
               5 = Thứ 6, 6 = Thứ 7, 7 = Chủ nhật
*/

USE LanguageCenterDB;

START TRANSACTION;

-- ============================================================
-- 1. GIÁO VIÊN MỚI
-- ============================================================
SELECT id INTO @role_teacher FROM role WHERE role_code = 'TEACHER';

INSERT INTO `user` (
    role_id, username, password_hash, full_name, email,
    phone_number, address, status, created_at, updated_at
) VALUES
(@role_teacher, 'teacher03',
 '$2a$10$3To.3pnbjyyn35PmqTRxYOHdUfgNUVcJvOnXDWy9ePTmLRvz35eAK',
 'Hoàng Thị Lan', 'teacher3@languagecenter.local', '0902000003',
 'Quận Gò Vấp, Thành phố Hồ Chí Minh', 'ACTIVE', NOW(), NOW()),

(@role_teacher, 'teacher04',
 '$2a$10$3To.3pnbjyyn35PmqTRxYOHdUfgNUVcJvOnXDWy9ePTmLRvz35eAK',
 'Nguyễn Văn Đức', 'teacher4@languagecenter.local', '0902000004',
 'Quận 12, Thành phố Hồ Chí Minh', 'ACTIVE', NOW(), NOW()),

(@role_teacher, 'teacher05',
 '$2a$10$3To.3pnbjyyn35PmqTRxYOHdUfgNUVcJvOnXDWy9ePTmLRvz35eAK',
 'Phạm Thị Mai', 'teacher5@languagecenter.local', '0902000005',
 'Huyện Bình Chánh, Thành phố Hồ Chí Minh', 'ACTIVE', NOW(), NOW());

SELECT id INTO @user_teacher_3 FROM `user` WHERE email = 'teacher3@languagecenter.local';
SELECT id INTO @user_teacher_4 FROM `user` WHERE email = 'teacher4@languagecenter.local';
SELECT id INTO @user_teacher_5 FROM `user` WHERE email = 'teacher5@languagecenter.local';

INSERT INTO teacher (user_id, teacher_code, specialization, degree, experience_years) VALUES
(@user_teacher_3, 'GV000003',
 'Tiếng Anh giao tiếp và luyện thi IELTS',
 'Thạc sĩ Ngôn ngữ Anh – Đại học Ngoại ngữ Hà Nội', 4),

(@user_teacher_4, 'GV000004',
 'Tiếng Trung thương mại và luyện thi HSK',
 'Cử nhân Ngôn ngữ Trung – Đại học KHXH & NV TP.HCM', 6),

(@user_teacher_5, 'GV000005',
 'Tiếng Nhật và luyện thi JLPT N3–N2',
 'Thạc sĩ Nhật Bản học – Đại học Osaka', 8);

SELECT id INTO @teacher_1 FROM teacher WHERE teacher_code = 'GV000001';
SELECT id INTO @teacher_2 FROM teacher WHERE teacher_code = 'GV000002';
SELECT id INTO @teacher_3 FROM teacher WHERE teacher_code = 'GV000003';
SELECT id INTO @teacher_4 FROM teacher WHERE teacher_code = 'GV000004';
SELECT id INTO @teacher_5 FROM teacher WHERE teacher_code = 'GV000005';

-- ============================================================
-- 2. PHÒNG HỌC MỚI
-- ============================================================
INSERT INTO room (room_code, room_name, capacity, location, status) VALUES
('P202', 'Phòng 202', 28, 'Tầng 2 - Cơ sở chính', 'ACTIVE'),
('P301', 'Phòng 301', 20, 'Tầng 3 - Cơ sở chính', 'ACTIVE');

SELECT id INTO @room_101 FROM room WHERE room_code = 'P101';
SELECT id INTO @room_102 FROM room WHERE room_code = 'P102';
SELECT id INTO @room_201 FROM room WHERE room_code = 'P201';
SELECT id INTO @room_202 FROM room WHERE room_code = 'P202';
SELECT id INTO @room_301 FROM room WHERE room_code = 'P301';

-- ============================================================
-- 3. RESOLVE CẤP ĐỘ (LEVEL) ĐÃ CÓ SẴN
-- ============================================================
SELECT lv.id INTO @level_en_a1
FROM level lv JOIN language lg ON lg.id = lv.language_id
WHERE lg.language_code = 'EN' AND lv.level_code = 'A1';

SELECT lv.id INTO @level_en_b1
FROM level lv JOIN language lg ON lg.id = lv.language_id
WHERE lg.language_code = 'EN' AND lv.level_code = 'B1';

SELECT lv.id INTO @level_ja_n4
FROM level lv JOIN language lg ON lg.id = lv.language_id
WHERE lg.language_code = 'JA' AND lv.level_code = 'N4';

SELECT lv.id INTO @level_ja_n5
FROM level lv JOIN language lg ON lg.id = lv.language_id
WHERE lg.language_code = 'JA' AND lv.level_code = 'N5';

SELECT lv.id INTO @level_zh_hsk1
FROM level lv JOIN language lg ON lg.id = lv.language_id
WHERE lg.language_code = 'ZH' AND lv.level_code = 'HSK1';

-- ============================================================
-- 4. KHÓA HỌC MỚI (tên tiếng Việt)
-- ============================================================
INSERT INTO course (
    level_id, course_code, course_name, slug, description, tuition_fee,
    total_sessions, duration_hours, status, publication_status,
    published_at, is_featured, created_at, updated_at
) VALUES

-- Khóa 1: Tiếng Anh Thiếu Nhi A1
(@level_en_a1,
 'EN-A1-THIEU-NHI',
 'Tiếng Anh Thiếu Nhi A1',
 'tieng-anh-thieu-nhi-a1',
 'Khóa học tiếng Anh dành cho trẻ em 6–12 tuổi. Chương trình học qua trò chơi, bài hát và hoạt hình giúp các em tiếp cận ngôn ngữ một cách tự nhiên và thú vị.',
 2800000, 24, 48, 'ACTIVE', 'PUBLISHED', NOW(), TRUE, NOW(), NOW()),

-- Khóa 2: Tiếng Anh Văn Phòng B1
(@level_en_b1,
 'EN-B1-VAN-PHONG',
 'Tiếng Anh Văn Phòng B1',
 'tieng-anh-van-phong-b1',
 'Trang bị kỹ năng tiếng Anh thực dụng trong môi trường công sở: viết email chuyên nghiệp, thuyết trình, họp trực tuyến và đàm phán với đối tác nước ngoài.',
 4800000, 30, 60, 'ACTIVE', 'PUBLISHED', NOW(), TRUE, NOW(), NOW()),

-- Khóa 3: Tiếng Nhật Giao Tiếp N4
(@level_ja_n4,
 'JA-N4-GIAO-TIEP',
 'Tiếng Nhật Giao Tiếp N4',
 'tieng-nhat-giao-tiep-n4',
 'Khóa luyện hội thoại tiếng Nhật thực tế theo chuẩn JLPT N4. Phù hợp với người đi làm tại doanh nghiệp Nhật Bản, du học sinh và người yêu thích văn hóa Nhật.',
 5200000, 32, 64, 'ACTIVE', 'PUBLISHED', NOW(), FALSE, NOW(), NOW()),

-- Khóa 4: Tiếng Trung Du Lịch HSK1
(@level_zh_hsk1,
 'ZH-HSK1-DU-LICH',
 'Tiếng Trung Du Lịch HSK1',
 'tieng-trung-du-lich-hsk1',
 'Khóa tiếng Trung căn bản dành cho người muốn du lịch Trung Quốc, Đài Loan hoặc giao tiếp hằng ngày. Tập trung phát âm chuẩn, từ vựng thông dụng và tình huống thực tế theo chuẩn HSK1.',
 3200000, 24, 48, 'ACTIVE', 'PUBLISHED', NOW(), FALSE, NOW(), NOW());

SELECT id INTO @course_en_a1_kid      FROM course WHERE course_code = 'EN-A1-THIEU-NHI';
SELECT id INTO @course_en_b1_office   FROM course WHERE course_code = 'EN-B1-VAN-PHONG';
SELECT id INTO @course_ja_n4_daily    FROM course WHERE course_code = 'JA-N4-GIAO-TIEP';
SELECT id INTO @course_zh_hsk1_travel FROM course WHERE course_code = 'ZH-HSK1-DU-LICH';

-- Resolve các khóa học cũ để tạo thêm lớp
SELECT id INTO @course_en_a1   FROM course WHERE course_code = 'EN-A1-COM';
SELECT id INTO @course_ja_n5   FROM course WHERE course_code = 'JA-N5-STD';
SELECT id INTO @course_zh_hsk1 FROM course WHERE course_code = 'ZH-HSK1-STD';

-- ============================================================
-- 5. 10 LỚP HỌC TRẠNG THÁI OPEN
-- ============================================================
INSERT INTO courseclass (
    course_id, teacher_id, class_code, class_name,
    start_date, end_date,
    max_students, applied_tuition_fee, status,
    created_at, updated_at
) VALUES

-- [1] Tiếng Anh Thiếu Nhi A1 – Sáng Thứ 2,4,6
(@course_en_a1_kid, @teacher_3,
 'EN-A1-KID-01', 'Tiếng Anh Thiếu Nhi A1 – Sáng Thứ 2,4,6',
 DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY),
 DATE_ADD(CURRENT_DATE, INTERVAL 89 DAY),
 15, 2800000, 'OPEN', NOW(), NOW()),

-- [2] Tiếng Anh Thiếu Nhi A1 – Cuối Tuần Sáng
(@course_en_a1_kid, @teacher_3,
 'EN-A1-KID-02', 'Tiếng Anh Thiếu Nhi A1 – Cuối Tuần Sáng',
 DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY),
 DATE_ADD(CURRENT_DATE, INTERVAL 91 DAY),
 15, 2800000, 'OPEN', NOW(), NOW()),

-- [3] Tiếng Anh Văn Phòng B1 – Tối Thứ 2,4,6
(@course_en_b1_office, @teacher_1,
 'EN-B1-VP-01', 'Tiếng Anh Văn Phòng B1 – Tối Thứ 2,4,6',
 DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY),
 DATE_ADD(CURRENT_DATE, INTERVAL 87 DAY),
 20, 4800000, 'OPEN', NOW(), NOW()),

-- [4] Tiếng Anh Văn Phòng B1 – Sáng Thứ 3,5,7
(@course_en_b1_office, @teacher_3,
 'EN-B1-VP-02', 'Tiếng Anh Văn Phòng B1 – Sáng Thứ 3,5,7',
 DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY),
 DATE_ADD(CURRENT_DATE, INTERVAL 90 DAY),
 20, 4800000, 'OPEN', NOW(), NOW()),

-- [5] Tiếng Nhật Giao Tiếp N4 – Tối Thứ 2,4
(@course_ja_n4_daily, @teacher_5,
 'JA-N4-GT-01', 'Tiếng Nhật Giao Tiếp N4 – Tối Thứ 2,4',
 DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY),
 DATE_ADD(CURRENT_DATE, INTERVAL 104 DAY),
 18, 5200000, 'OPEN', NOW(), NOW()),

-- [6] Tiếng Nhật Giao Tiếp N4 – Chiều Cuối Tuần
(@course_ja_n4_daily, @teacher_2,
 'JA-N4-GT-02', 'Tiếng Nhật Giao Tiếp N4 – Chiều Cuối Tuần',
 DATE_ADD(CURRENT_DATE, INTERVAL 8 DAY),
 DATE_ADD(CURRENT_DATE, INTERVAL 92 DAY),
 18, 5200000, 'OPEN', NOW(), NOW()),

-- [7] Tiếng Trung Du Lịch HSK1 – Online Tối Thứ 3,5,7
(@course_zh_hsk1_travel, @teacher_4,
 'ZH-HSK1-DL-01', 'Tiếng Trung Du Lịch HSK1 – Online Tối Thứ 3,5,7',
 DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY),
 DATE_ADD(CURRENT_DATE, INTERVAL 88 DAY),
 25, 3200000, 'OPEN', NOW(), NOW()),

-- [8] English A1 – Sáng Thứ 3,5,7 (lớp 2 của khóa giao tiếp cũ)
(@course_en_a1, @teacher_4,
 'EN-A1-OPEN-02', 'English A1 – Sáng Thứ 3,5,7',
 DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY),
 DATE_ADD(CURRENT_DATE, INTERVAL 89 DAY),
 20, 3200000, 'OPEN', NOW(), NOW()),

-- [9] Japanese N5 – Tối Thứ 2,4,6
(@course_ja_n5, @teacher_5,
 'JA-N5-OPEN-02', 'Japanese N5 – Tối Thứ 2,4,6',
 DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY),
 DATE_ADD(CURRENT_DATE, INTERVAL 91 DAY),
 18, 3800000, 'OPEN', NOW(), NOW()),

-- [10] Chinese HSK1 – Chiều Thứ 3,5 (lớp 2 của khóa cũ)
(@course_zh_hsk1, @teacher_4,
 'ZH-HSK1-OPEN-02', 'Chinese HSK1 – Chiều Thứ 3,5',
 DATE_ADD(CURRENT_DATE, INTERVAL 9 DAY),
 DATE_ADD(CURRENT_DATE, INTERVAL 93 DAY),
 20, 3500000, 'OPEN', NOW(), NOW());

-- Resolve ID các lớp vừa tạo
SELECT id INTO @cls_kid_01     FROM courseclass WHERE class_code = 'EN-A1-KID-01';
SELECT id INTO @cls_kid_02     FROM courseclass WHERE class_code = 'EN-A1-KID-02';
SELECT id INTO @cls_vp_01      FROM courseclass WHERE class_code = 'EN-B1-VP-01';
SELECT id INTO @cls_vp_02      FROM courseclass WHERE class_code = 'EN-B1-VP-02';
SELECT id INTO @cls_ja_n4_01   FROM courseclass WHERE class_code = 'JA-N4-GT-01';
SELECT id INTO @cls_ja_n4_02   FROM courseclass WHERE class_code = 'JA-N4-GT-02';
SELECT id INTO @cls_zh_dl_01   FROM courseclass WHERE class_code = 'ZH-HSK1-DL-01';
SELECT id INTO @cls_en_a1_02   FROM courseclass WHERE class_code = 'EN-A1-OPEN-02';
SELECT id INTO @cls_ja_n5_02   FROM courseclass WHERE class_code = 'JA-N5-OPEN-02';
SELECT id INTO @cls_zh_hsk1_02 FROM courseclass WHERE class_code = 'ZH-HSK1-OPEN-02';

-- ============================================================
-- 6. LỊCH HỌC CHO 10 LỚP MỚI
-- ============================================================
INSERT INTO classschedule (
    course_class_id, room_id, day_of_week, start_time, end_time,
    delivery_mode, meeting_url
) VALUES

-- [1] EN-A1-KID-01 : Thứ 2,4,6  08:00–10:00  Phòng 102
(@cls_kid_01, @room_102, 1, '08:00:00', '10:00:00', 'IN_PERSON', NULL),
(@cls_kid_01, @room_102, 3, '08:00:00', '10:00:00', 'IN_PERSON', NULL),
(@cls_kid_01, @room_102, 5, '08:00:00', '10:00:00', 'IN_PERSON', NULL),

-- [2] EN-A1-KID-02 : Thứ 7, CN  08:00–11:00  Phòng 102
(@cls_kid_02, @room_102, 6, '08:00:00', '11:00:00', 'IN_PERSON', NULL),
(@cls_kid_02, @room_102, 7, '08:00:00', '11:00:00', 'IN_PERSON', NULL),

-- [3] EN-B1-VP-01 : Thứ 2,4,6  18:00–20:30  Phòng 201
(@cls_vp_01, @room_201, 1, '18:00:00', '20:30:00', 'IN_PERSON', NULL),
(@cls_vp_01, @room_201, 3, '18:00:00', '20:30:00', 'IN_PERSON', NULL),
(@cls_vp_01, @room_201, 5, '18:00:00', '20:30:00', 'IN_PERSON', NULL),

-- [4] EN-B1-VP-02 : Thứ 3,5,7  09:00–11:30  Phòng 202
(@cls_vp_02, @room_202, 2, '09:00:00', '11:30:00', 'IN_PERSON', NULL),
(@cls_vp_02, @room_202, 4, '09:00:00', '11:30:00', 'IN_PERSON', NULL),
(@cls_vp_02, @room_202, 6, '09:00:00', '11:30:00', 'IN_PERSON', NULL),

-- [5] JA-N4-GT-01 : Thứ 2,4  19:00–21:00  Phòng 301
(@cls_ja_n4_01, @room_301, 1, '19:00:00', '21:00:00', 'IN_PERSON', NULL),
(@cls_ja_n4_01, @room_301, 3, '19:00:00', '21:00:00', 'IN_PERSON', NULL),

-- [6] JA-N4-GT-02 : Thứ 7, CN  13:30–16:30  Phòng 102
(@cls_ja_n4_02, @room_102, 6, '13:30:00', '16:30:00', 'IN_PERSON', NULL),
(@cls_ja_n4_02, @room_102, 7, '13:30:00', '16:30:00', 'IN_PERSON', NULL),

-- [7] ZH-HSK1-DL-01 : Thứ 3,5,7  19:30–21:30  Online
(@cls_zh_dl_01, NULL, 2, '19:30:00', '21:30:00', 'ONLINE',
 'https://meet.example.com/zh-hsk1-dl-01'),
(@cls_zh_dl_01, NULL, 4, '19:30:00', '21:30:00', 'ONLINE',
 'https://meet.example.com/zh-hsk1-dl-01'),
(@cls_zh_dl_01, NULL, 6, '19:30:00', '21:30:00', 'ONLINE',
 'https://meet.example.com/zh-hsk1-dl-01'),

-- [8] EN-A1-OPEN-02 : Thứ 3,5,7  09:00–11:00  Phòng 101
(@cls_en_a1_02, @room_101, 2, '09:00:00', '11:00:00', 'IN_PERSON', NULL),
(@cls_en_a1_02, @room_101, 4, '09:00:00', '11:00:00', 'IN_PERSON', NULL),
(@cls_en_a1_02, @room_101, 6, '09:00:00', '11:00:00', 'IN_PERSON', NULL),

-- [9] JA-N5-OPEN-02 : Thứ 2,4,6  19:00–21:00  Phòng 201
(@cls_ja_n5_02, @room_201, 1, '19:00:00', '21:00:00', 'IN_PERSON', NULL),
(@cls_ja_n5_02, @room_201, 3, '19:00:00', '21:00:00', 'IN_PERSON', NULL),
(@cls_ja_n5_02, @room_201, 5, '19:00:00', '21:00:00', 'IN_PERSON', NULL),

-- [10] ZH-HSK1-OPEN-02 : Thứ 3,5  14:00–16:00  Phòng 301
(@cls_zh_hsk1_02, @room_301, 2, '14:00:00', '16:00:00', 'IN_PERSON', NULL),
(@cls_zh_hsk1_02, @room_301, 4, '14:00:00', '16:00:00', 'IN_PERSON', NULL);

COMMIT;

-- ============================================================
-- Kiểm tra nhanh sau khi chạy
-- ============================================================
SELECT 'course'        AS bang, COUNT(*) AS tong FROM course
UNION ALL SELECT 'teacher',     COUNT(*) FROM teacher
UNION ALL SELECT 'room',        COUNT(*) FROM room
UNION ALL SELECT 'courseclass', COUNT(*) FROM courseclass
UNION ALL SELECT 'classschedule', COUNT(*) FROM classschedule;
