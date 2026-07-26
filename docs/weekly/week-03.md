# Week-03

## 1. Thông tin chung

- **Tên đề tài:** Website đăng ký khóa học cho trung tâm ngoại ngữ
- **Thời gian thực hiện:** Tuần 3
- **Công nghệ dự kiến:** ReactJS, Spring Boot và MySQL

## 2. Mục tiêu trong tuần

Trong tuần 3, mục tiêu chính là hoàn thiện phần phân tích nghiệp vụ, chuẩn hóa thiết kế cơ sở dữ liệu và chuẩn bị cấu trúc kỹ thuật để bắt đầu phát triển hệ thống. Các công việc tập trung vào:

- Hoàn thiện đặc tả các use case chính.
- Rà soát luồng nghiệp vụ của từng tác nhân.
- Hoàn thiện thiết kế cơ sở dữ liệu.
- Xác định đầy đủ khóa chính, khóa ngoại và các ràng buộc dữ liệu.

## 3. Công việc đã thực hiện

### 3.1. Hoàn thiện đặc tả các use case chính

Đã rà soát và hoàn thiện đặc tả cho các chức năng quan trọng của hệ thống, bao gồm:

- Đăng ký tài khoản.
- Đăng nhập hệ thống.
- Tra cứu khóa học.
- Đăng ký lớp học.
- Hủy đăng ký lớp học.
- Thanh toán học phí.
- Quản lý học viên.
- Quản lý giảng viên.
- Quản lý khóa học.
- Quản lý lớp học.
- Điểm danh học viên.
- Nhập và xem kết quả học tập.

Mỗi đặc tả use case được trình bày với các nội dung:

- Tên use case.
- Tác nhân tham gia.
- Mô tả.
- Điều kiện tiên quyết.
- Điều kiện sau.
- Luồng sự kiện chính.
- Luồng thay thế.
- Luồng ngoại lệ.

### 3.2. Rà soát nghiệp vụ đăng ký lớp học

Đã xác định lại luồng xử lý chính khi học viên đăng ký lớp:

1. Học viên tìm kiếm và chọn khóa học phù hợp.
2. Hệ thống hiển thị các lớp đang mở thuộc khóa học.
3. Học viên lựa chọn lớp muốn đăng ký.
4. Hệ thống kiểm tra sĩ số còn lại của lớp.
5. Hệ thống kiểm tra lịch học có bị trùng hay không.
6. Hệ thống kiểm tra điều kiện trình độ của học viên.
7. Hệ thống tạo thông tin đăng ký lớp.
8. Học viên lựa chọn hình thức thanh toán.
9. Hệ thống cập nhật trạng thái đăng ký và thanh toán.

Các quan hệ use case chính được xác định:

- **Đăng ký lớp học** bao gồm kiểm tra chỗ trống, kiểm tra trùng lịch và kiểm tra trình độ.
- **Thanh toán học phí** được thực hiện sau khi đăng ký lớp thành công.
- **Hủy đăng ký** chỉ được thực hiện khi đăng ký thỏa mãn điều kiện hủy.

### 3.3. Hoàn thiện thiết kế cơ sở dữ liệu

Đã rà soát và chuẩn hóa các bảng dữ liệu chính:

- `roles`: lưu danh sách vai trò.
- `users`: lưu thông tin tài khoản và thông tin người dùng.
- `languages`: lưu danh mục ngôn ngữ.
- `levels`: lưu danh mục trình độ.
- `courses`: lưu thông tin khóa học.
- `teachers`: lưu thông tin chuyên môn của giảng viên.
- `rooms`: lưu thông tin phòng học.
- `shifts`: lưu thông tin ca học.
- `classes`: lưu thông tin lớp học được mở.
- `enrollments`: lưu thông tin đăng ký lớp của học viên.
- `payments`: lưu thông tin thanh toán học phí.
- `attendance`: lưu kết quả điểm danh.
- `scores`: lưu kết quả học tập.
- `notifications`: lưu thông báo của người dùng.
- `system_logs`: lưu nhật ký hoạt động của hệ thống.

### 3.4. Xác định các mối quan hệ dữ liệu

Đã xác định các quan hệ chính giữa các bảng:

- Một vai trò có thể được gán cho nhiều người dùng.
- Một ngôn ngữ có thể có nhiều khóa học.
- Một trình độ có thể áp dụng cho nhiều khóa học.
- Một khóa học có thể mở nhiều lớp học.
- Một giảng viên có thể giảng dạy nhiều lớp.
- Một phòng học có thể được sử dụng cho nhiều lớp ở các thời điểm khác nhau.
- Một học viên có thể đăng ký nhiều lớp.
- Một lớp có thể có nhiều học viên thông qua bảng `enrollments`.
- Một lượt đăng ký có thể phát sinh thông tin thanh toán.
- Một học viên có nhiều kết quả điểm danh và kết quả học tập.

