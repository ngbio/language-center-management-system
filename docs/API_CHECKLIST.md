# API Development Checklist

Cập nhật: **05/09/2026**

| Ký hiệu | Trạng thái |
|---|---|
| ✅ | API đã có và chức năng chính đã hoàn thành |
| 🟡 | API đã có nhưng còn điểm cần sửa hoặc hoàn thiện |
| ⬜ | API chưa có hoặc mới được đề xuất |

> Trạng thái trong file này đánh giá theo code hiện tại. Chi tiết request và response xem tại [API_DOCUMENTATION.md](API_DOCUMENTATION.md).

## Authentication

| Trạng thái | Method | API | Chức năng/Ghi chú |
|:---:|---|---|---|
| ✅ | POST | `/api/auth/login` | Đăng nhập chung Student và Teacher |
| ✅ | POST | `/api/admin/auth/login` | Đăng nhập Admin |
| ✅ | POST | `/api/auth/register` | Đăng ký tài khoản Student |
| ✅ | POST | `/api/auth/teacher/register` | Teacher tự đăng ký, tài khoản `INACTIVE` chờ Admin kích hoạt |
| ✅ | GET | `/api/auth/me` | Xác thực token và lấy người dùng hiện tại |
| ⬜ | POST | `/api/auth/logout` | Chưa cần thiết với JWT stateless; frontend tự xóa token |
| ⬜ | POST | `/api/auth/refresh-token` | Chưa có refresh token |
| ⬜ | POST | `/api/auth/forgot-password` | Chưa có quên mật khẩu |
| ⬜ | POST | `/api/auth/reset-password` | Chưa có đặt lại mật khẩu |
| ⬜ | PUT | `/api/auth/change-password` | Chưa có đổi mật khẩu |

## Khóa học và nội dung Public

| Trạng thái | Method | API | Chức năng/Ghi chú |
|:---:|---|---|---|
| ✅ | GET | `/api/courses` | Danh sách khóa học `ACTIVE + PUBLISHED`, có phân trang |
| ✅ | GET | `/api/courses/slug/{slug}` | Chi tiết khóa học theo slug |
| ✅ | GET | `/api/courses/{id}/sections` | Danh sách section, chưa tải content |
| ✅ | GET | `/api/sections/{id}/contents` | Content theo free/preview hoặc quyền `CONFIRMED + PAID` |
| ⬜ | GET | `/api/courses/{id}/reviews` | Chưa có đánh giá khóa học |
| ⬜ | POST | `/api/courses/{id}/reviews` | Chưa có gửi đánh giá |
| ⬜ | GET | `/api/courses/{id}/related` | Chưa có khóa học liên quan |

## Danh mục Public

| Trạng thái | Method | API | Chức năng/Ghi chú |
|:---:|---|---|---|
| ✅ | GET | `/api/languages` | Danh sách ngôn ngữ đang hoạt động |
| ✅ | GET | `/api/languages/{id}` | Chi tiết ngôn ngữ đang hoạt động |
| ✅ | GET | `/api/languages/{id}/levels` | Trình độ theo ngôn ngữ |
| ✅ | GET | `/api/levels` | Danh sách trình độ, có lọc `languageId` |
| ✅ | GET | `/api/levels/{id}` | Chi tiết trình độ đang hoạt động |
| ✅ | GET | `/api/rooms` | Danh sách phòng |
| ✅ | GET | `/api/rooms/{id}` | Chi tiết phòng |
| ✅ | POST | `/api/rooms` | Tạo phòng, yêu cầu Admin |
| ✅ | PUT | `/api/rooms/{id}` | Cập nhật phòng, yêu cầu Admin |
| ✅ | DELETE | `/api/rooms/{id}` | Xóa phòng, yêu cầu Admin |

## Lớp học Public

| Trạng thái | Method | API | Chức năng/Ghi chú |
|:---:|---|---|---|
| 🟡 | GET | `/api/classes` | Đã có; cần lọc thêm Course `ACTIVE + PUBLISHED` |
| 🟡 | GET | `/api/classes/{id}` | Đã có; cần kiểm tra Course `ACTIVE + PUBLISHED` |
| ✅ | GET | `/api/classes/{classId}/schedules` | Lấy lịch cố định của lớp |

