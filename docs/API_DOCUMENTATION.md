# Language Center Management API

Tài liệu này mô tả các API hiện có trong backend tại thời điểm cập nhật **05/09/2026**.

## 1. Thông tin chung

- Base URL mặc định: `http://localhost:8081/api`
- Content-Type: `application/json`
- Xác thực: JWT Bearer Token
- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

Header dành cho API yêu cầu đăng nhập:

```http
Authorization: Bearer <token>
Content-Type: application/json
```

### Cấu trúc response chung

```json
{
  "status": 200,
  "message": "Thông báo kết quả",
  "data": {}
}
```

Response phân trang:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

### Quyền sử dụng

| Ký hiệu | Ý nghĩa |
|---|---|
| Public | Không cần đăng nhập |
| Authenticated | Mọi tài khoản đã đăng nhập |
| STUDENT | Chỉ học viên |
| TEACHER | Chỉ giáo viên |
| CONSULTANT | Chỉ tư vấn viên |
| ADMIN | Chỉ quản trị viên |

Các mã lỗi thường gặp: `400` dữ liệu không hợp lệ, `401` chưa xác thực, `403` không có quyền, `404` không tìm thấy, `409` trùng dữ liệu.

---

## 2. Authentication

### POST `/auth/login`

- Quyền: Public
- Công dụng: đăng nhập chung cho `STUDENT` và `TEACHER`; tài khoản Admin không được đăng nhập qua cổng này.

```json
{
  "email": "student@example.com",
  "password": "Student@123"
}
```

Response `data`:

```json
{
  "token": "jwt-token",
  "userId": 1,
  "email": "student@example.com",
  "role": "Học viên",
  "roleCode": "STUDENT"
}
```

### POST `/admin/auth/login`

- Quyền: Public
- Công dụng: đăng nhập riêng cho `ADMIN`.
- Request và response giống `/auth/login`.

### POST `/auth/register`

- Quyền: Public
- Công dụng: đăng ký tài khoản học viên.

```json
{
  "username": "nguyenvana",
  "password": "Student@123",
  "fullName": "Nguyễn Văn A",
  "email": "student@example.com",
  "phoneNumber": "0900000000",
  "address": "TP. Hồ Chí Minh",
  "dateOfBirth": "2000-01-01",
  "gender": "MALE",
  "avatar": "https://example.com/avatar.jpg"
}
```

`gender`: `MALE`, `FEMALE`, `OTHER`.

### POST `/auth/teacher/register`

- Quyền: Public
- Công dụng: giáo viên tự gửi đăng ký tài khoản.
- Trạng thái ban đầu: `INACTIVE`; chưa thể đăng nhập cho tới khi Admin đổi sang `ACTIVE` qua `PATCH /admin/users/{id}/status`.

```json
{
  "username": "teacher01",
  "password": "Teacher@123",
  "fullName": "Nguyễn Văn B",
  "email": "teacher@example.com",
  "phoneNumber": "0900000001",
  "address": "TP. Hồ Chí Minh",
  "specialization": "Tiếng Nhật N5",
  "degree": "Cử nhân Ngôn ngữ Nhật",
  "experienceYears": 3
}
```

### GET `/auth/me`

- Quyền: Authenticated
- Công dụng: kiểm tra token và lấy thông tin người đang đăng nhập.
- Trả về: `UserResponse`.

---

## 3. Khóa học public

### GET `/courses`

- Quyền: Public
- Công dụng: lấy các khóa học `ACTIVE + PUBLISHED`.

| Query | Mặc định | Mô tả |
|---|---:|---|
| `keyword` | rỗng | Tìm theo mã hoặc tên |
| `languageId` | rỗng | Lọc ngôn ngữ |
| `levelId` | rỗng | Lọc trình độ |
| `page` | `0` | Trang, bắt đầu từ 0 |
| `size` | `10` | Số phần tử |
| `sort` | `courseCode` | Trường sắp xếp |

- Trả về: `PageResponse<CourseResponse>`.

### GET `/courses/slug/{slug}`

- Quyền: Public
- Công dụng: lấy chi tiết khóa học `ACTIVE + PUBLISHED` theo slug, ví dụ `ngu-phap-minna-no-nihongo-n5`.
- Trả về: `CourseResponse`.

### GET `/courses/{id}/sections`

- Quyền: Public
- Công dụng: lấy danh sách section của một khóa học đang xuất bản; chưa tải content.
- Trả về: `CourseSectionResponse[]`.

### GET `/sections/{id}/contents`

