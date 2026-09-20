# Kiểm tra giao diện frontend trên điện thoại

Ngày kiểm tra: 20/09/2026.

## Kết luận

Frontend đã có responsive cho các nhóm Public, Student, Teacher, Admin và Staff, nhưng chưa hoàn chỉnh. Có lỗi cắt nội dung ở bảng thông báo, quản lý đăng ký, điểm danh giảng viên và nhật ký hệ thống. Việc đóng APK không tự khắc phục các lỗi CSS này.

Đây là báo cáo kiểm tra; chưa sửa mã giao diện trong đợt kiểm tra này.

## Phương pháp và giới hạn

- Đọc `docs/ai-context/frontend.md`, tài liệu kiểm thử, route và CSS/component liên quan.
- Duyệt tự động 45 đường dẫn ở chiều rộng 320, 390, 768 và 1280px bằng Chromium/Playwright với API giả lập. Kiểm tra thêm trạng thái điểm danh có học viên ở 820 và 1024px.
- Đo vị trí phần tử vượt khung, nội dung bị cắt và lỗi JavaScript; xem ảnh các trường hợp nghi ngờ. Không chỉ kiểm tra `document.scrollWidth`, vì `overflow-x: hidden` có thể che lỗi.
- Năm route quản trị ban đầu dùng sai định dạng dữ liệu giả lập; đã sửa fixture phân trang và chạy lại 20 trường hợp. Những lỗi do fixture ban đầu không được tính là lỗi ứng dụng.
- Phần lớn danh sách dùng dữ liệu rỗng, một số trang chi tiết có dữ liệu mẫu. Chưa kiểm hết modal, dữ liệu dài, tất cả form thêm/sửa, flashcard/quiz, chat có tin nhắn, lỗi backend, bàn phím ảo hoặc xoay màn hình. Không xác nhận tương thích Safari/iPhone hay APK trên thiết bị thật.
- Route kết quả thanh toán trong lượt quét khách chuyển đến đăng nhập; đã kiểm tra riêng trạng thái học viên ở 390px, chưa kiểm đầy đủ các kết quả giao dịch.

## Lỗi đã xác nhận

| Mức độ | Màn hình | Bằng chứng | Vị trí cần xem |
| --- | --- | --- | --- |
| Cao | Bảng thông báo học viên | Ở 390px, panel rộng 362px nhưng tọa độ trái là -103px; phần đầu và nội dung bên trái nằm ngoài màn hình. | `frontend/src/styles/PublicSite.css:37`, `.notification-panel` |
| Cao | Admin/Staff quản lý đăng ký | Ở 768px, form email và nút Xếp vào lớp có mép phải tại 895px, vượt viewport 127px. | `frontend/src/App.css:982` và `:985`, `.enrollment-admin-controls` |
| Vừa | Điểm danh giảng viên có học viên | Ở 1024px, hàng điểm danh cần 636px nhưng chỉ có 592px; nút Cập nhật riêng bị khung chi tiết cắt bên phải. | `frontend/src/styles/Attendance.css:62`, `.attendance-student-row`; khung cha có `overflow: hidden` |
| Vừa | Admin nhật ký hệ thống | Ở 1280px, bộ lọc Đến kéo tới 1323px; panel cắt nội dung. Đây là lỗi responsive ở màn rộng, cũng cần sửa khi rà soát tổng thể. | `frontend/src/App.css:997`, `.system-log-filters` |

Ảnh bằng chứng cục bộ, trong thư mục bị Git bỏ qua:

- `frontend/test-results/audit-notifications-390.png`
- `frontend/test-results/audit-768-_admin_enrollments.png`
- `frontend/test-results/audit-768-_staff_enrollments.png`
- `frontend/test-results/audit-teacher-1024.png`
- `frontend/test-results/audit-1280-_admin_system_logs.png`

## Phạm vi màn hình đã rà soát

Các route dưới đây đã được đọc cấu trúc và mở trong lượt quét cơ bản; không đồng nghĩa mọi trạng thái nghiệp vụ đều đạt.

| Nhóm | Màn hình/đường dẫn |
| --- | --- |
| Public | `/`, `/khoa-hoc`, `/ngon-ngu`, `/ngon-ngu/:id`, `/trinh-do/:id`, `/lop-hoc`, `/lop-hoc/:id`, `/khoa-hoc/:slug`, `/on-tap/:courseId` |
| Đăng nhập/tài khoản khách | `/login`, `/register`, `/forgot-password`, `/reset-password`, `/admin/login`, `/staff/login` |
| Student | `/khoa-hoc-cua-toi`, `/lop-hoc-cua-toi`, `/lich-su-dang-ky`, `/thong-tin-ca-nhan`, `/diem-danh`, `/doi-mat-khau`, `/thanh-toan/ket-qua` |
| Teacher | `/giao-vien/khoa-hoc`, `/giao-vien/lop-hoc`, `/giao-vien/thong-tin-ca-nhan`, `/giao-vien/diem-danh` |
| Admin | `/admin`, `/admin/users`, `/admin/languages`, `/admin/levels`, `/admin/courses`, `/admin/courses/:courseId/curriculum`, `/admin/courses/:courseId/contents/:contentId/practice`, `/admin/rooms`, `/admin/classes`, `/admin/enrollments`, `/admin/refunds`, `/admin/system-logs`, `/admin/profile`, `/admin/change-password` |
| Staff | `/staff/enrollments`, `/staff/refunds`, `/staff/chat`, `/staff/profile`, `/staff/change-password` |

Các phần responsive đã hiện diện: menu mobile, sidebar quản trị dạng mở/đóng, lưới thẻ chuyển cột, form chuyển một cột ở breakpoint nhỏ, bảng có vùng cuộn ngang, safe area cho native. Ngoài các lỗi nêu trên, lượt quét cơ bản chưa phát hiện lỗi vượt viewport đáng kể ở nội dung chính; hình trang trí tràn có chủ đích không được tính là lỗi.

## Khoảng trống kiểm thử cần bổ sung

`frontend/e2e/mobile-layout.spec.js` hiện chỉ kiểm tra trang chủ và dashboard Admin tại ba kích thước. Sáu test này không chứng minh mọi màn hình đã hỗ trợ điện thoại tốt.

Ưu tiên tiếp theo:

1. Sửa bốn lỗi đã tái hiện và thêm kiểm thử hồi quy cho từng lỗi.
2. Bổ sung dữ liệu dài và modal thêm/sửa cho lớp, khóa học, người dùng, hoàn tiền và chuyển lớp; kiểm tra trình biên tập nội dung, quiz và flashcard.
3. Kiểm tra chat, bàn phím ảo, menu tài khoản, thông báo nhiều dòng, dark mode và xoay màn hình trên Android/iOS thật.
4. Kiểm tra cỡ chữ form trên web: có trường dùng cỡ chữ dưới 16px; quy tắc tối thiểu 16px hiện chỉ áp dụng dưới `html.capacitor-native`.
5. Sau khi sửa và kiểm chứng, đồng bộ lại Capacitor rồi build APK; không dùng APK cũ để kết luận về giao diện mới.

Kết quả đo cục bộ: `frontend/test-results/mobile-audit.json`, `mobile-audit-recheck.json`, `mobile-interactions.json`. File recheck thay thế kết quả của năm route phân trang; lượt thử tương tác Admin không có kết quả đo vì fixture chưa mô phỏng xác thực Admin, nên bằng chứng Admin lấy từ lượt recheck.
