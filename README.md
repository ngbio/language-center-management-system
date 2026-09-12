# Language Center Management System

Nền tảng quản lý trung tâm ngoại ngữ tích hợp website giới thiệu khóa học, cổng học viên, không gian làm việc của giảng viên và hệ thống vận hành dành cho quản trị viên, tư vấn viên.

Hệ thống quản lý xuyên suốt vòng đời đào tạo: từ công bố khóa học, mở lớp, đăng ký, thanh toán, xếp lịch, tổ chức buổi học, điểm danh cho đến hoàn tiền và xuất hóa đơn.

## Tổng quan

Language Center Management System giải quyết các bài toán vận hành chính của một trung tâm ngoại ngữ trong cùng một hệ thống:

- Quản lý ngôn ngữ, trình độ, khóa học và nội dung học tập.
- Tổ chức lớp theo giảng viên, phòng học, lịch cố định và ngày khai giảng.
- Tiếp nhận đăng ký trực tiếp hoặc xếp lớp bởi nhân viên tư vấn.
- Quản lý thời hạn giữ chỗ, thanh toán trực tuyến và quyền truy cập khóa học.
- Cung cấp thời khóa biểu riêng cho học viên và giảng viên.
- Sinh buổi học từ lịch cố định, quản lý thay đổi lịch và trạng thái buổi học.
- Điểm danh, theo dõi tiến độ và tổng hợp tỷ lệ chuyên cần.
- Xử lý hoàn tiền, đối soát trạng thái và xuất hóa đơn PDF.
- Quản lý hình ảnh khóa học và ảnh đại diện trên Cloudinary.

## Các phân hệ chính

### Website công khai

- Giới thiệu trung tâm và đội ngũ giảng viên.
- Khám phá các ngôn ngữ và trình độ đang đào tạo.
- Tìm kiếm, lọc, sắp xếp và phân trang khóa học.
- Xem khóa học nổi bật, nội dung học thử và lớp đang mở.
- Lọc lớp theo khóa học, trình độ và ngày khai giảng.
- Xem lịch học, học phí, số chỗ còn lại và địa điểm học.
- Đăng ký tài khoản Student hoặc Teacher.

Dữ liệu Public được giới hạn theo trạng thái hoạt động và xuất bản. Link phòng học trực tuyến không được công khai cho người không có quyền.

### Cổng học viên

- Xem khóa học đã mua và khóa học miễn phí.
- Xem lớp đang tham gia, phòng học và thời khóa biểu cá nhân.
- Theo dõi lịch sử đăng ký và thời hạn thanh toán.
- Thanh toán qua MoMo hoặc ZaloPay.
- Tải hóa đơn PDF sau khi thanh toán thành công.
- Xem lịch sử thanh toán và trạng thái hoàn tiền.
- Truy cập nội dung đầy đủ khi enrollment đạt `CONFIRMED + PAID`.
- Xem kết quả điểm danh và tỷ lệ chuyên cần.
- Cập nhật hồ sơ và tải ảnh đại diện.

### Không gian giảng viên

- Xem khóa học và lớp được phân công.
- Xem thời khóa biểu, phòng học hoặc link học online.
- Sinh danh sách buổi học từ lịch cố định của lớp.
- Cập nhật chủ đề và nội dung buổi học.
- Xem danh sách học viên hợp lệ trong lớp.
- Điểm danh hàng loạt hoặc cập nhật từng học viên.
- Xem tổng hợp điểm danh và tỷ lệ chuyên cần.
- Cập nhật hồ sơ giảng viên.

Giảng viên chỉ được thao tác trên lớp mình phụ trách. Buổi học tự chuyển sang `COMPLETED` sau giờ kết thúc; điểm danh vẫn có thể được điều chỉnh trong thời hạn nghiệp vụ cho phép.

### Quản trị hệ thống

- Quản lý người dùng, vai trò và trạng thái tài khoản.
- Kích hoạt tài khoản giảng viên sau đăng ký.
- Quản lý ngôn ngữ và trình độ.
- Quản lý khóa học, trạng thái xuất bản và khóa học nổi bật.
- Quản lý section và nội dung khóa học.
- Quản lý phòng học và trạng thái sử dụng phòng.
- Tạo lớp, phân công giảng viên và chuyển trạng thái lớp.
- Thiết lập lịch cố định, phòng học hoặc hình thức online.
- Quản lý enrollment, thanh toán và hoàn tiền.

### Cổng tư vấn viên

- Tìm học viên bằng email tài khoản.
- Lọc lớp theo khóa học trước khi xếp lớp.
- Tạo enrollment cho học viên.
- Quản lý đăng ký, hủy và chuyển lớp cùng khóa học.
- Theo dõi trạng thái thanh toán.
- Tạo yêu cầu hoàn tiền và đối soát kết quả với cổng thanh toán.

