# Sơ đồ tuần tự hệ thống

Tài liệu mô tả thứ tự trao đổi thông điệp giữa tác nhân, giao diện, Backend, các tầng xử lý và cơ sở dữ liệu trong bốn use case chính của hệ thống.

## UC01. Đăng ký lớp học

### Vị trí chèn sơ đồ

<!-- Dán ảnh sơ đồ tuần tự UC01 tại đây. -->

### Mô tả luồng xử lý

1. Học viên truy cập danh sách khóa học trên Web Client.
2. Web Client gửi yêu cầu đến Controller để lấy các khóa học đang hoạt động.
3. Học viên chọn một khóa học; Web Client tiếp tục yêu cầu danh sách các lớp đang mở thuộc khóa học đó.
4. Học viên chọn lớp muốn đăng ký. Backend kiểm tra trạng thái tài khoản, trạng thái lớp, sĩ số, đăng ký trùng và xung đột lịch học.
5. Web Client hiển thị thông tin lớp, lịch học, giảng viên, học phí và số chỗ còn lại.
6. Khi học viên xác nhận đăng ký, Controller chuyển yêu cầu đến Service.
7. Service khóa bản ghi lớp trong transaction và kiểm tra lại các điều kiện đăng ký trước khi ghi dữ liệu.
8. Nếu hợp lệ, Service tạo enrollment ở trạng thái `CONFIRMED`. Lớp có học phí được đặt `paymentStatus = PENDING`; lớp miễn phí được đặt `paymentStatus = PAID`.
9. Repository lưu enrollment vào MySQL và kết quả được trả về Web Client.
10. Web Client hiển thị thông báo đăng ký lớp học thành công.

Nếu lớp vừa đủ sĩ số, enrollment bị trùng hoặc dữ liệu không hợp lệ, Service từ chối yêu cầu và Web Client hiển thị nguyên nhân.

## UC02. Thanh toán học phí

### Vị trí chèn sơ đồ

<!-- Dán ảnh sơ đồ tuần tự UC02 tại đây. -->

### Mô tả luồng xử lý

1. Học viên mở danh sách lớp đã đăng ký và chọn enrollment cần thanh toán.
2. Web Client gửi yêu cầu đến Controller. Service kiểm tra quyền sở hữu, thời hạn, trạng thái enrollment và trạng thái thanh toán.
3. Nếu enrollment ở trạng thái `CONFIRMED`, còn hạn, chưa thanh toán và có học phí lớn hơn 0, học viên chọn MoMo hoặc ZaloPay.
4. Service tạo payment ở trạng thái `PENDING`, sau đó gọi cổng thanh toán tương ứng để tạo yêu cầu giao dịch.
5. Backend trả URL thanh toán cho Web Client để chuyển học viên đến cổng thanh toán.
6. Cổng thanh toán xử lý giao dịch và gửi callback về Backend.
7. Controller tiếp nhận callback; Service xác minh chữ ký, mã giao dịch và số tiền thanh toán.
8. Nếu callback hợp lệ và số tiền khớp, Service cập nhật payment thành `PAID` và cập nhật `paymentStatus` của enrollment thành `PAID`. `enrollmentStatus` vẫn giữ nguyên `CONFIRMED`.
9. Sau khi transaction commit, Backend phát sự kiện gửi email xác nhận thanh toán và xử lý email bất đồng bộ.
10. Backend trả kết quả để Web Client hiển thị thông báo thanh toán thành công.

Nếu giao dịch thất bại, payment chuyển thành `FAILED`, còn enrollment tiếp tục ở trạng thái `CONFIRMED` và `paymentStatus = PENDING` để học viên có thể thử lại trong thời hạn. Callback sai chữ ký hoặc sai số tiền không được phép cập nhật trạng thái thanh toán.

## UC03. Điểm danh học viên

### Vị trí chèn sơ đồ

<!-- Dán ảnh sơ đồ tuần tự UC03 tại đây. -->

### Mô tả luồng xử lý

1. Giảng viên truy cập chức năng quản lý điểm danh.
2. Web Client yêu cầu danh sách các lớp do giảng viên phụ trách.
3. Controller chuyển yêu cầu đến Service; Service kiểm tra quyền phụ trách lớp.
4. Giảng viên chọn lớp và buổi học cần điểm danh.
5. Service kiểm tra lesson tồn tại, đã đến giờ bắt đầu, chưa bị hủy và chưa quá thời hạn chỉnh sửa điểm danh.
6. Repository lấy danh sách học viên có enrollment `CONFIRMED` và `paymentStatus = PAID`, cùng các bản ghi attendance đã có.
7. Web Client hiển thị danh sách học viên để giảng viên chọn trạng thái `PRESENT`, `ABSENT`, `LATE` hoặc `EXCUSED` và nhập ghi chú nếu cần.
8. Khi giảng viên lưu, Controller nhận danh sách điểm danh và chuyển đến Service.
9. Service kiểm tra danh sách không rỗng, không có học viên trùng, chỉ gồm học viên hợp lệ trong lớp và thực hiện tạo mới hoặc cập nhật attendance trong transaction.
10. Repository lưu kết quả vào MySQL; Backend trả thông báo điểm danh thành công cho Web Client.

Nếu giảng viên không phụ trách lớp, lesson không hợp lệ hoặc dữ liệu gửi lên vi phạm điều kiện, toàn bộ yêu cầu bị từ chối và dữ liệu điểm danh cũ được giữ nguyên.

## UC04. Quản lý khóa học

### Vị trí chèn sơ đồ

<!-- Dán ảnh sơ đồ tuần tự UC04 tại đây. -->

### Mô tả luồng xử lý

1. Quản trị viên truy cập chức năng quản lý khóa học.
2. Web Client gửi yêu cầu đến Controller để lấy danh sách khóa học từ MySQL.
3. Quản trị viên có thể tìm kiếm, lọc, phân trang, xem chi tiết, thêm mới, cập nhật hoặc xóa khóa học.
4. Với thao tác thêm hoặc cập nhật, Web Client hiển thị biểu mẫu và gửi dữ liệu khóa học đến Controller.
5. Validation kiểm tra các trường bắt buộc, mã khóa học, slug, học phí, thời lượng và trạng thái.
6. Service kiểm tra thêm ngôn ngữ, trình độ, trạng thái hoạt động, trạng thái xuất bản và các ràng buộc liên quan.
7. Nếu dữ liệu hợp lệ, Service gọi Repository để thêm mới hoặc cập nhật khóa học trong MySQL.
8. Với thao tác xóa, Service kiểm tra khóa học chưa có lớp liên quan trước khi xóa.
9. Backend trả kết quả xử lý để Web Client hiển thị thông báo thành công hoặc nguyên nhân từ chối.

Nếu mã hoặc slug đã tồn tại, dữ liệu bắt buộc bị thiếu, trình độ không hợp lệ hoặc khóa học đã có lớp, thao tác không được thực hiện và dữ liệu cũ được giữ nguyên.