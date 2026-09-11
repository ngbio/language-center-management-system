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
- [x] Login thành công trả đúng UserResponse.
- [x] Sai mật khẩu trả Unauthorized.
- [x] Tài khoản INACTIVE hoặc LOCKED không được login.
- [x] Login Admin chỉ chấp nhận role ADMIN.
- [x] Login Staff chỉ chấp nhận role CONSULTANT; ADMIN dùng cổng đăng nhập Admin riêng.
- [x] Đăng ký Student chuẩn hóa email/username và encode password.
- [x] Đăng ký Student tạo đồng thời User và Student.
- [x] Email trùng bị từ chối trước khi save.
- [x] Username trùng bị từ chối trước khi save.
- [x] Không tìm thấy role STUDENT thì rollback bằng exception.
- [x] Sinh student code không trùng.
- [x] Đăng ký Teacher tạo status chờ kích hoạt và Teacher record.
- [x] Sinh teacher code không trùng.
- [x] Principal null/rỗng bị từ chối ở các API hồ sơ.
- [x] Lấy và cập nhật Student profile đúng dữ liệu.
- [x] Cập nhật profile trim chuỗi và chuyển chuỗi rỗng thành null.
- [x] Password mới trùng password cũ bị từ chối.
- [x] Confirm password không khớp bị từ chối.
- [x] Search user chuẩn hóa keyword/role/status, sort và pagination.
- [x] Status filter không hợp lệ bị từ chối.
- [x] Đổi trạng thái user không tồn tại trả ResourceNotFound.

## 4. TeacherServiceImpl

- [x] Danh sách Teacher ACTIVE được map đúng.
- [x] Lấy profile Teacher từ Principal thành công.
- [x] Principal null bị từ chối.
- [x] Principal có name rỗng bị từ chối.
- [x] Không có hồ sơ Teacher trả ResourceNotFound.
- [x] Cập nhật profile trim dữ liệu, chuyển blank thành null và lưu updatedAt.
- [x] Cập nhật experienceYears khác null giữ đúng giá trị.
- [x] Repository save lỗi được truyền ra và không trả response sai.

## 5. LanguageServiceImpl

- [x] Tạo Language mới chuẩn hóa code/name và lưu.
- [x] Trùng language code bị từ chối.
- [x] Không xóa Language đã có Level.
- [x] Xóa Language chưa có Level thành công.
- [x] Trùng language name bị từ chối.
- [x] Update Language giữ đúng ID và kiểm tra duplicate loại trừ chính nó.
- [x] Status filter không hợp lệ bị từ chối.
- [x] Lấy Language không tồn tại trả ResourceNotFound.
- [x] Public get active không trả dữ liệu INACTIVE.
- [x] Change status cập nhật và map response đúng.

## 6. LevelServiceImpl

- [x] Tạo Level hợp lệ chuẩn hóa code/name/description.
- [x] Trùng code trong cùng Language bị từ chối.
- [x] Trùng display order trong cùng Language bị từ chối.
- [x] Không tạo Level cho Language thiếu hoặc không ACTIVE.
- [x] Status filter không hỗ trợ bị từ chối.
- [x] Level không tồn tại trả ResourceNotFound.
- [x] Không xóa Level đã có Course.
- [x] Cùng level code nhưng khác Language được phép.
- [x] Update cùng Language không truy vấn lại active Language không cần thiết.
- [x] Đổi Language khi update phải kiểm tra Language đích ACTIVE.
- [x] Delete Level không có Course thành công.
- [x] Get active loại bỏ Level hoặc Language INACTIVE.

## 7. RoomServiceImpl

- [x] Tạo Room chuẩn hóa code/name và status.
- [x] Room code trùng bị từ chối.
- [x] Không xóa Room đã có ClassSchedule.
- [x] Room không tồn tại trả ResourceNotFound.
- [x] Update Room kiểm tra duplicate loại trừ chính nó.
- [x] Status filter không hợp lệ bị từ chối.
- [x] Xóa Room chưa được sử dụng thành công.
- [x] Get all theo ACTIVE/MAINTENANCE/INACTIVE gọi đúng repository.

## 8. CourseServiceImpl

- [x] Search chuẩn hóa keyword, status, languageId, levelId, sort và direction.
- [x] Sort field/direction không hợp lệ bị từ chối hoặc dùng mặc định đúng thiết kế.
- [x] Get published by slug chỉ nhận ACTIVE + PUBLISHED.
- [x] Course/slug không tồn tại trả ResourceNotFound.
- [x] Tạo Course hợp lệ chuẩn hóa code, slug và text optional.
- [x] Course code trùng bị từ chối.
- [x] Slug trùng bị từ chối.
- [x] Update duplicate check loại trừ chính Course hiện tại.
- [x] Publication status PUBLISHED xử lý publishedAt đúng.
- [x] Status/publication status không hợp lệ bị từ chối.
- [x] Không xóa Course đã có Class hoặc Section theo ràng buộc hiện tại.

