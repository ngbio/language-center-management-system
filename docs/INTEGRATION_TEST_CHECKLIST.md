# Integration Test Checklist — Backend Spring Boot

## 1. Phạm vi và quy ước

Tài liệu này được lập từ source backend hiện tại, gồm controller, service, repository,
`SecurityConfig`, `application.properties` và schema MySQL.

- `[x]`: đã có test tự động và test đang chạy thành công.
- `[ ]`: chưa có test hoặc chưa kiểm tra đủ luồng được mô tả.
- Controller slice test dùng `@WebMvcTest`, `MockMvc` và mock service.
- Luồng nghiệp vụ/database dùng `@SpringBootTest` hoặc `@DataJpaTest` với MySQL Testcontainers.
- Không dùng database `.env` của developer khi chạy integration test.
- Role thực tế: `ADMIN`, `CONSULTANT`, `TEACHER`, `STUDENT`. Project không có role `MANAGER`.
- Callback MoMo/ZaloPay phải mock HTTP gateway; không gọi sandbox thật trong CI.

## 2. Nền tảng kiểm thử

- [x] Maven chạy JUnit 5, Mockito, AssertJ và Spring MockMvc.
- [x] JaCoCo sinh báo cáo khi chạy `mvn verify`.
- [x] Tạo profile `test` tách khỏi `.env` và MySQL local.
- [ ] Cấu hình MySQL Testcontainers dùng chung cho repository/integration test.
- [ ] Tạo fixture/builder dùng chung cho User, Course, Class, Schedule, Enrollment và Payment.
- [x] Tách test gateway bằng mock web server hoặc HTTP client mock.
- [ ] Cấu hình CI lưu JaCoCo HTML/XML làm artifact.
- [ ] Chỉ đặt coverage gate sau khi các module lõi đã được phủ ổn định.

## 3. Authentication và hồ sơ người dùng

### Public authentication

- [x] `POST /api/auth/login`: đăng nhập Student thành công và trả JWT/role đúng.
- [x] `POST /api/auth/login`: đăng nhập Teacher đã ACTIVE thành công.
- [x] `POST /api/auth/login`: sai thông tin đăng nhập hoặc tài khoản LOCKED/INACTIVE trả `401`.
- [x] `POST /api/auth/register`: request hợp lệ trả `201` và response role STUDENT.
- [ ] `POST /api/auth/register`: tạo User + Student trong cùng transaction.
- [x] `POST /api/auth/register`: controller validate email, password, số điện thoại và map dữ liệu trùng thành `409`.
- [x] `POST /api/auth/teacher/register`: request hợp lệ trả `201`, role TEACHER và status INACTIVE.
- [x] `POST /api/auth/teacher/register`: validation và tài khoản trùng trả `400/409`.
- [ ] `POST /api/auth/teacher/register`: rollback khi tạo User hoặc Teacher thất bại.
- [x] `GET /api/auth/me`: trả đúng người dùng từ Principal đã xác thực.
- [x] `PUT /api/auth/change-password`: request hợp lệ trả `200` và gọi service.
- [x] `PUT /api/auth/change-password`: password yếu trả `400`.
- [x] `PUT /api/auth/change-password`: chưa xác thực trả `401`.
- [x] `PUT /api/auth/change-password`: lỗi mật khẩu hiện tại, xác nhận không khớp và mật khẩu mới trùng cũ trả `400`.

### Admin/Staff authentication

- [x] `POST /api/admin/auth/login`: ADMIN đăng nhập thành công và nhận JWT/role đúng.
- [x] `POST /api/staff/auth/login`: CONSULTANT đăng nhập thành công và nhận JWT/role đúng.
- [x] Cổng Admin map tài khoản sai role thành response `401`.
- [x] Cổng Staff từ chối role không phù hợp.
- [x] Hai cổng từ chối tài khoản không ACTIVE.

### Student/Teacher profile

- [x] `GET/PUT /api/students/me/profile`: đọc, cập nhật, validate, yêu cầu xác thực và chống truy cập chéo.
- [x] `GET/PUT /api/teachers/me/profile`: đọc, cập nhật, validate, yêu cầu xác thực và chống truy cập chéo.
- [x] Student không gọi được API Teacher và Teacher không gọi được API Student (`403`).

## 4. Public catalog

