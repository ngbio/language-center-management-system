# Sơ đồ hoạt động hệ thống

Tài liệu mô tả luồng công việc, các bước xử lý, điều kiện rẽ nhánh và trách nhiệm của actor, giao diện và hệ thống trong bốn use case chính.

## UC01. Đăng ký lớp học

### Vị trí chèn sơ đồ

<!-- Dán ảnh sơ đồ hoạt động UC01 tại đây. -->

### Mô tả luồng xử lý

1. Quy trình bắt đầu khi học viên chọn chức năng đăng ký lớp.
2. Hệ thống lấy và hiển thị danh sách các khóa học đang hoạt động.
3. Học viên chọn khóa học; hệ thống hiển thị các lớp đang mở thuộc khóa học.
4. Học viên chọn lớp. Hệ thống kiểm tra đăng nhập, trạng thái tài khoản, trạng thái lớp, sĩ số, đăng ký trùng và xung đột lịch học.
5. Nếu không đủ điều kiện, giao diện hiển thị nguyên nhân và học viên lựa chọn lại.
6. Nếu đủ điều kiện, giao diện hiển thị thông tin lớp để học viên xác nhận.
7. Hệ thống kiểm tra lại sĩ số và các điều kiện trong transaction.
8. Nếu lớp đã đủ sĩ số, giao diện thông báo lớp hết chỗ.
9. Nếu còn chỗ, hệ thống tạo enrollment và thiết lập trạng thái thanh toán theo học phí của lớp.
10. Giao diện thông báo đăng ký thành công và quy trình kết thúc.

## UC02. Thanh toán học phí

### Vị trí chèn sơ đồ

<!-- Dán ảnh sơ đồ hoạt động UC02 tại đây. -->

### Mô tả luồng xử lý

1. Quy trình bắt đầu khi học viên đăng nhập và chọn enrollment cần thanh toán.
2. Hệ thống kiểm tra enrollment có tồn tại, thuộc học viên hiện tại, còn hạn, ở trạng thái `CONFIRMED` và chưa thanh toán hay không.
3. Nếu enrollment không hợp lệ hoặc đã thanh toán, hệ thống dừng quy trình và hiển thị thông báo phù hợp.
4. Nếu hợp lệ, giao diện hiển thị học phí và các phương thức MoMo, ZaloPay.
5. Học viên chọn phương thức, kiểm tra thông tin và xác nhận thanh toán.
6. Hệ thống tạo payment ở trạng thái `PENDING` và chuyển học viên đến cổng thanh toán.
7. Cổng thanh toán trả kết quả về Backend.
8. Backend xác minh callback, chữ ký và số tiền.
9. Nếu giao dịch thất bại, payment chuyển thành `FAILED`; enrollment vẫn chờ thanh toán trong thời hạn.
10. Nếu callback hợp lệ và số tiền khớp, hệ thống cập nhật payment và `paymentStatus` của enrollment thành `PAID`.
11. Sau khi transaction commit, hệ thống phát sự kiện gửi email xác nhận thanh toán bất đồng bộ.
12. Giao diện thông báo thanh toán thành công và quy trình kết thúc.

## UC03. Điểm danh học viên

### Vị trí chèn sơ đồ

<!-- Dán ảnh sơ đồ hoạt động UC03 tại đây. -->

### Mô tả luồng xử lý

1. Quy trình bắt đầu khi giảng viên chọn chức năng quản lý điểm danh.
2. Hệ thống lấy và hiển thị các lớp do giảng viên phụ trách.
3. Giảng viên chọn lớp và buổi học cần điểm danh.
4. Hệ thống kiểm tra quyền giảng viên, sự tồn tại của lesson, trạng thái hủy, thời điểm bắt đầu và thời hạn chỉnh sửa.
5. Nếu điều kiện không hợp lệ, giao diện từ chối thao tác và hiển thị lý do.
6. Nếu hợp lệ, hệ thống lấy danh sách học viên có enrollment `CONFIRMED` và `paymentStatus = PAID`, cùng kết quả điểm danh hiện có.
7. Giảng viên chọn trạng thái điểm danh và có thể nhập ghi chú.
8. Khi giảng viên nhấn lưu, hệ thống kiểm tra danh sách không rỗng, không trùng và chỉ chứa học viên hợp lệ của lớp.
9. Nếu dữ liệu không hợp lệ, hệ thống từ chối toàn bộ yêu cầu và giữ dữ liệu cũ.
10. Nếu dữ liệu hợp lệ, hệ thống tạo mới hoặc cập nhật attendance trong transaction.
11. Giao diện thông báo điểm danh thành công và quy trình kết thúc.

## UC04. Quản lý khóa học

### Vị trí chèn sơ đồ

<!-- Dán ảnh sơ đồ hoạt động UC04 tại đây. -->

### Mô tả luồng xử lý

1. Quy trình bắt đầu khi quản trị viên truy cập chức năng quản lý khóa học.
2. Hệ thống truy vấn và hiển thị danh sách khóa học.
3. Quản trị viên có thể tìm kiếm, lọc, phân trang, xem chi tiết, thêm mới, cập nhật hoặc xóa khóa học.
4. Với thao tác xem chi tiết, hệ thống hiển thị dữ liệu và kết thúc mà không thay đổi dữ liệu.
5. Với thao tác thêm mới hoặc cập nhật, hệ thống hiển thị biểu mẫu để quản trị viên nhập thông tin.
6. Quản trị viên xác nhận lưu; hệ thống kiểm tra các trường bắt buộc, mã, slug, ngôn ngữ, trình độ, học phí và trạng thái.
7. Nếu dữ liệu chưa hợp lệ, giao diện hiển thị lỗi để quản trị viên bổ sung hoặc sửa lại.
8. Nếu dữ liệu hợp lệ, hệ thống lưu khóa học vào MySQL và hiển thị thông báo thành công.
9. Với thao tác xóa, hệ thống kiểm tra khóa học có lớp liên quan hay không.
10. Nếu đã có lớp, hệ thống từ chối xóa và hiển thị nguyên nhân.
11. Nếu chưa có lớp, hệ thống xóa khóa học sau khi quản trị viên xác nhận.
12. Quy trình kết thúc sau khi giao diện hiển thị kết quả thao tác.