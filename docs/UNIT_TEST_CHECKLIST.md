# Unit Test Checklist — Backend Spring Boot

## 1. Phạm vi và quy ước

Checklist này theo dõi Unit Test cho các class có logic trong backend hiện tại.

- `[x]`: đã có test tự động và đang chạy thành công.
- `[ ]`: chưa có test hoặc chưa phủ đủ trường hợp được mô tả.
- Unit Test không khởi động Spring Context và không truy cập database/network thật.
- Repository, HTTP gateway, Cloudinary và dependency ngoài phải được mock.
- Không mock class đang được kiểm thử; không dùng `lenient()` để che strict-stubbing error.
- Không viết test riêng cho entity, DTO, enum, getter/setter hoặc configuration thuần túy.
- Repository custom query được theo dõi trong `INTEGRATION_TEST_CHECKLIST.md`, không tính là Unit Test.

## 2. Cấu trúc và nền tảng

- [x] Unit Test nằm trong `src/test/java/.../unit` và chia theo `service`, `controller`, `security`, `exception`.
- [x] Dùng JUnit 5 và Mockito.
- [x] Có thể dùng AssertJ cho assertion dễ đọc.
- [x] JaCoCo được chạy trong Maven `verify`.
- [x] Tạo fixture/builder dùng chung để giảm lặp khi dựng Course, Class, Enrollment, Payment và Lesson.
- [x] Chuẩn hóa tên test theo dạng `shouldExpectedResultWhenCondition`.
- [x] Tách `CatalogServicesTest` thành `LanguageServiceImplTest` và `RoomServiceImplTest` khi số test tăng.

## 3. UserServiceImpl

- [x] Không tìm thấy email khi login thì ném `UnauthorizedException`.
- [x] Đổi mật khẩu hợp lệ thì encode và lưu password hash mới.
- [x] Mật khẩu hiện tại sai thì không encode và không lưu.
- [ ] Login thành công trả đúng UserResponse.
- [ ] Sai mật khẩu trả Unauthorized.
- [ ] Tài khoản INACTIVE hoặc LOCKED không được login.
- [ ] Login Admin chỉ chấp nhận role ADMIN.
- [ ] Login Staff chỉ chấp nhận role CONSULTANT hoặc ADMIN theo nghiệp vụ hiện tại.
- [ ] Đăng ký Student chuẩn hóa email/username và encode password.
- [ ] Đăng ký Student tạo đồng thời User và Student.
- [ ] Email trùng bị từ chối trước khi save.
- [ ] Username trùng bị từ chối trước khi save.
- [ ] Không tìm thấy role STUDENT thì rollback bằng exception.
- [ ] Sinh student code không trùng.
- [ ] Đăng ký Teacher tạo status chờ kích hoạt và Teacher record.
- [ ] Sinh teacher code không trùng.
- [ ] Principal null/rỗng bị từ chối ở các API hồ sơ.
- [ ] Lấy và cập nhật Student profile đúng dữ liệu.
- [ ] Cập nhật profile trim chuỗi và chuyển chuỗi rỗng thành null.
- [ ] Password mới trùng password cũ bị từ chối.
- [ ] Confirm password không khớp bị từ chối.
- [ ] Search user chuẩn hóa keyword/role/status, sort và pagination.
- [ ] Status filter không hợp lệ bị từ chối.
- [ ] Đổi trạng thái user không tồn tại trả ResourceNotFound.

## 4. TeacherServiceImpl

- [x] Danh sách Teacher ACTIVE được map đúng.
- [x] Lấy profile Teacher từ Principal thành công.
- [x] Principal null bị từ chối.
- [x] Principal có name rỗng bị từ chối.
- [x] Không có hồ sơ Teacher trả ResourceNotFound.
- [x] Cập nhật profile trim dữ liệu, chuyển blank thành null và lưu updatedAt.
- [ ] Cập nhật experienceYears khác null giữ đúng giá trị.
- [ ] Repository save lỗi được truyền ra và không trả response sai.

## 5. LanguageServiceImpl

- [x] Tạo Language mới chuẩn hóa code/name và lưu.
- [x] Trùng language code bị từ chối.
- [x] Không xóa Language đã có Level.
- [x] Xóa Language chưa có Level thành công.
- [ ] Trùng language name bị từ chối.
- [ ] Update Language giữ đúng ID và kiểm tra duplicate loại trừ chính nó.
- [ ] Status filter không hợp lệ bị từ chối.
- [ ] Lấy Language không tồn tại trả ResourceNotFound.
- [ ] Public get active không trả dữ liệu INACTIVE.
- [ ] Change status cập nhật và map response đúng.

## 6. LevelServiceImpl