- [x] `GET /api/languages`, `/api/languages/{id}`, `/api/languages/{id}/levels`.
- [x] Public chỉ nhận Language/Level ACTIVE; ID không tồn tại trả `404`.
- [x] `GET /api/levels` và `/api/levels/{id}` với/không có `languageId`.
- [x] `GET /api/courses`: keyword, languageId, levelId, sort, direction và phân trang.
- [x] `GET /api/courses/slug/{slug}`: controller dùng truy vấn Course ACTIVE + PUBLISHED và map thiếu dữ liệu thành `404`.
- [x] `GET /api/courses/{id}/sections` và `/api/sections/{id}/contents`.
- [ ] Nội dung preview được xem công khai; nội dung khóa yêu cầu enrollment CONFIRMED + PAID.
- [ ] `GET /api/classes` và `/api/classes/{id}` chỉ trả lớp thuộc khóa ACTIVE + PUBLISHED.
- [x] Bộ lọc lớp theo course, level, ngày khai giảng, sort và pagination; ngày sai định dạng trả `400`.
- [x] `GET /api/teachers`: controller chỉ gọi truy vấn Teacher ACTIVE.
- [x] `GET /api/rooms`: không làm lộ dữ liệu quản trị hoặc meeting URL.

## 5. Admin catalog và user management

- [x] CRUD `/api/admin/languages`; trùng mã/tên trả `409`, có Level thì không được xóa.
- [x] PATCH status Language; kiểm tra ảnh hưởng tới dữ liệu Public.
- [x] CRUD `/api/admin/levels`; trùng code/display order trong cùng Language.
- [x] Không tạo Level cho Language INACTIVE và không xóa Level đã có Course.
- [x] CRUD `/api/admin/rooms`; trùng mã, capacity không hợp lệ và phòng đã có lịch.
- [x] CRUD `/api/admin/courses`; trùng code/slug, publication status và dữ liệu không hợp lệ.
- [x] `/api/admin/users`: tìm kiếm, lọc role/status, phân trang và xem chi tiết.
- [x] PATCH `/api/admin/users/{id}/status`: ADMIN kích hoạt Teacher và khóa tài khoản.
- [x] Mọi endpoint `/api/admin/**` trả `401` khi chưa login và `403` cho STUDENT/TEACHER.

## 6. Quản lý lớp và lịch cố định

- [x] CRUD `/api/admin/classes`: validate course, ngày, học phí, sĩ số và mã lớp trùng.
- [x] Gán Teacher chỉ khi Teacher ACTIVE.
- [x] Chuyển trạng thái lớp theo transition hợp lệ; chỉ ADMIN được PATCH status.
- [x] Không OPEN lớp khi thiếu Teacher hoặc chưa có lịch cố định hợp lệ.
- [x] `GET/POST /api/classes/{classId}/schedules`.
- [x] `PUT/DELETE /api/schedules/{id}` và chặn sửa/xóa sau khi đã sinh lesson.
- [x] Phát hiện trùng giờ trong cùng lớp.
- [x] Phát hiện trùng phòng khi khoảng ngày hai lớp giao nhau.
- [x] Phát hiện trùng lịch Teacher khi khoảng ngày hai lớp giao nhau.
- [x] Khi cập nhật phải loại trừ chính schedule hiện tại.
- [x] Lớp `CANCELLED` không gây xung đột tài nguyên.
- [x] ONLINE bắt buộc meeting URL và không có room; IN_PERSON bắt buộc room.
- [x] Chỉ ADMIN tạo, sửa, xóa lịch; role khác trả `403`.

## 7. Lesson

- [x] `POST /api/classes/{classId}/lessons/generate`: sinh đúng ngày theo lịch cố định.
- [x] Không sinh trước ngày khai giảng và không tạo lesson trùng.
- [x] Tổng lesson không vượt `totalSessions` và nằm trong start/end date của lớp.
- [x] Teacher chỉ sinh/xem/sửa lesson của lớp được phân công.
- [x] `GET /api/classes/{classId}/lessons`: kiểm tra quyền thành viên lớp.
- [x] `PUT /api/lessons/{id}`: cập nhật topic; meeting URL dùng chung từ lịch cố định.
- [x] `PATCH /api/lessons/{id}/reschedule`: chỉ ADMIN, không có attendance.
- [x] Dời lịch kiểm tra trùng phòng/Teacher theo ngày thực tế và loại trừ lesson hiện tại.
- [x] `PATCH /api/lessons/{id}/cancel`: chỉ role quản lý và không hủy sai trạng thái.
- [x] Scheduler tự chuyển lesson kết thúc sang COMPLETED theo timezone Việt Nam.

