/*
  Dữ liệu bổ sung cho LanguageCenterDB
  ======================================
  Chạy seed_language_center_mysql.sql TRƯỚC, sau đó chạy file này.

  Nội dung file này:
    - 3 giáo viên mới  (GV000003 – GV000005)
    - 2 phòng học mới  (P202, P301)
    - Không tạo thêm khóa học, lớp học hoặc lịch học.
    - Giữ 2 khóa EN-A1-COM, JA-N5-STD và 2 lớp trong seed chính.
    - Các file seed N5 nội dung/quiz riêng vẫn có thể tạo thêm khóa.

  Bản rút gọn không xóa dữ liệu đã nạp vào database trước đây.

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


-- ============================================================
-- 2. PHÒNG HỌC MỚI
-- ============================================================
INSERT INTO room (room_code, room_name, capacity, location, status) VALUES
('P202', 'Phòng 202', 28, 'Tầng 2 - Cơ sở chính', 'ACTIVE'),
('P301', 'Phòng 301', 20, 'Tầng 3 - Cơ sở chính', 'ACTIVE');

COMMIT;

-- ============================================================
-- Kiểm tra nhanh sau khi chạy
-- ============================================================
SELECT 'course'        AS bang, COUNT(*) AS tong FROM course
UNION ALL SELECT 'teacher',     COUNT(*) FROM teacher
UNION ALL SELECT 'room',        COUNT(*) FROM room
UNION ALL SELECT 'courseclass', COUNT(*) FROM courseclass
UNION ALL SELECT 'classschedule', COUNT(*) FROM classschedule;