## 9. CourseCurriculumServiceImpl và mapper

- [x] Trả danh sách section/content PUBLISHED đúng thứ tự.
- [x] Nội dung preview được trả khi chưa đăng nhập.
- [x] Nội dung không preview chỉ trả khi Student CONFIRMED + PAID.
- [x] Principal không phải Student không được nhận nội dung khóa.
- [x] Section/Course không tồn tại trả ResourceNotFound.
- [x] CourseCurriculumMapper map đầy đủ media URL, content type và preview; publication được lọc ở repository.

## 10. CourseClassServiceImpl

- [x] Search public chỉ trả lớp hợp lệ và map pagination.
- [x] Search admin chuẩn hóa filter/sort/direction.
- [x] Tạo lớp kiểm tra mã trùng, ngày, sĩ số và học phí.
- [x] Update lớp loại trừ chính ID khi kiểm tra class code.
- [x] Gán Teacher ACTIVE thành công.
- [x] Teacher thiếu/INACTIVE/LOCKED bị từ chối.
- [x] Không OPEN lớp thiếu Teacher hoặc lịch.
- [x] OPEN lớp phát hiện xung đột nội bộ giữa các schedule.
- [x] OPEN lớp phát hiện trùng phòng hoặc lịch Teacher.
- [x] FULL chỉ phù hợp khi đủ sĩ số theo rule hiện tại.
- [x] Change status khóa class trước khi cập nhật.
- [x] Teacher chỉ lấy được classes/courses mình phụ trách.
- [x] Principal Teacher không hợp lệ trả Unauthorized/ResourceNotFound.

## 11. ClassScheduleServiceImpl

- [x] Tạo schedule IN_PERSON bắt buộc Room ACTIVE và không có meeting URL.
- [x] Tạo schedule ONLINE bắt buộc meeting URL và không gắn Room.
- [x] Start time phải nhỏ hơn end time.
- [x] Day of week phải nằm trong phạm vi hợp lệ.
- [x] Phát hiện trùng giờ trong cùng lớp.
- [x] Phát hiện trùng Room và Teacher từ repository.
- [x] Update conflict loại trừ chính schedule hiện tại.
- [x] Không sửa lịch của lớp CANCELLED/COMPLETED theo rule hiện tại.
- [x] Không sửa/xóa lịch sau khi đã sinh Lesson.
- [x] Schedule/Class/Room không tồn tại trả exception phù hợp.
- [x] Create/update/delete khóa pessimistic Class/Schedule để tuần tự hóa request đồng thời.

## 12. LessonServiceImpl và scheduler

- [x] Generate lesson đúng các thứ cố định và khoảng ngày lớp.
- [x] Không generate trước ngày khai giảng.
- [x] Không tạo lesson trùng schedule/date.
- [x] Không vượt totalSessions.
- [x] Teacher chỉ thao tác lớp mình phụ trách.
- [x] Update topic trim đúng; meeting URL lấy từ ClassSchedule.
- [x] Reschedule chỉ trong khoảng ngày lớp và không trùng tài nguyên.
- [x] Không reschedule/cancel lesson đã có attendance.
- [x] Không thay đổi lesson của class CANCELLED/COMPLETED.
- [x] Scheduler chỉ hoàn thành lesson SCHEDULED/IN_PROGRESS đã qua end time.
- [x] Scheduler dùng timezone được cấu hình.
- [x] Scheduler không save khi chưa có lesson kết thúc.
- [x] LessonMapper map ngày, giờ, room và meeting URL đúng.
- [x] Generate khóa pessimistic Class; update/reschedule/cancel khóa pessimistic Lesson.

## 13. EnrollmentServiceImpl và expiration

