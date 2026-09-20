# Database context

Đã đối chiếu với `database_language_center_mysql.sql` tại commit nền `763bd81`; cập nhật gần nhất: 2026-09-13.

## Nguồn dữ liệu

- Schema tạo database mới: `database_language_center_mysql.sql`.
- Migration password reset cho database cũ: `migrate_add_password_reset_mysql.sql`.
- Seed chính/phụ: các file `seed_*.sql` ở root.
- Migration tự học cho DB cũ: `migrate_add_learning_review_mysql.sql` (chạy một lần, chọn đúng database).
- JPA entity: `backend/language-center-management/src/main/java/com/ntt/language_center_management/entity/`.
- Hibernate không tự sửa schema (`spring.jpa.hibernate.ddl-auto=none`).

## Nhóm bảng

- Identity: `role`, `user`, `student`, `teacher`, `password_reset_token`.
- Catalog: `language`, `level`, `course`, `course_section`, `course_content`.
- Vận hành lớp: `room`, `courseclass`, `classschedule`, `lesson`.
- Đăng ký/tài chính: `enrollment`, `payment`, `refund`.
- Học tập: `attendance`, `notification`.
- Tự học: `flashcard`, `flashcard_review`, `quiz`, `quiz_question`, `quiz_option`, `quiz_attempt`, `quiz_attempt_answer`.
- Audit: `system_logs`.

## Quan hệ chính

```text
role → user → student/teacher
language → level → course → course_section → course_content
course → courseclass → classschedule → lesson
teacher → courseclass
room → classschedule
student + courseclass → enrollment
enrollment → payment/refund/attendance
lesson → attendance
user → notification/password_reset_token/system_logs
```

## Quy tắc thay đổi schema

1. Xác định thay đổi dành cho database mới, database đang tồn tại hay cả hai.
2. Cập nhật entity và DTO/service liên quan cùng schema.
3. Nếu production đã có dữ liệu, tạo migration idempotent hoặc có hướng dẫn chạy một lần; không chỉ sửa file schema gốc.
4. Kiểm tra foreign key, unique constraint, index, giá trị enum và nullability.
5. Không sửa seed không liên quan và không ghi secret vào SQL.

## Cách đọc tiết kiệm

- Tìm bảng bằng `rg -n "CREATE TABLE <name>|REFERENCES <name>" database_language_center_mysql.sql`.
- Chỉ mở đoạn `CREATE TABLE` tương ứng và entity liên quan.
- Với API payload, ưu tiên DTO; không suy đoán JSON trực tiếp từ cột database.
- Với enum, đọc class trong package `enums` và constraint/default trong schema.

## Điểm dễ sai

- Entity class dùng tên `Courseclass` và `Classschedule`, trong khi khái niệm nghiệp vụ thường viết CourseClass/ClassSchedule.
- Payment và enrollment có trạng thái riêng; không đồng nhất hai trạng thái bằng suy đoán.
- Refund tham chiếu cả enrollment, payment và user xử lý.
- Attendance gắn với lesson và enrollment, không gắn trực tiếp student.
- Password reset token bị vô hiệu hóa theo vòng đời riêng; không dùng lại token cũ.
- Flashcard và quiz thuộc `course_content`; lịch ôn/lượt làm thuộc `student`, dùng được cho khóa miễn phí không có lớp/enrollment.
- Không có bảng tiến độ. Khóa có học phí kiểm tra enrollment CONFIRMED + PAID khi truy cập.
- Foreign key giữ lịch sử quiz; nội dung có flashcard/quiz không được xóa cứng. Gỡ xuất bản/lưu trữ để ẩn.