- Quyền HTTP: Public; có thể gửi Bearer Token.
- Công dụng: tải content khi người dùng mở một section.
- Quy tắc dữ liệu:
  - Course phải `ACTIVE + PUBLISHED`.
  - Course miễn phí: trả toàn bộ content `PUBLISHED`.
  - Course có phí, khách hoặc người chưa mua: chỉ trả content `PUBLISHED` có `preview = true`.
  - Student có enrollment `CONFIRMED + PAID`: trả toàn bộ content `PUBLISHED`.
- Trả về: `CourseContentResponse[]`.

---

## 4. Ngôn ngữ, trình độ và phòng học public

### GET `/languages`

- Quyền: Public
- Công dụng: lấy ngôn ngữ `ACTIVE`.
- Trả về: `LanguageResponse[]`.

### GET `/languages/{id}`

- Quyền: Public
- Công dụng: lấy một ngôn ngữ `ACTIVE`.

### GET `/languages/{id}/levels`

- Quyền: Public
- Công dụng: lấy các trình độ `ACTIVE` thuộc ngôn ngữ.

### GET `/levels`

- Quyền: Public
- Công dụng: lấy toàn bộ trình độ `ACTIVE`.
- Muốn lấy trình độ theo ngôn ngữ, dùng `GET /languages/{id}/levels`.

### GET `/levels/{id}`

- Quyền: Public
- Công dụng: lấy một trình độ `ACTIVE`.

### GET `/rooms`

- Quyền: Public
- Công dụng: lấy danh sách phòng học.

### GET `/rooms/{id}`

- Quyền: Public
- Công dụng: lấy chi tiết phòng học.

### POST `/rooms`

- Quyền: ADMIN
- Request: xem `RoomRequest` ở phần schema.

### PUT `/rooms/{id}`

- Quyền: ADMIN
- Request: `RoomRequest`.

### DELETE `/rooms/{id}`

- Quyền: ADMIN
- Không thể xóa phòng đã có lịch học.

> Các API ghi `/rooms` được giữ để tương thích. Phần Admin cũng có nhóm `/admin/rooms` bên dưới.

---

## 5. Lớp học public

### GET `/classes`

- Quyền: Public
- Công dụng: tìm các lớp có trạng thái `OPEN`.

| Query | Mặc định | Mô tả |
|---|---:|---|
| `keyword` | rỗng | Tìm mã hoặc tên lớp |
| `courseId` | rỗng | Lọc khóa học |
| `levelId` | rỗng | Lọc trình độ |
| `date` | rỗng | Ngày dạng `yyyy-MM-dd` |
| `page` | `0` | Trang, bắt đầu từ 0 |
| `size` | `10` | Số phần tử |
| `sort` | `startDate` | Trường sắp xếp |
| `direction` | `asc` | `asc` hoặc `desc` |

- Trả về: `PageResponse<CourseClassResponse>`.

### GET `/classes/{id}`

- Quyền: Public
- Công dụng: lấy chi tiết lớp đang mở.

### GET `/classes/{classId}/schedules`

- Quyền hiện tại: Public
- Công dụng: lấy lịch học cố định của lớp.
- Trả về: `ClassScheduleResponse[]`.

---

### GET `/teachers`

- Quyền: Public.
- Công dụng: lấy danh sách giảng viên có tài khoản `ACTIVE` để giới thiệu trên trang chủ.
- Trả về: `TeacherOptionResponse[]`, gồm `id`, `teacherCode`, `fullName`, `specialization` và `degree`.
- Không trả email, số điện thoại hoặc thông tin đăng nhập của giảng viên.

---

## 6. API dành cho Student

### GET `/students/me/profile`

- Quyền: STUDENT.
- Công dụng: lấy thông tin tài khoản và hồ sơ Student đang đăng nhập, gồm mã học viên, họ tên, email, số điện thoại, địa chỉ, ngày sinh, giới tính và ảnh đại diện.
- Trả về: `StudentProfileResponse`.

### PUT `/students/me/profile`

- Quyền: STUDENT.
- Công dụng: cập nhật họ tên, số điện thoại, địa chỉ, ngày sinh, giới tính và URL ảnh đại diện.
- Không cho sửa email, username, mã học viên, vai trò hoặc trạng thái tài khoản.

```json
{
  "fullName": "Nguyễn Văn An",
  "phoneNumber": "0901234567",
  "address": "TP. Hồ Chí Minh",
  "dateOfBirth": "2002-05-20",
  "gender": "MALE",
  "avatar": "https://example.com/avatar.jpg"
}
```

