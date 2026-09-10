# BÁO CÁO KIỂM THỬ

Ngày cập nhật: **10/09/2026**

## 1. Mục tiêu

Báo cáo ghi nhận kết quả kiểm thử tự động của backend hệ thống quản lý trung tâm ngoại ngữ. Unit test tập trung xác minh nghiệp vụ độc lập; integration test xác minh controller, bảo mật, validation, persistence và transaction khi các thành phần phối hợp.

## 2. Công cụ và quy ước

- Java và Spring Boot.
- JUnit 5 làm test framework.
- Mockito cô lập dependency trong unit test.
- AssertJ và JUnit Assertions kiểm tra kết quả.
- Maven Surefire thực thi và xuất báo cáo test.
- JaCoCo đo độ bao phủ mã nguồn.
- Tên test theo mẫu `shouldExpectedResultWhenCondition`.

## 3. Kết quả unit test

Lệnh xác minh:

```powershell
cd backend/language-center-management
mvn.cmd clean verify '-Dtest=com.ntt.language_center_management.unit.**'
```

Kết quả ngày 10/09/2026:

| Chỉ số | Kết quả |
|---|---:|
| Tổng unit test | 116 |
| Thành công | 116 |
| Failure | 0 |
| Error | 0 |
| Skipped | 0 |

Các service thuộc mục 3–6:

| Nhóm test | Số test | Kết quả |
|---|---:|---|
| `UserServiceImplTest` | 16 | Pass |
| `TeacherServiceImplTest` | 8 | Pass |
| `LanguageServiceImplTest` | 10 | Pass |
| `LevelServiceImplTest` | 12 | Pass |
| `RoomServiceImplTest` | 8 | Pass |
| `CourseServiceImplTest` | 10 | Pass |
| `CourseCurriculumAdminServiceTest` | 9 | Pass |
| `CourseClassServiceImplTest` | 13 | Pass |
| `ClassScheduleServiceImplTest` | 8 | Pass |
| `LessonServiceImplTest` | 11 | Pass |

## 4. Phạm vi nghiệp vụ đã kiểm thử

### UserServiceImpl

- Đăng nhập đúng/sai thông tin, tài khoản `INACTIVE` và `LOCKED`.
- Phân biệt cổng đăng nhập Student/Teacher, Admin và Consultant.
- Đăng ký Student tạo đồng thời `User` và `Student`.
- Đăng ký Teacher tạo tài khoản chờ kích hoạt và `Teacher` record.
- Chuẩn hóa email/username, encode mật khẩu và từ chối dữ liệu trùng.
- Sinh mã Student/Teacher đúng định dạng và không trùng trong các lần tạo.
- Lấy/cập nhật hồ sơ, xử lý Principal thiếu và chuẩn hóa trường tùy chọn.
- Đổi mật khẩu và các trường hợp mật khẩu sai, trùng hoặc confirm không khớp.
- Tìm kiếm có filter, sort, pagination và đổi trạng thái user.

### TeacherServiceImpl

- Chỉ lấy Teacher có tài khoản `ACTIVE`.
- Lấy và cập nhật đúng hồ sơ theo Principal.
- Chuẩn hóa chuỗi và cập nhật `experienceYears`, `updatedAt`.
- Truyền lỗi repository ra ngoài, không tạo response thành công giả.

### LanguageServiceImpl

- Tạo/cập nhật Language và chuẩn hóa code/name/description.
- Kiểm tra trùng code hoặc name, có loại trừ chính bản ghi đang cập nhật.
- Lọc trạng thái, lấy dữ liệu public `ACTIVE` và xử lý ID không tồn tại.
- Đổi trạng thái và ngăn xóa Language đã có Level.

### LevelServiceImpl

- Tạo/cập nhật Level và kiểm tra Language đích đang `ACTIVE`.
- Kiểm tra trùng code/display order trong phạm vi một Language.
- Cho phép cùng level code ở các Language khác nhau.
- Lấy dữ liệu public chỉ khi cả Level và Language đều `ACTIVE`.
- Ngăn xóa Level có Course và cho phép xóa Level chưa có Course.

