# Authentication context

Đã đối chiếu với source tại commit nền `763bd81`; cập nhật gần nhất: 2026-09-13.

## Thành phần chính

- HTTP authorization: `config/SecurityConfig.java`.
- JWT parsing/signing: `util/JwtUtils.java`.
- Bearer filter: `filters/JwtFilter.java`.
- Current identity helper: `security/CurrentUserResolver.java`.
- Account business logic: `service/UserService.java` và `service/impl/UserServiceImpl.java`.
- Frontend session: `frontend/src/utils/authSession.js`.
- Frontend Axios auth client: `frontend/src/configs/Apis.js`.

## Roles

- `ADMIN`: toàn quyền quản trị catalog, user, dashboard và vận hành.
- `CONSULTANT`: quản lý enrollment/refund và chat tư vấn.
- `STUDENT`: hồ sơ, enrollment, payment, khóa/lớp và điểm danh của bản thân.
- `TEACHER`: lớp được phân công, lesson, attendance và hồ sơ giáo viên.

## Endpoint auth

- Public auth: `/api/auth/**`.
- Tự học miễn phí: GET `/api/public/learning/**`, POST `/api/public/learning/quizzes/*/evaluate`; service vẫn kiểm tra khóa ACTIVE/PUBLISHED/free.
- Lịch sử tự học: `/api/students/me/learning/**` chỉ STUDENT và kiểm tra owner; `/api/admin/learning/**` chỉ ADMIN.
- Admin login: `POST /api/admin/auth/login`.
- Staff login: `POST /api/staff/auth/login`.
- Current account: `GET /api/auth/me`.
- Change password: `PUT /api/auth/change-password`.
- Forgot/reset password nằm dưới `/api/auth/` và dùng `PasswordResetService`.

Đối chiếu payload/response chi tiết tại `docs/API_DOCUMENTATION.md` hoặc đúng controller/DTO; không suy đoán field.

## Session phía frontend

- localStorage keys: `token`, `role`, `email`.
- `isTokenActive` đọc claim `exp` của JWT để tránh dùng token hết hạn.
- Axios interceptor xóa session và chuyển về trang login phù hợp khi nhận `401`.
- Frontend route guard không thay thế backend authorization.

## Password reset

```text
email → tạo token một lần → gửi link frontend → validate token
      → đặt mật khẩu mới → vô hiệu hóa token/JWT cũ → gửi thông báo
```

- Service: `PasswordResetService` / `PasswordResetServiceImpl`.
- Entity/repository: `PasswordResetToken`, `PasswordResetTokenRepository`.
- Mail event/gateway nằm trong `event/` và `service/impl/`.
- Frontend: `ForgotPasswordScreen.jsx`, `ResetPasswordScreen.jsx`.
- Database cũ cần migration `migrate_add_password_reset_mysql.sql`.

## Quy tắc an toàn

- Mật khẩu phải hash bằng BCrypt; không log password hoặc reset token.
- `401` dành cho thiếu/sai authentication; `403` dành cho đã xác thực nhưng thiếu quyền.
- Khi thêm endpoint, cập nhật `SecurityConfig` có chủ đích và kiểm tra ownership trong service.
- Không dùng email/role do client gửi để thay cho identity trong JWT.
- Test cả token hết hạn, sai role, truy cập chéo tài nguyên và token reset dùng lại.