## 8. Enrollment

- [x] `POST /api/enrollments`: Student tự đăng ký lớp OPEN còn chỗ.
- [x] Giữ chỗ tạo enrollment đúng status và deadline thanh toán hai ngày.
- [x] Chặn đăng ký trùng và chặn khi lớp FULL/CANCELLED/đã khai giảng không hợp lệ.
- [x] Chặn Student INACTIVE và phát hiện xung đột lịch với lớp đang giữ chỗ/học.
- [x] Concurrent enrollment không làm vượt sĩ số (kiểm tra pessimistic lock).
- [x] `POST /api/staff/enrollments`: ADMIN/CONSULTANT đăng ký bằng email Student.
- [x] Email không tồn tại hoặc không phải Student trả lỗi phù hợp.
- [x] `GET /api/students/me/enrollments`: chỉ trả enrollment của chính Student.
- [x] Danh sách courses/classes/schedules chỉ cấp quyền khi CONFIRMED + PAID.
- [x] `POST /api/enrollments/{id}/cancel-request`: chính chủ, đúng chính sách hủy.
- [x] PATCH status của Staff chỉ cho transition nghiệp vụ được phép.
- [x] Chuyển lớp chỉ sang lớp khác của cùng Course, còn chỗ và không xung đột lịch.
- [x] Chuyển lớp khóa hai lớp theo thứ tự để tránh deadlock và cập nhật FULL/OPEN đúng.
- [x] Job hết hạn hủy enrollment quá hạn và giải phóng chỗ.

## 9. Payment, invoice và refund

- [x] `POST /api/payments` và `/api/enrollments/{id}/payments`: chỉ chính chủ Student.
- [ ] Chỉ cho thanh toán enrollment còn PENDING và chưa quá deadline.
- [ ] Tạo request MoMo/ZaloPay đúng chữ ký nhưng không gọi sandbox thật trong test.
- [ ] Gateway timeout/từ chối: payment FAILED hoặc giữ PENDING theo nghiệp vụ hiện tại.
- [ ] MoMo IPN hợp lệ cập nhật payment PAID và enrollment CONFIRMED + PAID atomically.
- [ ] ZaloPay callback hợp lệ thực hiện cùng quy tắc.
- [ ] Callback sai chữ ký/sai số tiền/giao dịch không tồn tại không cập nhật enrollment.
- [ ] Callback lặp lại phải idempotent; chỉ một payment PAID cho mỗi enrollment.
- [x] `GET /api/students/me/payments`, lịch sử từng enrollment và tra cứu transaction.
- [x] `GET /api/enrollments/{id}/invoice` và `.pdf`: đúng chủ sở hữu/staff, chỉ dữ liệu hợp lệ.
- [x] PDF trả `application/pdf`, filename và nội dung không rỗng.
- [ ] Staff tạo refund cho payment PAID với idempotency key duy nhất.
- [ ] Refund COMPLETED mới cập nhật enrollment payment status REFUNDED/quyền học.
- [ ] Refund FAILED không làm mất quyền học; timeout giữ PENDING để đối soát.
- [ ] Không hoàn vượt số tiền đã trả hoặc hoàn trùng.
- [x] Chỉ ADMIN/CONSULTANT gọi `/api/staff/refunds/**`.

## 10. Attendance

- [x] Teacher lấy sheet chỉ cho lesson thuộc lớp mình phụ trách.
- [ ] Sheet chỉ chứa enrollment CONFIRMED + PAID.
- [ ] Bulk attendance tạo/cập nhật toàn bộ trong một transaction.
- [ ] Một item sai làm rollback toàn bộ batch.
- [x] PATCH một attendance kiểm tra đúng lesson, Student và Teacher phụ trách.
- [ ] Không điểm danh lớp/lesson CANCELLED.
- [ ] Cho cập nhật trong tối đa bảy ngày sau ngày học và chặn sau thời hạn.
- [ ] Sau giờ kết thúc lesson chuyển COMPLETED nhưng attendance vẫn cập nhật trong hạn.
- [x] Student chỉ xem attendance của chính mình.
- [ ] Summary bỏ lesson CANCELLED, phân biệt total/completed/marked và không chia cho 0.