- [x] Student tự enroll thành công và deadline bằng hai ngày theo rule hiện tại.
- [x] Staff enroll bằng email Student thành công.
- [x] Student/User INACTIVE bị từ chối.
- [x] Lớp không OPEN hoặc đã FULL bị từ chối.
- [x] Enrollment trùng bị từ chối.
- [x] Xung đột lịch Student bị từ chối.
- [x] Đếm capacity chỉ gồm các enrollment status hợp lệ.
- [x] Khi đủ chỗ cuối cùng, class chuyển FULL.
- [x] Request cancel chỉ cho chính chủ và đúng thời hạn/chính sách.
- [x] Staff change status chỉ chấp nhận transition hợp lệ.
- [x] Transfer chỉ sang lớp cùng Course.
- [x] Transfer kiểm tra capacity và schedule conflict.
- [x] Transfer khóa hai class theo thứ tự ID để tránh deadlock.
- [x] Transfer mở lại lớp nguồn và cập nhật FULL cho lớp đích khi cần.
- [x] Search Staff enrollment validate filter/page/size và áp dụng sort/pagination.
- [x] Student chỉ lấy enrollment, course, class và schedule của chính mình.
- [x] Danh sách học viên trong lớp chỉ cho ADMIN/CONSULTANT hoặc Teacher được phân công.
- [x] Lấy chi tiết enrollment cho Staff xử lý đúng trường hợp tồn tại/không tồn tại.
- [x] Enrollment quá hạn bị CANCELLED và lớp FULL được mở lại.
- [x] Enrollment chưa đến deadline không bị hủy.
- [x] ID enrollment không tồn tại được expiration job bỏ qua an toàn.
- [x] Expiration chỉ xử lý CONFIRMED + payment PENDING.
- [x] EnrollmentMapper map deadline/status/payment status đúng.

## 14. PaymentServiceImpl

- [x] Chỉ chính Student tạo payment cho enrollment của mình.
- [x] Enrollment quá deadline hoặc sai trạng thái bị từ chối.
- [x] Số tiền được chuyển sang đơn vị gateway đúng.
- [x] Thiếu cấu hình MoMo/ZaloPay báo lỗi rõ nhưng không lộ secret.
- [x] Callback URL localhost/non-HTTPS bị từ chối theo rule hiện tại.
- [x] Chỉ lưu Payment PENDING sau khi gateway tạo URL thanh toán thành công.
- [x] MoMo request có raw signature đúng thứ tự trường.
- [x] ZaloPay request có MAC đúng.
- [x] Gateway HTTP/network error không tạo Payment PENDING và được bọc thành lỗi an toàn.
- [x] MoMo IPN sai chữ ký không cập nhật payment/enrollment.
- [x] ZaloPay callback sai MAC không cập nhật dữ liệu.
- [x] Callback thành công cập nhật Payment PAID và Enrollment CONFIRMED + PAID.
- [x] Callback thất bại lưu Payment FAILED nhưng Enrollment vẫn PENDING.
- [x] Callback lặp lại idempotent.
- [x] Sai số tiền không được complete payment.
- [x] Constant-time signature comparison xử lý null/invalid input.

## 15. BillingServiceImpl, refund và invoice

- [x] Owner hoặc Staff xem được payment/refund; người khác bị Forbidden.
- [x] Staff validation chỉ chấp nhận ADMIN/CONSULTANT.
- [x] Chỉ refund Payment PAID thuộc enrollment đã thanh toán.
- [x] Idempotency key trùng trả kết quả cũ hoặc bị từ chối đúng rule.
- [x] Không refund vượt số tiền còn lại.
- [x] MoMo/ZaloPay refund request có đủ dữ liệu và signature/MAC.
- [x] Refund thành công chuyển COMPLETED và cập nhật quyền học.
- [x] Refund bị từ chối lưu FAILED, không đổi quyền học.
- [x] Timeout giữ PENDING để refresh/đối soát.
- [x] Refresh refund gọi đúng gateway theo PaymentMethod.
- [x] Invoice tính paid/refunded/net amount đúng.
- [x] Không tìm thấy Enrollment/Payment/Refund trả exception phù hợp.

## 16. InvoicePdfServiceImpl

- [x] Gọi BillingService lấy invoice đúng enrollment/principal.
- [x] PDF sinh ra không rỗng và bắt đầu bằng header `%PDF`.
- [x] Hiển thị đúng mã hóa đơn, Student, Course, Class và tiền.
- [x] Amount/date null được format an toàn.
- [x] Font không tồn tại hoặc lỗi PDF được xử lý đúng.

## 17. AttendanceServiceImpl