### RoomServiceImpl

- Tạo/cập nhật Room, chuẩn hóa dữ liệu và loại trừ chính bản ghi khi kiểm tra trùng.
- Lọc đúng các trạng thái `ACTIVE`, `MAINTENANCE`, `INACTIVE`.
- Từ chối trạng thái không hỗ trợ và ID không tồn tại.
- Ngăn xóa Room đã có lịch; cho phép xóa Room chưa được sử dụng.

### CourseServiceImpl

- Tìm kiếm có keyword, Language, Level, trạng thái, phân trang và sắp xếp an toàn.
- Chỉ lấy khóa học public theo slug với `ACTIVE` và `PUBLISHED`.
- Chuẩn hóa code, slug và các trường văn bản tùy chọn khi lưu.
- Kiểm tra trùng code/slug và loại trừ Course hiện tại khi cập nhật.
- Gán `publishedAt` khi publish và xóa thời điểm này khi chuyển về Draft.
- Ngăn xóa Course đã có Class hoặc Section.

### CourseCurriculumServiceImpl và mapper

- Trả section/content đã xuất bản theo đúng thứ tự repository.
- Khách chưa đăng nhập và principal không phải Student chỉ nhận nội dung preview.
- Student có enrollment `CONFIRMED` và thanh toán `PAID` nhận toàn bộ nội dung đã xuất bản.
- Xử lý Course/Section không tồn tại và map đầy đủ nội dung, media, loại nội dung, preview.

### CourseClassServiceImpl

- Tìm kiếm public/admin với filter, pagination và sort an toàn.
- Kiểm tra mã lớp, thời gian, sĩ số và học phí khi tạo/cập nhật.
- Chỉ phân công Teacher đang hoạt động.
- Kiểm tra điều kiện mở lớp, lịch nội bộ và xung đột phòng/Teacher.
- Chỉ chuyển `FULL` khi số enrollment hợp lệ đạt sức chứa.
- Khóa bản ghi trước khi đổi trạng thái và giới hạn dữ liệu theo Teacher hiện tại.

### ClassScheduleServiceImpl

- Kiểm tra quy tắc phòng học trực tiếp và đường dẫn học online.
- Kiểm tra ngày trong tuần, khoảng giờ, sức chứa phòng và xung đột tài nguyên.
- Không cho sửa lịch của lớp kết thúc/hủy hoặc lịch đã sinh Lesson.
- Create khóa pessimistic lớp; update/delete khóa Schedule rồi khóa lớp cha để tuần tự hóa thao tác đồng thời.

### LessonServiceImpl và scheduler

- Sinh Lesson đúng lịch cố định, khoảng ngày và giới hạn tổng số buổi.
- Kiểm tra quyền Teacher, nội dung, dời lịch, attendance và xung đột tài nguyên.
- Generate khóa pessimistic lớp; update/reschedule/cancel khóa pessimistic Lesson.
- Scheduler hoàn thành cả Lesson `SCHEDULED` và `IN_PROGRESS` đã qua giờ kết thúc theo timezone cấu hình.
- Mapper lấy ngày, giờ, phòng và meeting URL từ ClassSchedule.

Chi tiết từng trường hợp được theo dõi tại [UNIT_TEST_CHECKLIST.md](UNIT_TEST_CHECKLIST.md).

## 5. Độ bao phủ JaCoCo

Độ bao phủ hiện tại được tính trên toàn bộ mã nguồn backend trong lần chạy unit test:

| Loại coverage | Covered | Tổng | Tỷ lệ |
|---|---:|---:|---:|
| Instruction | 7.038 | 20.918 | 33,65% |
| Branch | 356 | 1.302 | 27,34% |
| Line | 1.803 | 4.505 | 40,02% |

Báo cáo HTML được sinh tại:

```text
backend/language-center-management/target/site/jacoco/index.html
```

Tỷ lệ tổng thể còn thấp vì checklist unit test của nhiều service phía sau chưa hoàn thành. Coverage hiện tại là số liệu nền để tiếp tục cải thiện, chưa được dùng làm quality gate.

