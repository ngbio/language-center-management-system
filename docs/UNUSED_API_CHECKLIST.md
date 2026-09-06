# API chưa được tích hợp vào giao diện

Cập nhật: **05/09/2026**

Tài liệu này đối chiếu controller backend với các lời gọi API trong `frontend/src`.

| Ký hiệu | Ý nghĩa |
|:---:|---|
| ⬜ | Chưa có giao diện gọi API |
| 🟡 | Mới dùng một phần hoặc chỉ dùng để lấy số liệu |
| ✅ | Đã được giao diện sử dụng đầy đủ |

## 1. Authentication và tài khoản

| Trạng thái | Method | API | Hiện trạng / giao diện còn thiếu |
|:---:|---|---|---|
| ✅ | POST | `/api/auth/register` | Đã có màn hình tự đăng ký Student tại `/register` |
| ✅ | POST | `/api/auth/teacher/register` | Teacher tự đăng ký ở trạng thái `INACTIVE`, chờ Admin kích hoạt |
| ✅ | POST | `/api/auth/login` | Đã dùng tại trang đăng nhập Student/Teacher |
| ✅ | POST | `/api/admin/auth/login` | Đã dùng tại trang đăng nhập Admin |
| ✅ | GET | `/api/auth/me` | Đã dùng để xác thực quyền khi vào Admin |
| 🟡 | GET | `/api/auth/me` | Chưa có trang hiển thị hồ sơ cá nhân; hiện chỉ dùng ngầm để kiểm tra token |

## 2. Khóa học Public

| Trạng thái | Method | API | Hiện trạng / giao diện còn thiếu |
|:---:|---|---|---|
| ✅ | GET | `/api/courses` | Trang `/khoa-hoc`, Dashboard và danh sách khóa miễn phí đang sử dụng |
| ✅ | GET | `/api/courses/slug/{slug}` | Đã dùng tại trang chi tiết khóa học |
| ✅ | GET | `/api/courses/{id}/sections` | Đã hiển thị curriculum trong trang chi tiết |
| ✅ | GET | `/api/sections/{id}/contents` | Đã tải content khi mở section |

## 3. Danh mục Public

| Trạng thái | Method | API | Hiện trạng / giao diện còn thiếu |
|:---:|---|---|---|
| ✅ | GET | `/api/languages` | Trang `/ngon-ngu`, bộ lọc khóa học và Admin đang dùng |
| ⬜ | GET | `/api/languages/{id}` | Chưa có trang chi tiết ngôn ngữ Public |
| ✅ | GET | `/api/languages/{id}/levels` | Bộ lọc khóa học Public gọi khi người dùng chọn ngôn ngữ |
| ✅ | GET | `/api/levels` | Dashboard và form khóa học đang dùng |
| ✅ | GET | `/api/teachers` | Đã dùng ở khu vực “Đội ngũ giảng viên” trên trang chủ |
| ⬜ | GET | `/api/levels/{id}` | Chưa có trang chi tiết trình độ Public |
| ✅ | GET | `/api/rooms` | Dashboard đang dùng để lấy tổng số phòng |
| ⬜ | GET | `/api/rooms/{id}` | Chưa có giao diện chi tiết phòng Public |
| ⬜ | POST | `/api/rooms` | Chưa dùng; giao diện Admin dùng `/api/admin/rooms` |
| ⬜ | PUT | `/api/rooms/{id}` | Chưa dùng; giao diện Admin dùng `/api/admin/rooms/{id}` |
| ⬜ | DELETE | `/api/rooms/{id}` | Chưa dùng; giao diện Admin dùng endpoint Admin |

## 4. Lớp học Public

| Trạng thái | Method | API | Hiện trạng / giao diện còn thiếu |
|:---:|---|---|---|
| ✅ | GET | `/api/classes` | Trang `/lop-hoc` có tìm kiếm, lọc khóa học, trình độ, ngày khai giảng và sắp xếp |
| ⬜ | GET | `/api/classes/{id}` | Chưa có trang chi tiết lớp và chọn lớp để đăng ký |
| ⬜ | GET | `/api/classes/{classId}/schedules` | Chưa gọi trực tiếp; Student đang dùng API thời khóa biểu tổng hợp |

## 5. Student