- [x] Tạo Level hợp lệ chuẩn hóa code/name/description.
- [x] Trùng code trong cùng Language bị từ chối.
- [x] Trùng display order trong cùng Language bị từ chối.
- [x] Không tạo Level cho Language thiếu hoặc không ACTIVE.
- [x] Status filter không hỗ trợ bị từ chối.
- [x] Level không tồn tại trả ResourceNotFound.
- [x] Không xóa Level đã có Course.
- [ ] Cùng level code nhưng khác Language được phép.
- [ ] Update cùng Language không truy vấn lại active Language không cần thiết.
- [ ] Đổi Language khi update phải kiểm tra Language đích ACTIVE.
- [ ] Delete Level không có Course thành công.
- [ ] Get active loại bỏ Level hoặc Language INACTIVE.

## 7. RoomServiceImpl

- [x] Tạo Room chuẩn hóa code/name và status.
- [x] Room code trùng bị từ chối.
- [x] Không xóa Room đã có ClassSchedule.
- [x] Room không tồn tại trả ResourceNotFound.
- [ ] Update Room kiểm tra duplicate loại trừ chính nó.
- [ ] Status filter không hợp lệ bị từ chối.
- [ ] Xóa Room chưa được sử dụng thành công.
- [ ] Get all theo ACTIVE/MAINTENANCE/INACTIVE gọi đúng repository.

## 8. CourseServiceImpl

- [ ] Search chuẩn hóa keyword, status, languageId, levelId, sort và direction.
- [ ] Sort field/direction không hợp lệ bị từ chối hoặc dùng mặc định đúng thiết kế.
- [ ] Get published by slug chỉ nhận ACTIVE + PUBLISHED.
- [ ] Course/slug không tồn tại trả ResourceNotFound.
- [ ] Tạo Course hợp lệ chuẩn hóa code, slug và text optional.
- [ ] Course code trùng bị từ chối.
- [ ] Slug trùng bị từ chối.
- [ ] Update duplicate check loại trừ chính Course hiện tại.
- [ ] Publication status PUBLISHED xử lý publishedAt đúng.
- [ ] Status/publication status không hợp lệ bị từ chối.
- [ ] Không xóa Course đã có Class hoặc Section theo ràng buộc hiện tại.

## 9. CourseCurriculumServiceImpl và mapper

- [ ] Trả danh sách section/content PUBLISHED đúng thứ tự.
- [ ] Nội dung preview được trả khi chưa đăng nhập.
- [ ] Nội dung không preview chỉ trả khi Student CONFIRMED + PAID.
- [ ] Principal không phải Student không được nhận nội dung khóa.
- [ ] Section/Course không tồn tại trả ResourceNotFound.
- [ ] CourseCurriculumMapper map đầy đủ media URL, publication và preview.

## 10. CourseClassServiceImpl

- [ ] Search public chỉ trả lớp hợp lệ và map pagination.
- [ ] Search admin chuẩn hóa filter/sort/direction.
- [ ] Tạo lớp kiểm tra mã trùng, ngày, sĩ số và học phí.
- [ ] Update lớp loại trừ chính ID khi kiểm tra class code.
- [ ] Gán Teacher ACTIVE thành công.
- [ ] Teacher thiếu/INACTIVE/LOCKED bị từ chối.
- [ ] Không OPEN lớp thiếu Teacher hoặc lịch.
- [ ] OPEN lớp phát hiện xung đột nội bộ giữa các schedule.
- [ ] OPEN lớp phát hiện trùng phòng hoặc lịch Teacher.
- [ ] FULL chỉ phù hợp khi đủ sĩ số theo rule hiện tại.
- [ ] Change status khóa class trước khi cập nhật.
- [ ] Teacher chỉ lấy được classes/courses mình phụ trách.
- [ ] Principal Teacher không hợp lệ trả Unauthorized/ResourceNotFound.

## 11. ClassScheduleServiceImpl

- [ ] Tạo schedule IN_PERSON bắt buộc Room ACTIVE và không có meeting URL.
- [ ] Tạo schedule ONLINE bắt buộc meeting URL và không gắn Room.
- [ ] Start time phải nhỏ hơn end time.
- [ ] Day of week phải nằm trong phạm vi hợp lệ.
- [ ] Phát hiện trùng giờ trong cùng lớp.
- [ ] Phát hiện trùng Room và Teacher từ repository.
- [ ] Update conflict loại trừ chính schedule hiện tại.
- [ ] Không sửa lịch của lớp CANCELLED/COMPLETED theo rule hiện tại.
- [ ] Không sửa/xóa lịch sau khi đã sinh Lesson.
- [ ] Schedule/Class/Room không tồn tại trả exception phù hợp.

## 12. LessonServiceImpl và scheduler