## 6. Lỗi nghiệp vụ phát hiện trong quá trình test

- Luồng đăng ký trước đây kiểm tra và lưu email/username nguyên trạng. Test đã phát hiện nguy cơ email có khoảng trắng hoặc khác chữ hoa/thường vượt qua kiểm tra trùng. Service đã được sửa để trim username, trim và chuyển email về chữ thường trước khi kiểm tra và lưu.
- Mô tả cổng Staff trước đây cho rằng có thể nhận cả Admin. Kiểm tra code xác nhận cổng Staff chỉ nhận `CONSULTANT`; Admin sử dụng cổng đăng nhập Admin riêng. Checklist đã được điều chỉnh theo đúng triển khai.
- Bộ lọc trạng thái lớp phía Admin từng so sánh chuỗi với khóa enum nên status hợp lệ vẫn bị từ chối. Service đã chuẩn hóa chuỗi và chuyển sang `ClassStatus` trước khi kiểm tra.
- Chuyển lớp sang `FULL` trước đây chưa kiểm tra sĩ số. Service hiện chỉ cho phép khi số enrollment đang hoạt động đạt sức chứa.
- Truy vấn lớp của Teacher trước đây có thể phát sinh `NullPointerException` khi Principal null. Luồng hiện dùng chung bước kiểm tra Principal/profile an toàn.
- Chuyển đổi `java.sql.Date` trong kiểm tra lịch từng gây `UnsupportedOperationException`; service hiện xử lý riêng kiểu ngày JDBC.
- Các thao tác lịch và Lesson quan trọng trước đây chỉ đọc bản ghi thông thường. Repository/service hiện dùng `PESSIMISTIC_WRITE` để ngăn hai transaction cùng sửa hoặc cùng sinh dữ liệu dựa trên trạng thái cũ.

## 7. Kiểm thử transaction đồng thời

Unit test hiện xác minh service gọi đúng repository có `PESSIMISTIC_WRITE` và khóa theo thứ tự aggregate:

- Tạo lịch: khóa `Courseclass` trước khi kiểm tra và lưu.
- Sửa/xóa lịch: khóa `Classschedule`, sau đó khóa `Courseclass` cha.
- Sinh Lesson: khóa `Courseclass` để hai request không cùng tính số buổi còn lại.
- Sửa/dời/hủy Lesson: khóa chính bản ghi `Lesson`.

Unit test dùng Mockito không thể chứng minh transaction thứ hai thực sự chờ transaction thứ nhất. Kịch bản hai thread tranh cùng row cần integration test với MySQL/Testcontainers thật, hai transaction độc lập và cơ chế đồng bộ thread. Dự án hiện chưa cấu hình Testcontainers nên báo cáo chưa tuyên bố đã kiểm thử hành vi chờ lock ở tầng database.

## 8. Cách đọc kết quả thất bại

- `Failure`: test hoàn thành nhưng kết quả thực tế khác mong đợi.
- `Error`: test bị gián đoạn bởi exception ngoài dự kiến hoặc lỗi cấu hình.
- Khi một test thất bại, Maven trả exit code khác `0`; CI phải báo thất bại và thay đổi chưa nên được merge.
- Chi tiết Surefire nằm trong `backend/language-center-management/target/surefire-reports/`.

Một stack trace `Unexpected API error` có thể xuất hiện khi chạy `GlobalExceptionHandlerTest`. Đây là exception giả lập để kiểm tra việc che giấu thông tin nhạy cảm; test vẫn hợp lệ nếu Maven kết thúc với `Failures: 0, Errors: 0`.

## 8. Giới hạn và công việc tiếp theo

- Hoàn thiện các mục unit test còn chưa đánh dấu trong checklist.
- Chạy toàn bộ integration test bằng môi trường database test ổn định.
- Tăng coverage cho các service có nhiều nhánh nghiệp vụ và các trường hợp transaction rollback.
- Cân nhắc đặt quality gate JaCoCo sau khi phạm vi test chính đã hoàn thành.
- Không phụ thuộc duy nhất vào coverage: assertion phải kiểm tra đúng kết quả và side effect nghiệp vụ.