Consultant không được tự đánh dấu enrollment là đã thanh toán. Trạng thái `PAID` chỉ được cập nhật từ callback hợp lệ của MoMo hoặc ZaloPay.

## Luồng đăng ký và thanh toán

```text
Student tự đăng ký hoặc Consultant xếp lớp
                    ↓
Enrollment được tạo và giữ chỗ trong thời hạn quy định
                    ↓
Student chọn MoMo hoặc ZaloPay
                    ↓
Payment = PENDING
                    ↓
Callback hợp lệ từ cổng thanh toán
                    ↓
Payment = PAID
Enrollment = CONFIRMED + PAID
                    ↓
Kích hoạt quyền truy cập lớp và nội dung khóa học
```

Giao dịch thất bại được lưu tại bảng payment để phục vụ tra cứu, trong khi enrollment tiếp tục chờ thanh toán cho đến khi hết hạn.

## Luồng hoàn tiền

```text
Consultant/Admin tạo yêu cầu hoàn tiền
                    ↓
Refund = PENDING
                    ↓
Backend ký request và gửi đến MoMo/ZaloPay
                    ↓
COMPLETED: thu hồi quyền học và cập nhật enrollment
FAILED: lưu lỗi để kiểm tra
PENDING: tiếp tục đối soát khi chưa có kết quả cuối cùng
```

Mỗi yêu cầu hoàn tiền có mã giao dịch và idempotency key nhằm hạn chế gửi trùng. Quyền học chỉ thay đổi sau khi cổng thanh toán xác nhận hoàn tiền thành công.

## Quản lý lịch và buổi học

Lớp được cấu hình bằng lịch cố định trong tuần, ví dụ thứ Hai, Tư, Sáu. Từ lịch này, hệ thống sinh các lesson thực tế trong khoảng ngày bắt đầu và kết thúc của lớp.

Khi dời một buổi học, hệ thống kiểm tra:

- Buổi học chưa bắt đầu và chưa bị hủy hoặc hoàn thành.
- Buổi học chưa có dữ liệu điểm danh.
- Ngày mới thuộc thời gian hoạt động của lớp và chưa diễn ra.
- Không trùng phòng học hoặc lịch của giảng viên.
- Không tạo lesson trùng trên cùng lịch.

Hệ thống lưu ngày ban đầu, ngày mới, lý do, thời điểm và người thực hiện để đảm bảo khả năng truy vết. Việc gửi thông báo tự động khi thay đổi lịch được định hướng tích hợp qua module Notification.

## Điểm nổi bật

### Phân quyền theo vai trò và quyền sở hữu dữ liệu

Hệ thống không chỉ kiểm tra role tại tầng Security mà còn kiểm tra quyền trên từng tài nguyên. Giảng viên không thể sửa lớp của người khác; học viên không thể xem lớp hoặc dữ liệu điểm danh của tài khoản khác.

### Kiểm soát quyền truy cập nội dung

- Khóa học miễn phí trả nội dung đã xuất bản.
- Khóa học có phí chỉ công khai nội dung học thử.
- Nội dung đầy đủ yêu cầu enrollment `CONFIRMED + PAID`.
- Khóa học và lớp Public phải có trạng thái hoạt động, xuất bản phù hợp.

### Thanh toán được xác nhận từ phía máy chủ

Frontend không có quyền tự chuyển giao dịch thành `PAID`. Backend xác minh callback và chữ ký của cổng thanh toán trước khi cập nhật payment, enrollment và quyền học.

### Tính nhất quán giao dịch

Các nghiệp vụ nhiều bước như đăng ký, điểm danh, thanh toán và hoàn tiền được xử lý trong transaction. Khi validation thất bại, toàn bộ thay đổi liên quan được rollback để tránh dữ liệu lưu dở dang.

### Phòng chống xung đột lịch

Trước khi mở lịch hoặc dời lesson, hệ thống kiểm tra đồng thời tài nguyên phòng học và giảng viên, tránh hai lớp sử dụng cùng tài nguyên trong cùng thời gian.

### Điểm danh có kiểm soát

Danh sách điểm danh chỉ bao gồm học viên đã xác nhận và thanh toán. Backend hỗ trợ lưu hàng loạt, sửa riêng từng bản ghi và giới hạn thời gian chỉnh sửa sau ngày học. Việc lưu hàng loạt sử dụng tra cứu theo `Map` để tránh N+1 query.

### Quản lý thời gian theo múi giờ nghiệp vụ

Scheduler và validation thời gian sử dụng múi giờ cấu hình `Asia/Ho_Chi_Minh`, tránh sai lệch khi triển khai backend trên máy chủ sử dụng UTC.