### POST `/enrollments`

- Quyền: STUDENT
- Công dụng: học viên hiện tại đăng ký một lớp.

```json
{
  "courseClassId": 1
}
```

- Khi tạo mới: `enrollmentStatus = PENDING`, `paymentStatus = PENDING`.
- Trả về HTTP `201` và `EnrollmentResponse`.

### GET `/students/me/enrollments`

- Quyền: STUDENT
- Công dụng: lấy lịch sử đăng ký của học viên hiện tại.
- Trả về: `EnrollmentSummaryResponse[]`.

### GET `/students/me/courses`

- Quyền: STUDENT
- Công dụng: trang “Khóa học của tôi”.
- Chỉ trả khóa học:
  - Enrollment `CONFIRMED`.
  - Payment `PAID`.
  - Course `ACTIVE + PUBLISHED`.
- Trả về: `CourseResponse[]`.

### GET `/students/me/classes`

- Quyền: STUDENT
- Công dụng: lấy các lớp học đã được kích hoạt của học viên hiện tại.
- Điều kiện: enrollment `CONFIRMED`, payment `PAID`, lớp không `CANCELLED`, khóa học `ACTIVE + PUBLISHED`.
- Trả về: `CourseClassResponse[]`, sắp xếp theo ngày bắt đầu mới nhất.

### GET `/students/me/schedules`

- Quyền: STUDENT
- Công dụng: lấy thời khóa biểu hàng tuần của các lớp đã được kích hoạt.
- Điều kiện quyền giống `/students/me/classes`.
- Trả về: `ClassScheduleResponse[]`, sắp xếp theo thứ và giờ bắt đầu.

### POST `/enrollments/{id}/cancel-request`

- Quyền: STUDENT
- Công dụng: hủy enrollment thuộc chính học viên đang đăng nhập.

```json
{
  "cancellationReason": "Không thể tiếp tục tham gia lớp"
}
```

Chỉ hủy trước ngày khai giảng và khi chưa phát sinh thanh toán.

### POST `/payments`

- Quyền: STUDENT; chỉ thanh toán enrollment thuộc chính tài khoản hiện tại.
- Enrollment phải được Staff xác nhận: `enrollmentStatus = CONFIRMED` và `paymentStatus = PENDING`.
- Phương thức: `MOMO` hoặc `ZALOPAY`.
- Hệ thống chỉ hỗ trợ thanh toán online qua hai ví này; không nhận tiền mặt, chuyển khoản ngân hàng hoặc thẻ trực tiếp trong bảng `payment`.
- Hệ thống tạo giao dịch `PENDING`, ký request ở backend và trả `paymentUrl` để frontend chuyển trang.

```json
{
  "enrollmentId": 15,
  "method": "MOMO"
}
```

### GET `/students/me/payments`

- Quyền: STUDENT.
- Trả lịch sử giao dịch của Student hiện tại, không trả secret hoặc dữ liệu của tài khoản khác.

### POST `/payments/momo/ipn`

- Public callback dành cho MoMo sandbox.
- Backend xác minh HMAC-SHA256, mã giao dịch và số tiền trước khi cập nhật payment và enrollment sang `PAID`.

### POST `/payments/zalopay/callback`

- Public callback dành cho ZaloPay sandbox.
- Backend xác minh MAC bằng `ZALOPAY_KEY2`, mã giao dịch và số tiền trước khi cập nhật payment và enrollment sang `PAID`.

---

## 7. API dành cho Teacher

### GET `/teachers/me/profile`

- Quyền: TEACHER.
- Công dụng: lấy tài khoản và hồ sơ chuyên môn của giảng viên đang đăng nhập.
- Trả về: mã giảng viên, username, họ tên, email, điện thoại, địa chỉ, chuyên môn, bằng cấp, số năm kinh nghiệm và trạng thái.

### PUT `/teachers/me/profile`

- Quyền: TEACHER.
- Công dụng: cập nhật họ tên, điện thoại, địa chỉ và thông tin chuyên môn của chính giảng viên.
- Không cho sửa email, username, mã giảng viên, vai trò hoặc trạng thái tài khoản.

```json
{
  "fullName": "Lê Hoàng Nam",
  "phoneNumber": "0902000002",
  "address": "Thành phố Hồ Chí Minh",
  "specialization": "Japanese language and JLPT",
  "degree": "Bachelor of Japanese Studies",
  "experienceYears": 5
}
```