## Student

| Trạng thái | Method | API | Chức năng/Ghi chú |
|:---:|---|---|---|
| ✅ | POST | `/api/enrollments` | Student đăng ký lớp |
| ✅ | GET | `/api/students/me/enrollments` | Lịch sử đăng ký của Student |
| ✅ | GET | `/api/students/me/courses` | Khóa học đã `CONFIRMED + PAID` |
| ✅ | GET | `/api/students/me/classes` | Lớp học đã `CONFIRMED + PAID` và chưa bị hủy |
| ✅ | GET | `/api/students/me/schedules` | Thời khóa biểu của các lớp đã kích hoạt |
| ✅ | POST | `/api/enrollments/{id}/cancel-request` | Hủy enrollment của chính Student |
| ✅ | GET | `/api/classes/{classId}/lessons` | Student phải có enrollment `CONFIRMED + PAID` đúng lớp |
| ⬜ | GET | `/api/students/me/profile` | Có thể dùng `/auth/me`, nhưng chưa có DTO hồ sơ Student đầy đủ |
| ⬜ | PUT | `/api/students/me/profile` | Chưa có cập nhật thông tin cá nhân |
| ⬜ | GET | `/api/students/me/courses/{courseId}/progress` | Chưa theo dõi tiến độ học |
| ⬜ | PATCH | `/api/students/me/contents/{contentId}/complete` | Chưa đánh dấu bài đã học |
| ⬜ | GET | `/api/students/me/payments` | Chưa có lịch sử thanh toán |
| ⬜ | GET | `/api/students/me/certificates` | Chưa có chứng chỉ của Student |

## Teacher

| Trạng thái | Method | API | Chức năng/Ghi chú |
|:---:|---|---|---|
| ✅ | GET | `/api/teachers/me/classes` | Các lớp được phân công |
| ✅ | GET | `/api/classes/{classId}/enrollments` | Danh sách học viên; service kiểm tra giáo viên phụ trách |
| ✅ | GET | `/api/classes/{classId}/lessons` | Danh sách buổi học của lớp phụ trách |
| ✅ | PUT | `/api/lessons/{id}` | Cập nhật chủ đề và link học |
| ⬜ | GET | `/api/teachers/me/profile` | Chưa có hồ sơ Teacher riêng |
| ⬜ | PUT | `/api/teachers/me/profile` | Chưa có cập nhật hồ sơ Teacher |
| ⬜ | POST | `/api/lessons/{id}/attendance` | Chưa có API điểm danh |
| ⬜ | PUT | `/api/lessons/{id}/attendance` | Chưa có cập nhật điểm danh |

## Staff quản lý Enrollment

| Trạng thái | Method | API | Chức năng/Ghi chú |
|:---:|---|---|---|
| ✅ | POST | `/api/staff/enrollments` | Admin/Consultant xếp lớp cho Student |
| ✅ | GET | `/api/classes/{id}/enrollments` | Admin/Consultant/Teacher xem enrollment của lớp |
| ✅ | PATCH | `/api/staff/enrollments/{id}/status` | Chuyển `PENDING`, `CONFIRMED`, `CANCELLED` |
| ✅ | POST | `/api/staff/enrollments/{id}/transfer` | Chuyển enrollment chưa thanh toán sang lớp khác |
| ⬜ | PATCH | `/api/staff/enrollments/{id}/payment-status` | Chưa có cập nhật trạng thái thanh toán |
| ⬜ | GET | `/api/staff/enrollments` | Chưa có tìm kiếm tất cả enrollment có phân trang |
| ⬜ | GET | `/api/staff/enrollments/{id}` | Chưa có API chi tiết riêng theo enrollment ID |

## Lịch học và buổi học

