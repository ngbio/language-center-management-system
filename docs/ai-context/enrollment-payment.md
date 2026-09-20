# Enrollment, payment and refund context

Đã đối chiếu với source tại commit nền `763bd81`; cập nhật gần nhất: 2026-09-13.

## Thành phần chính

- Enrollment: `EnrollmentService` / `EnrollmentServiceImpl`, `EnrollmentRepository`, `Enrollment`.
- Payment: `PaymentService` / `PaymentServiceImpl`, `PaymentRepository`, `Payment`.
- Billing/refund: `BillingService` / `BillingServiceImpl`, `RefundRepository`, `Refund`.
- Invoice: `InvoicePdfService` / `InvoicePdfServiceImpl`.
- Security routes: `config/SecurityConfig.java`.
- Frontend endpoints: nhóm enrollment/payment/refund trong `frontend/src/configs/Apis.js`.

## Luồng enrollment và payment

```text
Student tự đăng ký hoặc Consultant xếp lớp
→ tạo enrollment và giữ chỗ
→ Student tạo payment MoMo/ZaloPay (PENDING)
→ gateway callback được backend xác minh
→ payment thành công
→ enrollment CONFIRMED + PAID
→ mở quyền truy cập lớp/nội dung
```

- `POST /api/enrollments` chỉ dành cho Student.
- Admin/Consultant thao tác qua `/api/staff/enrollments/**`.
- Student tạo payment qua `/api/payments` hoặc `/api/enrollments/{id}/payments`.
- Callback MoMo/ZaloPay được public để gateway gọi, nhưng backend phải xác minh chữ ký.
- Giao dịch thất bại vẫn có thể được lưu để đối soát; enrollment giữ trạng thái độc lập.
- Enrollment hết hạn được xử lý bởi `EnrollmentExpirationServiceImpl`.

## Luồng refund

```text
Admin/Consultant tạo yêu cầu
→ Refund PENDING với transaction/idempotency data
→ gọi gateway hoặc refresh trạng thái
→ COMPLETED: chỉ khi tổng hoàn thành công đủ tổng thu mới hủy enrollment và thu hồi quyền học
→ FAILED/PENDING: giữ dữ liệu để kiểm tra hoặc đối soát tiếp
```

- Staff routes: `/api/staff/refunds/**` và `/api/staff/enrollments/{id}/refunds`.
- Không thu hồi quyền trước khi gateway xác nhận hoàn tiền thành công.
- Hoàn một phần giữ nguyên trạng thái enrollment; hoàn đủ chuyển `REFUNDED + CANCELLED`.
- Refund hiện chưa phát email; hướng dẫn luồng và tái sử dụng Brevo: [REFUND_VA_BREVO_GUIDE.md](../REFUND_VA_BREVO_GUIDE.md).
- Mọi thao tác nhiều bảng phải nằm trong transaction và hỗ trợ retry/idempotency.

## Trạng thái

Các enum nằm trong package `enums`: `EnrollmentStatus`, `EnrollmentPaymentStatus`, `PaymentTransactionStatus`, `PaymentMethod`, `RefundStatus`. Luôn đọc enum và validation hiện tại trước khi thêm transition; không suy đoán trạng thái chỉ từ tên nút UI.

## Quy tắc nghiệp vụ cần giữ

- Frontend và Consultant không được tự đánh dấu `PAID`.
- Chỉ callback hợp lệ từ server mới xác nhận payment.
- Quyền nội dung trả phí yêu cầu enrollment `CONFIRMED + PAID`.
- Transfer enrollment phải giữ cùng khóa học và kiểm tra sức chứa/lịch theo service hiện tại.
- Cancel/refund/payment phải kiểm tra owner hoặc role quản lý.
- Email sau giao dịch được phát qua event sau khi transaction thành công.

## Cách đọc theo thay đổi

1. Tìm endpoint trong controller và `Apis.js`.
2. Mở method service tương ứng cùng enum trạng thái.
3. Chỉ đọc entity/repository của bảng bị thay đổi.
4. Kiểm tra unit test service và integration test controller cùng luồng.
5. Nếu sửa callback, test chữ ký sai, callback lặp và thứ tự callback bất thường.

