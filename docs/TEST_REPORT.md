# BÁO CÁO KIỂM THỬ

Ngày cập nhật: **11/09/2026**

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

## 3. Kết quả kiểm thử

Lệnh xác minh:

```powershell
cd backend/language-center-management
mvn.cmd verify
```

Kết quả ngày 11/09/2026:

| Chỉ số | Kết quả |
|---|---:|
| Test class | 36 |
| Unit test | 205 |
| Integration test | 90 |
| Spring context test | 1 |
| Tổng test | 296 |
| Thành công | 296 |
| Failure | 0 |
| Error | 0 |
| Skipped | 0 |

Các nhóm unit test:

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
| `EnrollmentServiceImplTest` | 15 | Pass |
| `EnrollmentExpirationServiceImplTest` | 4 | Pass |
| `PaymentServiceImplTest` | 19 | Pass |
| `BillingServiceImplTest` | 14 | Pass |
| `AttendanceServiceImplTest` | 9 | Pass |
| `DashboardServiceImplTest` | 6 | Pass |
| `CloudinaryImageUploadServiceTest` | 6 | Pass |
| `InvoicePdfServiceImplTest` | 3 | Pass |
| `SystemLogServiceImplTest` | 4 | Pass |
| `JwtFilterTest` | 6 | Pass |
| `JwtUtilsTest` | 3 | Pass |
| Mapper, controller và exception handler | 11 | Pass |

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

### Enrollment và thời hạn thanh toán

- Student và Staff đăng ký theo đúng quyền, trạng thái lớp, sức chứa và lịch trùng.
- Hạn thanh toán được tạo sau hai ngày; enrollment miễn phí được xác nhận ngay.
- Hủy đăng ký, mở lại lớp `FULL`, chuyển lớp cùng khóa học và khóa bản ghi theo thứ tự an toàn.
- Kiểm tra quyền xem enrollment của Student, Teacher phụ trách và Staff.
- Scheduler chỉ hết hạn enrollment đủ điều kiện và đã quá hạn.

### Payment, refund và hóa đơn

- Kiểm tra điều kiện thanh toán, quyền sở hữu, hạn thanh toán và cấu hình callback public.
- Xác minh chữ ký/MAC, số tiền, tính idempotent và callback thành công/thất bại của MoMo/ZaloPay.
- Không tạo Payment `PENDING` nếu gateway chưa trả URL thanh toán hợp lệ.
- Hoàn tiền thành công, bị từ chối, timeout, đối soát lại và bảo toàn quyền học khi chưa `COMPLETED`.
- Xuất PDF có nội dung hợp lệ, xử lý dữ liệu nullable và lỗi font cấu hình.

### Attendance, dashboard, media và security

- Điểm danh hàng loạt không N+1, cập nhật giữ nguyên `attendanceTime`, kiểm tra cửa sổ bảy ngày và quyền Teacher.
- Tổng hợp tiến độ/chuyên cần dùng đúng mẫu số và bỏ qua lesson bị hủy.
- Dashboard kiểm tra các chỉ số tổng hợp và khoảng ngày báo cáo.
- Cloudinary kiểm tra loại file, kích thước, phản hồi thiếu URL và xóa ảnh khi upload lỗi.
- JWT filter/util kiểm tra token hợp lệ, hết hạn/sai chữ ký, tài khoản không hoạt động và không ghi đè SecurityContext.
- System log kiểm tra filter, phân trang, mapping và cơ chế không làm hỏng nghiệp vụ chính khi ghi log thất bại.

Chi tiết từng trường hợp được theo dõi tại [UNIT_TEST_CHECKLIST.md](UNIT_TEST_CHECKLIST.md).

## 5. Độ bao phủ JaCoCo

Độ bao phủ hiện tại được tính trên toàn bộ mã nguồn backend trong lần chạy `mvn verify`, gồm unit test và integration test:

| Loại coverage | Covered | Tổng | Tỷ lệ |
|---|---:|---:|---:|
| Branch | 728 | 1.316 | 55,32% |
| Line | 3.492 | 4.511 | 77,41% |

Báo cáo HTML được sinh tại:

```text
backend/language-center-management/target/site/jacoco/index.html
```

Checklist đã bao phủ các nhánh nghiệp vụ chính. Coverage tổng thể không được đẩy lên bằng các test getter/setter, DTO hoặc cấu hình không có logic; hiện chưa dùng coverage làm quality gate bắt buộc.

## 6. Lỗi nghiệp vụ phát hiện trong quá trình test

- Luồng đăng ký trước đây kiểm tra và lưu email/username nguyên trạng. Test đã phát hiện nguy cơ email có khoảng trắng hoặc khác chữ hoa/thường vượt qua kiểm tra trùng. Service đã được sửa để trim username, trim và chuyển email về chữ thường trước khi kiểm tra và lưu.
- Mô tả cổng Staff trước đây cho rằng có thể nhận cả Admin. Kiểm tra code xác nhận cổng Staff chỉ nhận `CONSULTANT`; Admin sử dụng cổng đăng nhập Admin riêng. Checklist đã được điều chỉnh theo đúng triển khai.
- Bộ lọc trạng thái lớp phía Admin từng so sánh chuỗi với khóa enum nên status hợp lệ vẫn bị từ chối. Service đã chuẩn hóa chuỗi và chuyển sang `ClassStatus` trước khi kiểm tra.
- Chuyển lớp sang `FULL` trước đây chưa kiểm tra sĩ số. Service hiện chỉ cho phép khi số enrollment đang hoạt động đạt sức chứa.
- Truy vấn lớp của Teacher trước đây có thể phát sinh `NullPointerException` khi Principal null. Luồng hiện dùng chung bước kiểm tra Principal/profile an toàn.
- Chuyển đổi `java.sql.Date` trong kiểm tra lịch từng gây `UnsupportedOperationException`; service hiện xử lý riêng kiểu ngày JDBC.
- Các thao tác lịch và Lesson quan trọng trước đây chỉ đọc bản ghi thông thường. Repository/service hiện dùng `PESSIMISTIC_WRITE` để ngăn hai transaction cùng sửa hoặc cùng sinh dữ liệu dựa trên trạng thái cũ.
- Luồng hủy/chuyển enrollment từng so sánh `ClassStatus` với tập `String`, khiến lớp `OPEN`/`FULL` hợp lệ vẫn có thể bị từ chối. Service hiện so sánh trực tiếp bằng enum.

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

## 9. Giới hạn và công việc tiếp theo

- Bổ sung Testcontainers MySQL để chứng minh hành vi chờ lock và rollback ở database thật.
- Tăng branch coverage cho các lớp còn nhiều nhánh tích hợp bên ngoài khi có môi trường sandbox ổn định.
- Cân nhắc đặt quality gate JaCoCo sau khi phạm vi test chính đã hoàn thành.
- Không phụ thuộc duy nhất vào coverage: assertion phải kiểm tra đúng kết quả và side effect nghiệp vụ.