### GET `/teachers/me/classes`

- Quyền: TEACHER
- Công dụng: lấy các lớp được phân công cho giáo viên hiện tại.
- Trả về: `CourseClassResponse[]`.

### GET `/teachers/me/courses`

- Quyền: TEACHER
- Công dụng: lấy danh sách khóa học không trùng lặp từ các lớp được phân công cho giáo viên hiện tại.
- Trả về: `CourseResponse[]`.

### GET `/classes/{classId}/enrollments`

- Quyền: ADMIN, CONSULTANT, TEACHER
- Teacher chỉ xem được lớp mình phụ trách.
- Trả về: `EnrollmentSummaryResponse[]`.

### GET `/classes/{classId}/lessons`

- Quyền HTTP: Authenticated.
- Quyền nghiệp vụ: ADMIN/CONSULTANT, giáo viên phụ trách hoặc Student có enrollment `CONFIRMED + PAID` đúng lớp.
- Trả về: `LessonResponse[]`.

### PUT `/lessons/{id}`

- Quyền: ADMIN, CONSULTANT, TEACHER
- Teacher chỉ sửa buổi học thuộc lớp mình phụ trách.

```json
{
  "topic": "Ngữ pháp bài 1",
  "meetingUrl": "https://meet.example.com/lesson-1"
}
```

---

## 8. Quản lý enrollment — Staff

> Các API hiện đã được tích hợp vào màn hình Admin `/admin/enrollments`. Màn hình làm việc theo từng lớp vì backend chưa có API phân trang toàn bộ enrollment.

### POST `/staff/enrollments`

- Quyền: ADMIN, CONSULTANT
- Công dụng: nhân viên xếp lớp cho học viên.

```json
{
  "courseClassId": 1,
  "studentId": 10
}
```

### PATCH `/staff/enrollments/{id}/status`

- Quyền: ADMIN, CONSULTANT

```json
{
  "status": "CONFIRMED"
}
```

`status`: `PENDING`, `CONFIRMED`, `CANCELLED`.

### POST `/staff/enrollments/{id}/transfer`

- Quyền: ADMIN, CONSULTANT
- Công dụng: chuyển một enrollment chưa thanh toán sang lớp khác.

```json
{
  "targetCourseClassId": 2
}
```

---

## 9. Quản lý lịch học và buổi học

### POST `/classes/{classId}/schedules`

- Quyền: ADMIN, CONSULTANT
- HTTP thành công: `201`.

```json
{
  "roomId": 1,
  "dayOfWeek": 2,
  "startTime": "18:00",
  "endTime": "20:00",
  "deliveryMode": "IN_PERSON",
  "meetingUrl": null
}
```

- `dayOfWeek`: từ `1` đến `7`.
- `deliveryMode`: `IN_PERSON` hoặc `ONLINE`.
- `IN_PERSON` bắt buộc có `roomId` và không có `meetingUrl`.
- `ONLINE` bắt buộc có `meetingUrl` và không có `roomId`.

### PUT `/schedules/{id}`

- Quyền: ADMIN, CONSULTANT
- Request: giống API tạo schedule.

### DELETE `/schedules/{id}`

- Quyền: ADMIN, CONSULTANT
- Không thể xóa lịch đã sinh buổi học.

### POST `/classes/{classId}/lessons/generate`

- Quyền: ADMIN, CONSULTANT
- Công dụng: sinh danh sách buổi học từ schedule và tổng số buổi của khóa học.
- HTTP thành công: `201`.

### PATCH `/lessons/{id}/reschedule`

- Quyền: ADMIN, CONSULTANT

```json
{
  "lessonDate": "2026-09-20"
}
```

### PATCH `/lessons/{id}/cancel`

- Quyền: ADMIN, CONSULTANT
- Công dụng: chuyển trạng thái buổi học thành `CANCELLED`.

---

## 10. Admin — Người dùng và giáo viên

### POST `/admin/teachers`

- Quyền: ADMIN

```json
{
  "username": "teacher01",
  "password": "Teacher@123",
  "fullName": "Giáo viên A",
  "email": "teacher@example.com",
  "phoneNumber": "0900000000",
  "address": "TP. Hồ Chí Minh",
  "specialization": "Tiếng Nhật",
  "degree": "Cử nhân",
  "experienceYears": 3
}
```

### GET `/admin/teachers`

- Quyền: ADMIN
- Công dụng: lấy danh sách giáo viên đang hoạt động để chọn khi phân công.
- Trả về: `TeacherOptionResponse[]`.