- [ ] Generate lesson đúng các thứ cố định và khoảng ngày lớp.
- [ ] Không generate trước ngày khai giảng.
- [ ] Không tạo lesson trùng schedule/date.
- [ ] Không vượt totalSessions.
- [ ] Teacher chỉ thao tác lớp mình phụ trách.
- [ ] Update topic/meeting URL trim đúng.
- [ ] Reschedule chỉ trong khoảng ngày lớp và không trùng tài nguyên.
- [ ] Không reschedule/cancel lesson đã có attendance.
- [ ] Không thay đổi lesson của class CANCELLED/COMPLETED.
- [ ] Scheduler chỉ hoàn thành lesson SCHEDULED/IN_PROGRESS đã qua end time.
- [ ] Scheduler dùng timezone được cấu hình.
- [ ] Scheduler không save khi chưa có lesson kết thúc.
- [ ] LessonMapper map ngày, giờ, room và meeting URL đúng.

## 13. EnrollmentServiceImpl và expiration

- [ ] Student tự enroll thành công và deadline bằng hai ngày theo rule hiện tại.
- [ ] Staff enroll bằng email Student thành công.
- [ ] Student/User INACTIVE bị từ chối.
- [ ] Lớp không OPEN hoặc đã FULL bị từ chối.
- [ ] Enrollment trùng bị từ chối.
- [ ] Xung đột lịch Student bị từ chối.
- [ ] Đếm capacity chỉ gồm các enrollment status hợp lệ.
- [ ] Khi đủ chỗ cuối cùng, class chuyển FULL.
- [ ] Request cancel chỉ cho chính chủ và đúng thời hạn/chính sách.
- [ ] Staff change status chỉ chấp nhận transition hợp lệ.
- [ ] Transfer chỉ sang lớp cùng Course.
- [ ] Transfer kiểm tra capacity và schedule conflict.
- [ ] Transfer khóa hai class theo thứ tự ID để tránh deadlock.
- [ ] Transfer mở lại lớp nguồn và cập nhật FULL cho lớp đích khi cần.
- [x] Enrollment quá hạn bị CANCELLED và lớp FULL được mở lại.
- [x] Enrollment chưa đến deadline không bị hủy.
- [x] ID enrollment không tồn tại được expiration job bỏ qua an toàn.
- [ ] Expiration chỉ xử lý CONFIRMED + payment PENDING.
- [ ] EnrollmentMapper map deadline/status/payment status đúng.

## 14. PaymentServiceImpl

- [ ] Chỉ chính Student tạo payment cho enrollment của mình.
- [ ] Enrollment quá deadline hoặc sai trạng thái bị từ chối.
- [ ] Số tiền được chuyển sang đơn vị gateway đúng.
- [ ] Thiếu cấu hình MoMo/ZaloPay báo lỗi rõ nhưng không lộ secret.
- [ ] Callback URL localhost/non-HTTPS bị từ chối theo rule hiện tại.
- [ ] Tạo pending Payment trước khi gọi gateway.
- [ ] MoMo request có raw signature đúng thứ tự trường.
- [ ] ZaloPay request có MAC đúng.
- [ ] Gateway HTTP/network error cập nhật trạng thái theo nghiệp vụ hiện tại.
- [ ] MoMo IPN sai chữ ký không cập nhật payment/enrollment.
- [ ] ZaloPay callback sai MAC không cập nhật dữ liệu.
- [ ] Callback thành công cập nhật Payment PAID và Enrollment CONFIRMED + PAID.
- [ ] Callback thất bại lưu Payment FAILED nhưng Enrollment vẫn PENDING.
- [ ] Callback lặp lại idempotent.
- [ ] Sai số tiền không được complete payment.
- [ ] Constant-time signature comparison xử lý null/invalid input.

## 15. BillingServiceImpl, refund và invoice

- [ ] Owner hoặc Staff xem được payment/refund; người khác bị Forbidden.
- [ ] Staff validation chỉ chấp nhận ADMIN/CONSULTANT.
- [ ] Chỉ refund Payment PAID thuộc enrollment đã thanh toán.
- [ ] Idempotency key trùng trả kết quả cũ hoặc bị từ chối đúng rule.
- [ ] Không refund vượt số tiền còn lại.
- [ ] MoMo/ZaloPay refund request và signature đúng.
- [ ] Refund thành công chuyển COMPLETED và cập nhật quyền học.
- [ ] Refund bị từ chối lưu FAILED, không đổi quyền học.
- [ ] Timeout giữ PENDING để refresh/đối soát.
- [ ] Refresh refund gọi đúng gateway theo PaymentMethod.
- [ ] Invoice tính paid/refunded/net amount đúng.
- [ ] Không tìm thấy Enrollment/Payment/Refund trả exception phù hợp.

## 16. InvoicePdfServiceImpl

- [ ] Gọi BillingService lấy invoice đúng enrollment/principal.
- [ ] PDF sinh ra không rỗng và bắt đầu bằng header `%PDF`.
- [ ] Hiển thị đúng mã hóa đơn, Student, Course, Class và tiền.
- [ ] Amount/date null được format an toàn.
- [ ] Font không tồn tại hoặc lỗi PDF được xử lý đúng.