Đồng thời, đã xác định các khóa ngoại cần thiết để bảo đảm tính toàn vẹn dữ liệu giữa các bảng.

### 3.5. Chuẩn hóa các trường trạng thái

Đã rà soát các trường trạng thái trong hệ thống và lựa chọn lưu trực tiếp trong từng bảng thay vì tạo một bảng trạng thái chung. Một số trạng thái dự kiến:

- Trạng thái tài khoản: hoạt động, bị khóa.
- Trạng thái khóa học: đang hoạt động, ngừng hoạt động.
- Trạng thái lớp học: sắp mở, đang học, đã kết thúc, đã hủy.
- Trạng thái đăng ký: chờ thanh toán, đã xác nhận, đã hủy.
- Trạng thái thanh toán: chưa thanh toán, đã thanh toán, thất bại, đã hoàn tiền.

Cách thiết kế này giúp trạng thái của từng nghiệp vụ rõ ràng và hạn chế việc dùng chung các giá trị không cùng ý nghĩa.

### 3.6. Chuẩn bị cấu trúc dự án

Đã xác định cấu trúc tổng thể của hệ thống:

- **Frontend:** ReactJS, chịu trách nhiệm xây dựng giao diện và gửi yêu cầu đến backend.
- **Backend:** Spring Boot, tổ chức theo các lớp Controller, Service và Repository.
- **Database:** MySQL, lưu trữ dữ liệu của toàn bộ hệ thống.

Các module backend dự kiến gồm:

- Authentication.
- User Management.
- Course Management.
- Class Management.
- Enrollment Management.
- Payment Management.
- Attendance Management.
- Score Management.
- Notification.
- Report.

## 4. Kết quả đạt được

Sau tuần 3, các kết quả đã hoàn thành gồm:

- Hoàn thiện nội dung đặc tả cho các use case chính.
- Làm rõ luồng đăng ký lớp học và thanh toán học phí.
- Hoàn thiện danh sách các bảng dữ liệu cần thiết.
- Xác định mối quan hệ, khóa chính và khóa ngoại giữa các bảng.
- Thống nhất sử dụng một bảng `users` kết hợp với `roles` để quản lý các loại người dùng.
- Loại bỏ các bảng không cần thiết nhằm đơn giản hóa cơ sở dữ liệu.
- Chuẩn bị cơ sở để bắt đầu tạo database.

## 5. Vấn đề gặp phải và hướng giải quyết

### 5.1. Phân biệt khóa học và lớp học

- **Vấn đề:** Dễ nhầm lẫn giữa chức năng đăng ký khóa học và đăng ký lớp học.
- **Hướng giải quyết:** Xác định khóa học là chương trình đào tạo, còn lớp học là một lần mở cụ thể của khóa học, có giảng viên, phòng học, ca học và sĩ số. Học viên sẽ đăng ký vào lớp học.

### 5.2. Thiết kế thông tin nhân viên

- **Vấn đề:** Cân nhắc tạo bảng riêng cho Admin và nhân viên tư vấn.
- **Hướng giải quyết:** Sử dụng bảng `users` kết hợp với `roles`. Admin và Consultant được phân biệt bằng vai trò, không cần tạo bảng nhân viên riêng nếu chưa có thuộc tính nghiệp vụ đặc thù.

### 5.3. Quản lý trạng thái

- **Vấn đề:** Cân nhắc tạo một bảng trạng thái dùng chung cho toàn hệ thống.
- **Hướng giải quyết:** Mỗi bảng sử dụng nhóm trạng thái riêng vì trạng thái tài khoản, lớp học, đăng ký và thanh toán có ý nghĩa nghiệp vụ khác nhau.

## 7. Đánh giá tiến độ

Tiến độ tuần 3 cơ bản đáp ứng mục tiêu đề ra. Phần phân tích nghiệp vụ và thiết kế dữ liệu đã tương đối hoàn chỉnh, giúp giảm nguy cơ thay đổi lớn khi bắt đầu lập trình. Trong tuần 4, trọng tâm sẽ chuyển từ giai đoạn phân tích, thiết kế sang giai đoạn triển khai cơ sở dữ liệu và xây dựng các chức năng backend đầu tiên.