### GET `/admin/users`

- Quyền: ADMIN

| Query | Mặc định |
|---|---:|
| `keyword` | rỗng |
| `roleCode` | rỗng |
| `status` | rỗng |
| `page` | `0` |
| `size` | `10` |
| `sort` | `createdAt` |
| `direction` | `desc` |

- Trả về: `PageResponse<UserResponse>`.

### GET `/admin/users/{id}`

- Quyền: ADMIN
- Trả về: `UserResponse`.

### PATCH `/admin/users/{id}/status`

- Quyền: ADMIN

```json
{
  "status": "ACTIVE"
}
```

`status`: `ACTIVE`, `INACTIVE`, `LOCKED`.

---

## 11. Admin — Khóa học

### GET `/admin/courses`

- Quyền: ADMIN
- Query: `keyword`, `languageId`, `levelId`, `status`, `page=0`, `size=10`, `sort=courseCode`.
- Trả về: `PageResponse<CourseResponse>` gồm cả khóa nháp/chưa hoạt động theo bộ lọc.

### GET `/admin/courses/{id}`

- Quyền: ADMIN
- Trả về: `CourseResponse`.

### POST `/admin/courses`

- Quyền: ADMIN
- HTTP thành công: `201`.
- Request: `CourseRequest` ở phần schema.

### PUT `/admin/courses/{id}`

- Quyền: ADMIN
- Request: `CourseRequest`.

### DELETE `/admin/courses/{id}`

- Quyền: ADMIN
- Không thể xóa khóa học đã có lớp.

---

## 12. Admin/Consultant — Lớp học

### GET `/admin/classes`

- Quyền: ADMIN, CONSULTANT
- Query: `keyword`, `courseId`, `levelId`, `status`, `page=0`, `size=10`, `sort=startDate`, `direction=asc`.
- Trả về: `PageResponse<CourseClassResponse>`.

### POST `/admin/classes`

- Quyền: ADMIN, CONSULTANT
- HTTP thành công: `201`.
- Request: `CourseClassRequest` ở phần schema.

### PUT `/admin/classes/{id}`

- Quyền: ADMIN, CONSULTANT
- Request: `CourseClassRequest`.

### PATCH `/admin/classes/{id}/teacher`

- Quyền: ADMIN, CONSULTANT

```json
{
  "teacherId": 1
}
```

### PATCH `/admin/classes/{id}/status`

- Quyền: chỉ ADMIN

```json
{
  "status": "OPEN"
}
```

