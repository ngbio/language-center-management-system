# Backend context

Đã đối chiếu với source tại commit nền `763bd81`; cập nhật gần nhất: 2026-09-13.

## Stack và điểm vào

- Java 25, Spring Boot 4.1, Spring MVC, Spring Security, Spring Data JPA, MySQL.
- Root: `backend/language-center-management/`.
- Main class: `src/main/java/com/ntt/language_center_management/LanguageCenterManagementApplication.java`.
- Cấu hình runtime: `src/main/resources/application.properties`.
- Server mặc định: `8081`; API prefix: `/api`.
- Hibernate `ddl-auto=none`: schema không được tự tạo/cập nhật từ entity.

## Luồng request

```text
HTTP → Security/JWT filter → Controller → Service → Repository → MySQL
                                  ↓          ↓
                                 DTO       Mapper/Event/Integration
```

- Controller chỉ điều phối HTTP và validation đầu vào.
- Business rule, ownership và transaction đặt trong service.
- Repository dùng Spring Data JPA; projection nằm ở `repository/projection`.
- Request/response model tách trong `dto/request` và `dto/response`.
- Exception API xử lý tập trung tại `exception/GlobalExceptionHandler.java`.

## Controller theo quyền

- `controller/publicapi`: catalog công khai.
- `controller/student`: dữ liệu và hành động của học viên hiện tại.
- `controller/teacher`: lớp, khóa học, hồ sơ và điểm danh của giáo viên.
- `controller/admin`: catalog, user, dashboard, log hệ thống.
- `controller/management`: nghiệp vụ Admin/Consultant và lịch học.
- `controller/chat`, `controller/media`: chat token và upload ảnh.

Quyền URL cuối cùng nằm trong `config/SecurityConfig.java`. Service vẫn phải kiểm tra ownership của dữ liệu.

## Domain service

- Catalog: `LanguageService`, `LevelService`, `CourseService`, `CourseCurriculumService`, `RoomService`.
- Tự học: `LearningService` / `LearningServiceImpl`, DTO nhóm `LearningRequest` và `LearningResponse`.
- Lớp/lịch/buổi học: `CourseClassService`, `ClassScheduleService`, `LessonService`.
- Đăng ký/tài chính: `EnrollmentService`, `PaymentService`, `BillingService`, `InvoicePdfService`.
- Điểm danh: `AttendanceService`.
- Tài khoản: `UserService`, `TeacherService`, `PasswordResetService`.
- Hỗ trợ: `NotificationService`, `FirebaseChatService`, `ImageUploadService`, `DashboardService`, `SystemLogService`.
- Scheduler: `EnrollmentExpirationServiceImpl`, `LessonCompletionScheduler`, `LessonReminderScheduler`.

Interface ở `service/`; implementation ở `service/impl/`. Khi thay contract, kiểm tra controller, implementation và test cùng domain.

## Tích hợp ngoài

- Payment: MoMo và ZaloPay qua service/payment support classes.
- Email: `MailGateway`, `BrevoMailGateway`, `SmtpMailGateway`; sự kiện email trong `event/`.
- Ảnh: Cloudinary qua `ImageUploadService` và `config/CloudinaryConfig.java`.
- Chat: Firebase custom token/service qua `FirebaseChatService`.
- Invoice PDF: `InvoicePdfServiceImpl`, PDFBox và font Noto Sans trong resources.

## Quy tắc quan trọng

- Tự học: public `/api/public/learning`, student `/api/students/me/learning`, admin `/api/admin/learning`.
- Quiz chấm ở server; DTO public không chứa đáp án/giải thích trước khi nộp. Khách free chấm tức thời không lưu DB.
- Khóa quiz khi bắt đầu lượt làm để giữ giới hạn, khóa lượt làm khi nộp; tiếp tục lượt chưa nộp thay vì tạo mới.
- Câu hỏi chỉ sửa khi quiz DRAFT và chưa có attempt. Sau khi có attempt không đổi điểm đạt/giới hạn.
- `PublicQuizRateLimiter`: tối đa 20 lần nộp/phút/IP trên mỗi backend instance, không tin X-Forwarded-For.

- API stateless, xác thực bằng Bearer JWT.
- Transaction nhiều bước phải rollback toàn bộ khi validation thất bại.
- Thời gian nghiệp vụ dùng `ApplicationDateTimeUtils` và múi giờ cấu hình Asia/Ho_Chi_Minh.
- Frontend không được tự xác nhận payment thành công.
- Không đưa credential thanh toán, Firebase, Cloudinary hoặc mail vào Git.

## Cách đọc theo tác vụ

Tìm controller endpoint → service interface → đúng method implementation → repository/entity liên quan → test cùng tên. Chỉ mở `SecurityConfig` khi route hoặc role liên quan thay đổi.