| Trạng thái | Method | API | Chức năng/Ghi chú |
|:---:|---|---|---|
| ✅ | POST | `/api/classes/{classId}/schedules` | Tạo lịch học |
| ✅ | PUT | `/api/schedules/{id}` | Cập nhật lịch học |
| ✅ | DELETE | `/api/schedules/{id}` | Xóa lịch chưa sinh buổi học |
| ✅ | POST | `/api/classes/{classId}/lessons/generate` | Sinh buổi học từ schedule |
| ✅ | GET | `/api/classes/{classId}/lessons` | API dùng chung; Student yêu cầu `CONFIRMED + PAID` |
| ✅ | PUT | `/api/lessons/{id}` | Cập nhật nội dung buổi học |
| ✅ | PATCH | `/api/lessons/{id}/reschedule` | Dời ngày học |
| ✅ | PATCH | `/api/lessons/{id}/cancel` | Hủy buổi học |
| ⬜ | GET | `/api/lessons/{id}` | Chưa có API lấy riêng chi tiết một buổi học |
| ⬜ | PATCH | `/api/lessons/{id}/status` | Chưa quản lý đầy đủ trạng thái buổi học |

## Admin quản lý người dùng

| Trạng thái | Method | API | Chức năng/Ghi chú |
|:---:|---|---|---|
| ✅ | POST | `/api/admin/teachers` | Tạo tài khoản và hồ sơ Teacher |
| ✅ | GET | `/api/admin/teachers` | Danh sách Teacher đang hoạt động |
| ✅ | GET | `/api/admin/users` | Danh sách người dùng có tìm kiếm và phân trang |
| ✅ | GET | `/api/admin/users/{id}` | Chi tiết người dùng |
| ✅ | PATCH | `/api/admin/users/{id}/status` | Đổi trạng thái tài khoản |
| ⬜ | PUT | `/api/admin/users/{id}` | Chưa có cập nhật thông tin người dùng |
| ⬜ | DELETE | `/api/admin/users/{id}` | Chưa có xóa người dùng; nên ưu tiên khóa tài khoản |

## Admin quản lý khóa học

| Trạng thái | Method | API | Chức năng/Ghi chú |
|:---:|---|---|---|
| ✅ | GET | `/api/admin/courses` | Danh sách khóa học có tìm kiếm và phân trang |
| ✅ | GET | `/api/admin/courses/{id}` | Chi tiết khóa học, gồm khóa nháp |
| ✅ | POST | `/api/admin/courses` | Tạo khóa học |
| ✅ | PUT | `/api/admin/courses/{id}` | Cập nhật khóa học |
| ✅ | DELETE | `/api/admin/courses/{id}` | Xóa khóa chưa có lớp |
| ⬜ | POST | `/api/admin/courses/{courseId}/sections` | Chưa có API tạo section |
| ⬜ | PUT | `/api/admin/sections/{id}` | Chưa có API cập nhật section |
| ⬜ | DELETE | `/api/admin/sections/{id}` | Chưa có API xóa section |
| ⬜ | PATCH | `/api/admin/sections/reorder` | Chưa có sắp xếp section |
| ⬜ | POST | `/api/admin/sections/{sectionId}/contents` | Chưa có API tạo content |
| ⬜ | PUT | `/api/admin/contents/{id}` | Chưa có API cập nhật content |
| ⬜ | DELETE | `/api/admin/contents/{id}` | Chưa có API xóa content |
| ⬜ | PATCH | `/api/admin/contents/{id}/publication-status` | Chưa có API publish/unpublish content |
| ⬜ | PATCH | `/api/admin/contents/reorder` | Chưa có sắp xếp content |

## Admin/Consultant quản lý lớp