`status`: `DRAFT`, `OPEN`, `FULL`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`.

---

## 13. Admin — Ngôn ngữ

### GET `/admin/languages`

- Quyền: ADMIN
- Query tùy chọn: `status`.

### GET `/admin/languages/{id}`

- Quyền: ADMIN

### POST `/admin/languages`

- Quyền: ADMIN
- Request: `LanguageRequest`.
- HTTP thành công: `201`.

### PUT `/admin/languages/{id}`

- Quyền: ADMIN
- Request: `LanguageRequest`.

### PATCH `/admin/languages/{id}/status`

- Quyền: ADMIN
- Body: `{ "status": "ACTIVE" }`.

### DELETE `/admin/languages/{id}`

- Quyền: ADMIN
- Không thể xóa ngôn ngữ đã có trình độ.

---

## 14. Admin — Trình độ

### GET `/admin/levels`

- Quyền: ADMIN
- Query tùy chọn: `languageId`, `status`.

### GET `/admin/levels/{id}`

- Quyền: ADMIN

### POST `/admin/levels`

- Quyền: ADMIN
- Request: `LevelRequest`.
- HTTP thành công: `201`.

### PUT `/admin/levels/{id}`

- Quyền: ADMIN
- Request: `LevelRequest`.

### PATCH `/admin/levels/{id}/status`

- Quyền: ADMIN
- Body: `{ "status": "ACTIVE" }`.

### DELETE `/admin/levels/{id}`

- Quyền: ADMIN
- Không thể xóa trình độ đã có khóa học.

---

## 15. Admin — Phòng học

### GET `/admin/rooms`

- Quyền: ADMIN
- Query tùy chọn: `status`.

### GET `/admin/rooms/{id}`

- Quyền: ADMIN

### POST `/admin/rooms`

- Quyền: ADMIN
- Request: `RoomRequest`.
- HTTP thành công: `201`.

### PUT `/admin/rooms/{id}`

- Quyền: ADMIN
- Request: `RoomRequest`.

### DELETE `/admin/rooms/{id}`

- Quyền: ADMIN
- Không thể xóa phòng đã có lịch học.

---

## 16. Request schema dùng chung

### `CourseRequest`

```json
{
  "courseCode": "JA-N5-01",
  "courseName": "Ngữ pháp tiếng Nhật N5",
  "slug": "ngu-phap-tieng-nhat-n5",
  "shortDescription": "Mô tả ngắn",
  "description": "Mô tả chi tiết",
  "thumbnailUrl": "https://example.com/thumbnail.jpg",
  "bannerUrl": "https://example.com/banner.jpg",
  "targetAudience": "Người mới học",
  "prerequisites": "Biết Hiragana và Katakana",
  "learningOutcomes": "Nắm vững ngữ pháp N5",
  "syllabusSummary": "25 bài học",
  "certificateInfo": "Chứng nhận hoàn thành",
  "tuitionFee": 1200000,
  "totalSessions": 25,
  "durationHours": 50,
  "levelId": 1,
  "status": "ACTIVE",
  "publicationStatus": "PUBLISHED",
  "featured": true
}
```

- `status`: `ACTIVE`, `INACTIVE`.
- `publicationStatus`: `DRAFT`, `PUBLISHED`, `ARCHIVED`.
- `slug`: chữ thường, số và dấu gạch ngang.

### `CourseClassRequest`

```json
{
  "classCode": "JA-N5-K01",
  "className": "Lớp N5 buổi tối",
  "startDate": "2026-10-01",
  "endDate": "2027-01-31",
  "maxStudents": 20,
  "appliedTuitionFee": 1200000,
  "courseId": 1,
  "teacherId": 1
}
```

### `LanguageRequest`

```json
{
  "languageCode": "JA",
  "languageName": "Tiếng Nhật",
  "description": "Chương trình tiếng Nhật",
  "status": "ACTIVE"
}
```

### `LevelRequest`

```json
{
  "languageId": 1,
  "levelCode": "N5",
  "levelName": "Sơ cấp N5",
  "description": "Trình độ nhập môn",
  "displayOrder": 1,
  "status": "ACTIVE"
}
```

### `RoomRequest`

```json
{
  "roomCode": "P101",
  "roomName": "Phòng 101",
  "capacity": 25,
  "location": "Tầng 1",
  "status": "ACTIVE"
}
```

`status`: `ACTIVE`, `MAINTENANCE`, `INACTIVE`.

---

## 17. Các response model chính

- `UserResponse`: `id`, `username`, `fullName`, `email`, `phoneNumber`, `address`, `roleName`, `roleCode`, `status`, `createdAt`, `updatedAt`.
- `CourseResponse`: thông tin khóa học, URL ảnh, học phí, trạng thái, trình độ, ngôn ngữ và thời gian tạo/cập nhật.
- `CourseSectionResponse`: `id`, `title`, `description`, `displayOrder`.
- `CourseContentResponse`: `id`, `title`, `summary`, `contentHtml`, `audioUrl`, `videoUrl`, `documentUrl`, `contentType`, `displayOrder`, `preview`.
- `CourseClassResponse`: thông tin lớp, số chỗ đã đăng ký/còn lại, khóa học và giáo viên.
- `ClassScheduleResponse`: lớp, thứ, giờ học, hình thức, `roomCode`, `roomName`, `roomLocation` hoặc `meetingUrl`.
- `LessonResponse`: ngày/giờ học, chủ đề, trạng thái, hình thức, phòng hoặc meeting URL.
- `EnrollmentResponse`: thông tin đầy đủ enrollment, học viên, lớp và khóa học.
- `EnrollmentSummaryResponse`: thông tin tóm tắt enrollment dùng cho danh sách.

## 18. Lưu ý hiện trạng

- Backend dùng session stateless; server không lưu phiên đăng nhập, frontend giữ JWT trong `localStorage`.
- Một Bearer Token sai hoặc hết hạn gửi vào cả API Public cũng bị `JwtFilter` trả `401`; khi gọi với tư cách khách, không gửi header Authorization.
- Staff chỉ xác nhận, hủy hoặc chuyển enrollment. `PAID` chỉ được cập nhật sau callback MoMo/ZaloPay có chữ ký hợp lệ.
- API CRUD cho `course_section` và `course_content` chưa có; hiện backend mới cung cấp API đọc public cho curriculum.
- Student và Teacher đã có API cùng giao diện đọc/cập nhật hồ sơ riêng.