- [x] Chỉ Teacher được phân công mới lấy/lưu sheet.
- [x] Class hoặc Lesson CANCELLED bị từ chối.
- [x] Sheet chỉ chứa enrollment CONFIRMED + PAID.
- [x] Bulk attendance tạo record mới đúng status/note/time.
- [x] Bulk update dùng attendance hiện có và cập nhật updatedAt.
- [x] Không thay đổi attendanceTime khi sửa.
- [x] Student không thuộc lớp bị từ chối.
- [x] Trùng Student trong request bị từ chối.
- [x] Item lỗi không gọi saveAll.
- [x] PATCH attendance chỉ cho Teacher phụ trách.
- [x] Chặn update quá bảy ngày sau ngày học.
- [x] Student getMine chỉ trả dữ liệu chính mình.
- [x] Summary đếm PRESENT/LATE/ABSENT/EXCUSED đúng.
- [x] Attendance rate không chia cho 0 và bỏ lesson CANCELLED.

## 18. DashboardServiceImpl

- [x] Summary thay null aggregate bằng zero.
- [x] Validate from không sau to.
- [x] Khoảng ngày mặc định được tính đúng.
- [x] Revenue nhóm YearMonth và trừ refund đúng.
- [x] Enrollment report map các cột native query đúng kiểu Number.
- [x] Popular course giới hạn số lượng hợp lệ.
- [x] Teacher load map active classes/generated/completed lessons đúng.
- [x] Upcoming classes map Teacher null và ngày đúng.

## 19. CloudinaryImageUploadService

- [x] File null/rỗng bị từ chối.
- [x] File vượt max size bị từ chối.
- [x] MIME type ngoài whitelist ảnh bị từ chối.
- [x] Purpose được normalize và kiểm tra whitelist.
- [x] ADMIN/STUDENT được upload đúng purpose theo rule hiện tại.
- [x] Role không có quyền bị Forbidden.
- [x] Principal hoặc User không tồn tại bị từ chối.
- [x] Upload options gửi folder/resource type đúng.
- [x] Cloudinary response map URL/publicId/width/height/bytes đúng kiểu.
- [x] Cloudinary exception được bọc thành lỗi nghiệp vụ an toàn.

## 20. Security, JWT và exception handler

- [x] JwtFilter không có Authorization header thì tiếp tục filter chain.
- [x] Authorization header sai định dạng trả Unauthorized và không chạy chain.
- [x] Bearer token hợp lệ thiết lập Authentication và authorities đúng.
- [x] Token hết hạn/sai chữ ký trả JSON 401.
- [x] User từ token không tồn tại hoặc không ACTIVE bị từ chối.
- [x] SecurityContext không bị ghi đè khi đã xác thực.
- [x] JwtUtils generate/parse subject thành công.
- [x] JwtUtils từ chối secret quá ngắn, token hết hạn và token bị sửa.
- [x] Domain exception được map sang 400/401/403/404/409/502 đúng.
- [x] JSON enum/body không đọc được trả 400 và không lộ parser detail.
- [x] File quá lớn trả 413.
- [x] Unexpected exception trả 500 với message tổng quát.
- [x] Validation errors được distinct và ghép đúng field/message.
- [x] Thiếu request part/parameter và type mismatch trả 400.

## 21. SystemLogServiceImpl

- [x] Validate page, size, khoảng ngày và log level.
- [x] Search chuẩn hóa filter, sắp xếp `createdAt DESC` và map response.
- [x] HTTP 401/403 tạo WARN + AUTHENTICATION_FAILURE.
- [x] HTTP 5xx tạo ERROR + HTTP_REQUEST_FAILED.
- [x] Lỗi lưu SystemLog không làm hỏng request nghiệp vụ chính.

## 22. Controller unit test

Controller chỉ cần Unit Test khi có logic ngoài việc gọi service và đóng gói response.
Các kiểm tra HTTP mapping, validation và role ưu tiên đặt ở Integration Test.

- [x] ApiUserController login tạo JWT và trả response nhất quán.
- [x] AdminLanguageApiController create xóa ID do client cung cấp và trả 201.
- [x] Không có controller cần lặp lại nhánh Principal null; authorization được kiểm tra ở service/security test.
- [x] Controller tạo header PDF filename/content type được kiểm tra riêng.
- [x] Không tạo Unit Test lặp lại cho controller chỉ chuyển tiếp một lời gọi service.

## 23. Thứ tự triển khai đề xuất

1. [x] EnrollmentServiceImpl và ClassScheduleServiceImpl.
2. [x] PaymentServiceImpl và BillingServiceImpl.
3. [x] AttendanceServiceImpl và LessonServiceImpl.
4. [x] CourseClassServiceImpl và CourseServiceImpl.
5. [x] UserServiceImpl.
6. [x] Dashboard, Cloudinary, PDF, JWT, SystemLog và mapper có logic.
7. [x] Chạy `mvn verify`, đọc JaCoCo theo class và bổ sung branch quan trọng còn thiếu.