| Trạng thái | Method | API | Chức năng/Ghi chú |
|:---:|---|---|---|
| ✅ | GET | `/api/admin/classes` | Danh sách lớp có tìm kiếm và phân trang |
| ✅ | POST | `/api/admin/classes` | Tạo lớp ở trạng thái `DRAFT` |
| ✅ | PUT | `/api/admin/classes/{id}` | Cập nhật lớp |
| ✅ | PATCH | `/api/admin/classes/{id}/teacher` | Phân công Teacher |
| 🟡 | PATCH | `/api/admin/classes/{id}/status` | Đã có; khi mở lớp chưa kiểm tra Course `PUBLISHED` |
| ⬜ | GET | `/api/admin/classes/{id}` | Chưa có endpoint Admin lấy riêng một lớp |
| ⬜ | DELETE | `/api/admin/classes/{id}` | Chưa có xóa lớp Draft |

## Admin quản lý ngôn ngữ

| Trạng thái | Method | API | Chức năng/Ghi chú |
|:---:|---|---|---|
| ✅ | GET | `/api/admin/languages` | Danh sách, có lọc trạng thái |
| ✅ | GET | `/api/admin/languages/{id}` | Chi tiết ngôn ngữ |
| ✅ | POST | `/api/admin/languages` | Tạo ngôn ngữ |
| ✅ | PUT | `/api/admin/languages/{id}` | Cập nhật ngôn ngữ |
| ✅ | PATCH | `/api/admin/languages/{id}/status` | Đổi trạng thái |
| ✅ | DELETE | `/api/admin/languages/{id}` | Xóa khi chưa có trình độ |

## Admin quản lý trình độ

| Trạng thái | Method | API | Chức năng/Ghi chú |
|:---:|---|---|---|
| ✅ | GET | `/api/admin/levels` | Danh sách, lọc theo ngôn ngữ/trạng thái |
| ✅ | GET | `/api/admin/levels/{id}` | Chi tiết trình độ |
| ✅ | POST | `/api/admin/levels` | Tạo trình độ |
| ✅ | PUT | `/api/admin/levels/{id}` | Cập nhật trình độ |
| ✅ | PATCH | `/api/admin/levels/{id}/status` | Đổi trạng thái |
| ✅ | DELETE | `/api/admin/levels/{id}` | Xóa khi chưa có khóa học |

## Admin quản lý phòng

| Trạng thái | Method | API | Chức năng/Ghi chú |
|:---:|---|---|---|
| ✅ | GET | `/api/admin/rooms` | Danh sách, có lọc trạng thái |
| ✅ | GET | `/api/admin/rooms/{id}` | Chi tiết phòng |
| ✅ | POST | `/api/admin/rooms` | Tạo phòng |
| ✅ | PUT | `/api/admin/rooms/{id}` | Cập nhật phòng |
| ✅ | DELETE | `/api/admin/rooms/{id}` | Xóa khi chưa có lịch học |

## Thanh toán — chưa triển khai

| Trạng thái | Method | API đề xuất | Chức năng/Ghi chú |
|:---:|---|---|---|
| ⬜ | POST | `/api/enrollments/{id}/payments` | Tạo giao dịch thanh toán |
| ⬜ | GET | `/api/enrollments/{id}/payments` | Lịch sử thanh toán của enrollment |
| ⬜ | POST | `/api/payments/{id}/confirm` | Xác nhận thanh toán thủ công |
| ⬜ | POST | `/api/payments/callback` | Nhận callback từ cổng thanh toán |
| ⬜ | POST | `/api/payments/{id}/refund` | Hoàn tiền |

## Các việc nên ưu tiên tiếp theo

| Ưu tiên | Trạng thái | Công việc |
|:---:|:---:|---|
| 1 | ✅ | Quyền xem lesson của Student yêu cầu enrollment `CONFIRMED + PAID` |
| 2 | 🟡 | Lọc lớp Public theo Course `ACTIVE + PUBLISHED` |
| 3 | 🟡 | Khi mở lớp phải kiểm tra Course đã `PUBLISHED` |
| 4 | ⬜ | Thêm CRUD cho `course_section` và `course_content` |
| 5 | ⬜ | Xây dựng API payment để thay đổi `paymentStatus` hợp lệ |
| 6 | ⬜ | Thêm API cập nhật profile Student/Teacher |
| 7 | ⬜ | Thêm API điểm danh và tiến độ học tập |