## 11. Dashboard và báo cáo

- [ ] Summary trả đúng tổng Student, Teacher, Course, lớp, enrollment và doanh thu.
- [ ] Revenue report trừ refund COMPLETED và nhóm đúng theo tháng.
- [ ] Enrollment report phân biệt pending/confirmed/paid/cancelled.
- [ ] Popular courses tôn trọng khoảng ngày và giới hạn kết quả.
- [ ] Teacher load không tính lớp/lesson CANCELLED.
- [ ] Upcoming classes dùng timezone/ngày hiện tại đúng và validate khoảng ngày.
- [x] ADMIN truy cập toàn bộ report; CONSULTANT chỉ truy cập endpoint được cấp quyền.

## 12. Upload ảnh

- [x] ADMIN upload ảnh Course; STUDENT upload avatar đúng purpose.
- [x] TEACHER/CONSULTANT và purpose không hợp lệ bị từ chối theo cấu hình hiện tại.
- [ ] File rỗng, quá dung lượng hoặc MIME không phải ảnh trả `400/413` phù hợp.
- [ ] Cloudinary lỗi được chuyển thành response an toàn, không lộ credential.
- [x] Upload thành công trả URL, public ID, kích thước và định dạng đúng.

## 13. Repository integration bằng MySQL

- [ ] `ClassScheduleRepository.existsClassTimeConflict` phát hiện overlap biên chính xác.
- [ ] `existsResourceConflict/existsConflict` kiểm tra room và Teacher.
- [ ] Query loại trừ schedule hiện tại và bỏ lớp CANCELLED.
- [ ] `LessonRepository.existsResourceConflictOnDate` kiểm tra ngày học thực tế.
- [ ] `EnrollmentRepository.existsScheduleConflict` kiểm tra khoảng ngày và giờ giao nhau.
- [ ] Query accessible course/class/schedule chỉ nhận CONFIRMED + PAID và dữ liệu published.
- [ ] Pessimistic lock enrollment/class hoạt động trong transaction cạnh tranh.
- [ ] Native query dashboard, revenue, refund, popular course và teacher load trả đúng kiểu/số liệu.
- [ ] Unique/check constraint quan trọng của payment, refund và enrollment được xác nhận trên MySQL.

## 14. Security matrix chung

- [x] Public endpoint hoạt động không cần token.
- [ ] Protected endpoint thiếu token hoặc token hỏng/hết hạn trả JSON `401` thống nhất.
- [x] Token hợp lệ nhưng sai role trả JSON `403` thống nhất.
- [ ] ADMIN không mặc nhiên truy cập endpoint chỉ dành riêng cho STUDENT/TEACHER.
- [ ] CONSULTANT chỉ có quyền lớp, enrollment, refund và report được khai báo.
- [x] TEACHER không được quản trị catalog/payment/refund.
- [x] STUDENT không đọc/sửa dữ liệu của Student khác bằng cách đổi path ID.
- [ ] Callback payment public vẫn bắt buộc chữ ký gateway hợp lệ.
- [x] CORS OPTIONS và allowed origins hoạt động đúng cấu hình.

## 15. Thứ tự triển khai đề xuất

1. Test profile và MySQL Testcontainers.
2. Security matrix và authentication.
3. Enrollment + capacity/concurrency.
4. Payment callback + refund + invoice.
5. Class schedule conflict + Lesson.
6. Attendance.
7. Admin catalog, dashboard và upload.
8. Chạy `mvn verify`, xem JaCoCo và bổ sung các nhánh còn thiếu.

## 16. Trạng thái hiện tại

- Integration/controller test hiện có **11 lớp**, bao phủ authentication/profile, public/admin
  catalog, lớp/lịch học, lesson, enrollment, payment/refund/invoice, attendance,
  dashboard/upload và security matrix.
- Tổng số test backend ở lần `mvn test` gần nhất: **362**; **0 failure**, **0 error**,
  **1 skipped** (mail smoke test chỉ chạy khi được bật rõ ràng).
- Riêng bốn lớp vừa bổ sung/mở rộng có **45 test**, tất cả đều pass.
- Các Unit Test hiện có không được đánh dấu thay cho Integration Test trong tài liệu này.