| Trạng thái | Method | API | Hiện trạng / giao diện còn thiếu |
|:---:|---|---|---|
| ✅ | POST | `/api/enrollments` | Đã có luồng chọn lớp và xác nhận đăng ký từ trang chi tiết khóa học |
| ✅ | GET | `/api/students/me/enrollments` | Đã dùng tại trang riêng “Lịch sử đăng ký & thanh toán” (`/lich-su-dang-ky`) |
| ✅ | GET | `/api/students/me/courses` | Đã dùng ở nhóm “Khóa học đã mua” |
| ✅ | GET | `/api/students/me/classes` | Đã dùng ở tab “Lớp học” |
| ✅ | GET | `/api/students/me/schedules` | Đã dùng ở tab “Thời khóa biểu” |
| ✅ | GET | `/api/students/me/profile` | Đã dùng tại trang “Thông tin cá nhân” của Student |
| ✅ | PUT | `/api/students/me/profile` | Đã dùng để cập nhật hồ sơ Student |
| ✅ | POST | `/api/payments` | Đã dùng tại trang lịch sử; enrollment giữ chỗ ngay và có hạn thanh toán 48 giờ |
| ✅ | GET | `/api/students/me/payments` | Đã dùng tại trang kết quả thanh toán |
| ⬜ | GET | `/api/enrollments/{id}/payments` | Backend đã có; chưa có màn chi tiết mọi lần thử thanh toán |
| ⬜ | GET | `/api/payments/{transactionCode}` | Backend đã có; chưa có ô tra cứu giao dịch |
| ✅ | POST | `/api/staff/enrollments/{id}/refunds` | Đã có nút hoàn toàn bộ học phí trong màn quản lý enrollment |
| ⬜ | GET | `/api/enrollments/{id}/refunds` | Backend đã có; chưa hiển thị lịch sử hoàn tiền chi tiết |
| ⬜ | GET | `/api/enrollments/{id}/invoice` | Backend đã có JSON; frontend hiện dùng endpoint PDF |
| ✅ | GET | `/api/enrollments/{id}/invoice.pdf` | Nút “Tải hóa đơn PDF” đã tích hợp trong lịch sử đăng ký và thanh toán của Student |
| ✅ | POST | `/api/enrollments/{id}/cancel-request` | Đã có nút yêu cầu hủy tại trang lịch sử riêng |
| ⬜ | GET | `/api/classes/{classId}/lessons` | Chưa có màn hình danh sách buổi học cho Student |

## 6. Teacher

Hiện frontend chưa có workspace hoặc route riêng dành cho Teacher.

| Trạng thái | Method | API | Giao diện cần bổ sung |
|:---:|---|---|---|
| ✅ | GET | `/api/teachers/me/classes` | Đã dùng tại trang lớp học và thời khóa biểu Teacher |
| ✅ | GET | `/api/teachers/me/courses` | Đã dùng tại trang khóa học phụ trách của Teacher |
| ✅ | GET | `/api/teachers/me/profile` | Đã dùng tại trang thông tin cá nhân Teacher |
| ✅ | PUT | `/api/teachers/me/profile` | Đã dùng để cập nhật hồ sơ chuyên môn Teacher |
| ⬜ | GET | `/api/classes/{id}/enrollments` | Danh sách học viên trong lớp của Teacher |
| ⬜ | GET | `/api/classes/{classId}/lessons` | Lịch và danh sách buổi dạy |
| ⬜ | PUT | `/api/lessons/{id}` | Form cập nhật chủ đề và meeting URL |

## 7. Staff quản lý enrollment

Đã có màn hình quản lý enrollment tại `/admin/enrollments` dành cho Admin.

| Trạng thái | Method | API | Giao diện cần bổ sung |
|:---:|---|---|---|
| ✅ | POST | `/api/staff/enrollments` | Form Staff/Admin lọc theo khóa học, chọn lớp, tìm Student bằng email và xếp lớp |
| ✅ | GET | `/api/classes/{id}/enrollments` | Danh sách đăng ký theo lớp trên màn hình Admin |
| ✅ | PATCH | `/api/staff/enrollments/{id}/status` | Giao diện dùng để hủy; không còn nút xác nhận thủ công |
| ✅ | POST | `/api/staff/enrollments/{id}/transfer` | Thao tác chuyển đăng ký sang lớp khác cùng khóa học |

## 8. Quản lý lịch học và buổi học

Backend đã có toàn bộ API cơ bản nhưng giao diện Admin/Consultant chưa tích hợp.

| Trạng thái | Method | API | Giao diện cần bổ sung |
|:---:|---|---|---|
| ⬜ | GET | `/api/classes/{classId}/schedules` | Danh sách lịch trong màn hình quản lý lớp |
| ⬜ | POST | `/api/classes/{classId}/schedules` | Form tạo lịch học |
| ⬜ | PUT | `/api/schedules/{id}` | Form sửa lịch học |
| ⬜ | DELETE | `/api/schedules/{id}` | Nút xóa lịch chưa sinh buổi |
| ⬜ | POST | `/api/classes/{classId}/lessons/generate` | Nút sinh danh sách buổi học |
| ⬜ | GET | `/api/classes/{classId}/lessons` | Danh sách buổi học trong quản lý lớp |
| ⬜ | PUT | `/api/lessons/{id}` | Sửa nội dung buổi học |
| ⬜ | PATCH | `/api/lessons/{id}/reschedule` | Dời ngày học |
| ⬜ | PATCH | `/api/lessons/{id}/cancel` | Hủy buổi học |

## 9. Admin quản lý người dùng

| Trạng thái | Method | API | Hiện trạng / giao diện còn thiếu |
|:---:|---|---|---|
| ⬜ | POST | `/api/admin/teachers` | Chưa có form tạo tài khoản Teacher |
| ✅ | GET | `/api/admin/teachers` | Đã dùng làm lựa chọn phân công giáo viên |
| ✅ | GET | `/api/admin/users` | Đã có danh sách, tìm kiếm và bộ lọc |
| ✅ | GET | `/api/admin/users/{id}` | Đã có modal chi tiết |
| ✅ | PATCH | `/api/admin/users/{id}/status` | Đã có cập nhật trạng thái tài khoản |