## 17. AttendanceServiceImpl

- [ ] Chỉ Teacher được phân công mới lấy/lưu sheet.
- [ ] Class hoặc Lesson CANCELLED bị từ chối.
- [ ] Sheet chỉ chứa enrollment CONFIRMED + PAID.
- [ ] Bulk attendance tạo record mới đúng status/note/time.
- [ ] Bulk update dùng attendance hiện có và cập nhật updatedAt.
- [ ] Không thay đổi attendanceTime khi sửa.
- [ ] Student không thuộc lớp bị từ chối.
- [ ] Trùng Student trong request bị từ chối.
- [ ] Item lỗi không gọi saveAll.
- [ ] PATCH attendance chỉ cho Teacher phụ trách.
- [ ] Chặn update quá bảy ngày sau ngày học.
- [ ] Student getMine chỉ trả dữ liệu chính mình.
- [ ] Summary đếm PRESENT/LATE/ABSENT/EXCUSED đúng.
- [ ] Attendance rate không chia cho 0 và bỏ lesson CANCELLED.

## 18. DashboardServiceImpl

- [ ] Summary thay null aggregate bằng zero.
- [ ] Validate from không sau to.
- [ ] Khoảng ngày mặc định được tính đúng.
- [ ] Revenue nhóm YearMonth và trừ refund đúng.
- [ ] Enrollment report map các cột native query đúng kiểu Number.
- [ ] Popular course giới hạn số lượng hợp lệ.
- [ ] Teacher load map active classes/generated/completed lessons đúng.
- [ ] Upcoming classes map Teacher null và ngày đúng.

## 19. CloudinaryImageUploadService

- [ ] File null/rỗng bị từ chối.
- [ ] File vượt max size bị từ chối.
- [ ] MIME type không bắt đầu bằng `image/` bị từ chối.
- [ ] Purpose được normalize và kiểm tra whitelist.
- [ ] ADMIN/STUDENT được upload đúng purpose theo rule hiện tại.
- [ ] Role không có quyền bị Forbidden.
- [ ] Principal hoặc User không tồn tại bị từ chối.
- [ ] Upload options gửi folder/resource type đúng.
- [ ] Cloudinary response map URL/publicId/width/height/bytes đúng kiểu.
- [ ] Cloudinary exception được bọc thành lỗi nghiệp vụ an toàn.

## 20. Security, JWT và exception handler

- [x] JwtFilter không có Authorization header thì tiếp tục filter chain.
- [x] Authorization header sai định dạng trả Unauthorized và không chạy chain.
- [ ] Bearer token hợp lệ thiết lập Authentication và authorities đúng.
- [ ] Token hết hạn/sai chữ ký trả JSON 401.
- [ ] User từ token không tồn tại hoặc không ACTIVE bị từ chối.
- [ ] SecurityContext không bị ghi đè khi đã xác thực.
- [ ] JwtUtils generate/parse subject thành công.
- [ ] JwtUtils từ chối secret quá ngắn, token hết hạn và token bị sửa.
- [x] Domain exception được map sang 400/401/403/404/409/502 đúng.
- [x] JSON enum/body không đọc được trả 400 và không lộ parser detail.
- [x] File quá lớn trả 413.
- [x] Unexpected exception trả 500 với message tổng quát.
- [ ] Validation errors được distinct và ghép đúng field/message.
- [ ] Thiếu request part/parameter và type mismatch trả 400.

## 21. Controller unit test

Controller chỉ cần Unit Test khi có logic ngoài việc gọi service và đóng gói response.
Các kiểm tra HTTP mapping, validation và role ưu tiên đặt ở Integration Test.

- [x] ApiUserController login tạo JWT và trả response nhất quán.
- [x] AdminLanguageApiController create xóa ID do client cung cấp và trả 201.
- [ ] Controller có nhánh Principal null được kiểm tra nếu logic nằm trực tiếp trong controller.
- [ ] Controller tạo header PDF filename/content type được kiểm tra riêng.
- [ ] Không tạo Unit Test lặp lại cho controller chỉ chuyển tiếp một lời gọi service.

## 22. Thứ tự triển khai đề xuất

1. EnrollmentServiceImpl và ClassScheduleServiceImpl.
2. PaymentServiceImpl và BillingServiceImpl.
3. AttendanceServiceImpl và LessonServiceImpl.
4. CourseClassServiceImpl và CourseServiceImpl.
5. UserServiceImpl còn thiếu.
6. Dashboard, Cloudinary, PDF, JWT và mapper có logic.
7. Chạy `mvn verify`, đọc JaCoCo theo class và bổ sung branch còn thiếu.