### Bảo vệ dữ liệu Public

API Public chỉ cung cấp dữ liệu cần thiết cho việc giới thiệu và đăng ký. Meeting URL của lớp online được che với người không có quyền quản lý hoặc không thuộc lớp.

### Tích hợp dịch vụ ngoài

- MoMo Sandbox và ZaloPay Sandbox cho thanh toán, hoàn tiền.
- Cloudinary cho ảnh đại diện, thumbnail và banner khóa học.
- Brevo HTTPS API cho email giao dịch khi triển khai trên Render; SMTP vẫn có thể dùng
  trong môi trường local.
- PDFBox cho hóa đơn PDF hỗ trợ nội dung tiếng Việt.
- JWT cho xác thực stateless giữa frontend và backend.

## Kiến trúc hệ thống

```text
React SPA
   ↓ REST API / JWT
Spring Boot
   ├── Security & JWT Filter
   ├── Controller
   ├── Service & Transaction
   ├── Repository / Spring Data JPA
   ├── Scheduler
   └── Integration: MoMo, ZaloPay, Cloudinary, Brevo, PDF
            ↓
          MySQL
```

Backend được tổ chức theo các tầng Controller, Service, Repository, Mapper, DTO và Entity. Response API được chuẩn hóa, exception được xử lý tập trung, còn phân quyền nghiệp vụ được đặt tại service để hạn chế truy cập chéo dữ liệu.

## Công nghệ sử dụng

### Backend

- Java 25
- Spring Boot 4
- Spring Web MVC
- Spring Security và JWT
- Spring Data JPA / Hibernate
- MySQL
- Bean Validation
- PDFBox
- Cloudinary Java SDK
- Brevo Email API qua HTTPS

### Frontend

- React 19
- React Router
- Axios
- Vite
- CSS responsive cho từng khu vực Public, Student, Teacher, Staff và Admin

## Gửi email khi deploy Railway hoặc Render

Backend hỗ trợ `smtp` và `brevo`. Trên Railway hoặc Render nên chọn Brevo để email được
gửi qua HTTPS cổng 443. Sau khi xác minh địa chỉ gửi hoặc domain và tạo API key trong
Brevo, cấu hình các biến môi trường sau trên nền tảng deploy:

```env
MAIL_ENABLED=true
MAIL_PROVIDER=brevo
MAIL_FROM=Lingua Center <your-verified-sender@example.com>
BREVO_API_KEY=xkeysib-CHANGE_ME
BREVO_BASE_URL=https://api.brevo.com
APP_FRONTEND_URL=https://your-frontend.example.com
PASSWORD_RESET_EXPIRATION_MINUTES=30
PASSWORD_RESET_COOLDOWN_SECONDS=60
```

Không đưa `BREVO_API_KEY` vào Git hoặc Docker image. Khi bật Brevo mà thiếu
`BREVO_API_KEY` hoặc `MAIL_FROM`, backend sẽ từ chối khởi động và báo rõ biến còn thiếu.
Email đăng ký, thanh toán và đặt lại mật khẩu được gửi bất đồng bộ sau khi transaction
thành công; lỗi từ Brevo được ghi trong log nhưng không rollback nghiệp vụ đã hoàn tất.
`APP_FRONTEND_URL` phải là URL HTTPS của frontend đã deploy vì email quên mật khẩu sẽ trỏ
đến `${APP_FRONTEND_URL}/reset-password?token=...`.

Nếu database đã tồn tại từ phiên bản trước, chạy một lần
`migrate_add_password_reset_mysql.sql` trước khi deploy backend mới. Với database tạo mới,
`database_language_center_mysql.sql` đã có sẵn bảng token và cột vô hiệu hóa JWT cũ.

## Tài liệu dự án

- [API Documentation](docs/API_DOCUMENTATION.md): endpoint, quyền truy cập, request và response.
- [API Checklist](docs/API_CHECKLIST.md): trạng thái hoàn thiện API theo module.
- [Unused API Checklist](docs/UNUSED_API_CHECKLIST.md): API chưa được tích hợp vào giao diện.
- [Use cases](docs/use-case.md): tác nhân và các trường hợp sử dụng chính.

## Định hướng phát triển

- Notification khi dời lịch, thanh toán và thay đổi enrollment.
- Kiểm thử tự động cho Security, Attendance, Lesson, Payment và Refund.
- Distributed lock cho scheduler khi triển khai nhiều backend instance.
- Lịch sử đầy đủ cho nhiều lần dời một lesson.
- Quy trình đối soát thanh toán và hoàn tiền ở môi trường production.

---

Language Center Management System hướng tới một nền tảng vận hành nhất quán, minh bạch và có khả năng mở rộng cho toàn bộ hoạt động đào tạo của trung tâm ngoại ngữ.
