# Project context instructions

Mục tiêu của tài liệu này là định tuyến việc đọc source, tránh quét lại toàn bộ repository.

## Cách bắt đầu một tác vụ

1. Đọc file ngữ cảnh nhỏ nhất phù hợp trong `docs/ai-context/`.
2. Chỉ dùng `rg` để tìm các symbol, route hoặc file được nhắc trong tài liệu đó.
3. Chỉ mở source trực tiếp tham gia vào thay đổi và test liên quan.
4. Không đọc toàn bộ `README.md`, toàn bộ controller/service hoặc toàn bộ schema nếu tác vụ không yêu cầu.

## Định tuyến ngữ cảnh

- React, CSS, route, màn hình: `docs/ai-context/frontend.md`
- Spring Boot, controller, service, repository: `docs/ai-context/backend.md`
- MySQL, entity, quan hệ dữ liệu, seed/migration: `docs/ai-context/database.md`
- Đăng nhập, JWT, role, đổi/quên mật khẩu: `docs/ai-context/authentication.md`
- Enrollment, payment, refund, invoice: `docs/ai-context/enrollment-payment.md`
- Chọn và chạy kiểm thử: `docs/ai-context/testing.md`

Nếu tác vụ thuộc nhiều domain, chỉ đọc các file tương ứng. Ví dụ sửa thanh toán phía học viên thì đọc `frontend.md` và `enrollment-payment.md`; chỉ đọc `backend.md` nếu cần thay API.

## Nguồn sự thật

- Source code và cấu hình đang chạy là nguồn chính xác nhất.
- `database_language_center_mysql.sql` là schema MySQL tạo mới; migration riêng dùng cho database đã tồn tại.
- `frontend/src/configs/Apis.js` là bản đồ endpoint mà frontend đang sử dụng.
- `backend/.../config/SecurityConfig.java` là bản đồ quyền truy cập HTTP.
- `docs/API_DOCUMENTATION.md` chứa đặc tả API chi tiết khi thật sự cần payload hoặc response.

Nếu tài liệu ngữ cảnh khác source, làm theo source và cập nhật tài liệu ngữ cảnh trong cùng thay đổi.

## Quy tắc giữ tài liệu gọn

- Ghi kiến trúc, dependency và điều dễ sai; không sao chép nguyên source.
- Không liệt kê từng method nếu có thể tìm nhanh bằng tên class.
- Khi thêm một module lớn, cập nhật file domain tương ứng và mục định tuyến ở đây.
- Mỗi file ngữ cảnh nên giữ dưới khoảng 150 dòng.

## An toàn và phạm vi

- Không sửa secret hoặc commit file `.env`.
- Không tự thay đổi schema production; phân biệt schema tạo mới, migration và seed.
- Không làm mất thay đổi chưa commit của người dùng.
- Chạy kiểm tra tỷ lệ với phạm vi thay đổi theo `docs/ai-context/testing.md`.