## 10. Admin quản lý khóa học

| Trạng thái | Method | API | Hiện trạng / giao diện còn thiếu |
|:---:|---|---|---|
| ✅ | GET | `/api/admin/courses` | Đã có danh sách và bộ lọc |
| ⬜ | GET | `/api/admin/courses/{id}` | Chưa gọi riêng; form sửa dùng dữ liệu có sẵn từ danh sách |
| ✅ | POST | `/api/admin/courses` | Đã có form tạo |
| ✅ | PUT | `/api/admin/courses/{id}` | Đã có form cập nhật |
| ✅ | DELETE | `/api/admin/courses/{id}` | Đã có thao tác xóa |

## 11. Admin/Consultant quản lý lớp

| Trạng thái | Method | API | Hiện trạng / giao diện còn thiếu |
|:---:|---|---|---|
| ✅ | GET | `/api/admin/classes` | Đã có danh sách, phân trang và bộ lọc |
| ✅ | POST | `/api/admin/classes` | Đã có form tạo lớp |
| ⬜ | PUT | `/api/admin/classes/{id}` | Chưa có form sửa toàn bộ thông tin lớp |
| ✅ | PATCH | `/api/admin/classes/{id}/teacher` | Đã có phân công giáo viên |
| ✅ | PATCH | `/api/admin/classes/{id}/status` | Đã có chuyển trạng thái lớp |

## 12. Admin quản lý ngôn ngữ

| Trạng thái | Method | API | Hiện trạng / giao diện còn thiếu |
|:---:|---|---|---|
| ✅ | GET | `/api/admin/languages` | Đã có danh sách và lọc trạng thái |
| ⬜ | GET | `/api/admin/languages/{id}` | Chưa gọi riêng; form sửa dùng dữ liệu từ danh sách |
| ✅ | POST | `/api/admin/languages` | Đã có form tạo |
| ✅ | PUT | `/api/admin/languages/{id}` | Đã có form cập nhật, gồm cả trạng thái |
| ⬜ | PATCH | `/api/admin/languages/{id}/status` | Chưa dùng nút đổi trạng thái riêng |
| ✅ | DELETE | `/api/admin/languages/{id}` | Đã có thao tác xóa |

## 13. Admin quản lý trình độ

| Trạng thái | Method | API | Hiện trạng / giao diện còn thiếu |
|:---:|---|---|---|
| ✅ | GET | `/api/admin/levels` | Đã có danh sách và lọc trạng thái |
| ⬜ | GET | `/api/admin/levels/{id}` | Chưa gọi riêng; form sửa dùng dữ liệu từ danh sách |
| ✅ | POST | `/api/admin/levels` | Đã có form tạo |
| ✅ | PUT | `/api/admin/levels/{id}` | Đã có form cập nhật, gồm cả trạng thái |
| ⬜ | PATCH | `/api/admin/levels/{id}/status` | Chưa dùng nút đổi trạng thái riêng |
| ✅ | DELETE | `/api/admin/levels/{id}` | Đã có thao tác xóa |

## 14. Admin quản lý phòng

| Trạng thái | Method | API | Hiện trạng / giao diện còn thiếu |
|:---:|---|---|---|
| ✅ | GET | `/api/admin/rooms` | Đã có danh sách và lọc trạng thái |
| ⬜ | GET | `/api/admin/rooms/{id}` | Chưa gọi riêng; form sửa dùng dữ liệu từ danh sách |
| ✅ | POST | `/api/admin/rooms` | Đã có form tạo |
| ✅ | PUT | `/api/admin/rooms/{id}` | Đã có form cập nhật |
| ✅ | DELETE | `/api/admin/rooms/{id}` | Đã có thao tác xóa |

## 15. Thứ tự nên tích hợp tiếp

| Ưu tiên | Chức năng | Các API chính |
|:---:|---|---|
| 1 | Hủy đăng ký từ lịch sử | `POST /enrollments/{id}/cancel-request` |
| 3 | Student xem các buổi học | `GET /classes/{classId}/lessons` |
| 4 | Admin quản lý enrollment theo lớp | Các API `/staff/enrollments` và `/classes/{id}/enrollments` |
| 5 | Admin quản lý lịch và sinh buổi học | Các API `/schedules` và `/lessons` |
| 6 | Xây dựng workspace Teacher | `/teachers/me/classes`, enrollment và lesson |
| 7 | Đăng ký tài khoản và hồ sơ cá nhân | `/auth/register`, `/auth/me` |

## Tổng kết

| Loại | Số dòng chức năng |
|---|---:|
| Đã tích hợp đầy đủ | 38 |
| Tích hợp một phần | 2 |
| Chưa tích hợp | 39 |

Một endpoint có thể xuất hiện ở nhiều nhóm quyền hoặc chức năng, vì vậy bảng tổng kết đếm theo **dòng chức năng giao diện**, không phải số endpoint URL duy nhất.
