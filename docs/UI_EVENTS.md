# Giao diện sự kiện

## Bật/tắt Noel bằng một cấu hình

Trong `frontend/.env`, thêm hoặc sửa:

```dotenv
VITE_UI_EVENT=christmas
```

Để quay lại giao diện thường:

```dotenv
VITE_UI_EVENT=none
```

Khởi động lại Vite sau khi đổi `.env`. Nếu đang deploy, đổi biến ở **nơi build frontend**, rồi build/deploy lại. Cấu hình Vite được đưa vào bundle lúc build; chỉ tải lại trình duyệt hoặc đổi env backend không thay đổi giao diện sự kiện.

Hiện mặc định là `christmas` nếu không khai báo. Muốn tắt, đặt rõ `none`, không chỉ xóa biến. Nguồn quyết định duy nhất: `frontend/src/configs/uiEvent.js`. Không có lịch tự bật/tắt theo ngày.

## Phạm vi

| Khu vực | Trang trí Noel |
| --- | --- |
| Trang chủ | Cây thông/sách/quà, lời chúc, tuyết nhẹ |
| Trang công khai | Banner lời chúc, mũ logo, footer; cành thông ở đầu trang danh mục |
| Khóa học, lớp học, ngôn ngữ, trình độ | Đầu trang có điểm nhấn mùa lễ hội; nội dung giữ nguyên |
| Học viên và giáo viên | Trang khóa/lớp/lịch/điểm danh dùng cùng lớp trang trí ở shell/hero; các trang hồ sơ, thanh toán dùng banner/logo/footer chung |
| Bài học, flashcard, quiz | Lời chúc nhỏ trong sidebar, mũ logo và đường viền vàng; không có tuyết che bài |
| Admin và tư vấn viên | Lời chúc trong sidebar, mũ logo, điểm nhấn topbar/tiêu đề/panel; bảng và thao tác không đổi |
| Đăng nhập, đăng ký, quên/đặt lại mật khẩu | Cành thông ở vùng minh họa, mũ logo và lời chúc dưới form |

Chế độ tối được hỗ trợ. Tuyết chỉ có ở hero trang chủ; nút Bật/Tắt tuyết lưu vào `localStorage.christmasSnow`. Tắt tuyết không tắt theme Noel. Thiết bị bật giảm chuyển động (`prefers-reduced-motion`) không chạy tuyết.

## Những gì được khôi phục khi đặt `none`

- Ẩn banner, mũ, cành thông, lời chúc và tuyết trên mọi màn hình.
- Trang chủ trở về minh họa, mô tả và nhãn giới thiệu trước sự kiện.
- Không áp dụng CSS trang trí Noel cho auth/admin/staff/learning.
- Giữ các cải tiến bố cục chi tiết khóa học và không gian học đã làm trước Noel. Đây là thay đổi giao diện chính, không thuộc sự kiện.
- Không thay đổi API, tài khoản, dữ liệu, thanh toán, đăng ký hay quyền truy cập.

## Các file liên quan

- `frontend/src/configs/uiEvent.js`: đọc cấu hình event; export `uiEvent`, `isChristmas`.
- `frontend/src/main.jsx`: đặt `data-ui-event` trên thẻ `<html>`, nạp stylesheet.
- `frontend/src/styles/Christmas.css`: lớp CSS sự kiện; rule dùng chung được chặn bởi `[data-ui-event="christmas"]`.
- `frontend/src/components/SeasonalAccent.jsx`: lời chúc dùng chung, tự ẩn khi tắt.
- `frontend/src/components/public/ChristmasScene.jsx`: minh họa trang chủ.
- `frontend/src/assets/christmas-sprig.svg`: trang trí cành thông.
- `PublicLayout.jsx`, `HomeScreen.jsx`: render có điều kiện theo cấu hình.
- `AdminLayout.jsx`, `StaffLayout.jsx`, `LearningScreen.jsx`: gắn component lời chúc dùng chung.

## Thêm sự kiện sau này

Thêm một giá trị có tên rõ ràng trong `uiEvent.js` (ví dụ `tet`), component/stylesheet riêng và điều kiện render tương ứng. Không dùng CSS Noel toàn cục để ghi đè một sự kiện khác. Luôn giữ đường quay về `none`.

## Kiểm tra sau khi thay đổi

1. Kiểm tra cả `christmas` và `none`, nhớ restart Vite giữa hai cấu hình.
2. Kiểm tra trang chủ, catalog/chi tiết, đăng nhập/đăng ký, học tập, admin và tư vấn.
3. Kiểm tra mobile 320/390px, tablet, desktop và chế độ tối.
4. Kiểm tra nút tuyết, tải lại để xác nhận lưu lựa chọn và chế độ giảm chuyển động.
5. Chạy build frontend và các kiểm tra giao diện liên quan.

Không cần chạy SQL, sửa database hoặc dùng `git reset` để tắt giao diện sự kiện.

Bộ kiểm tra tự động: `frontend/e2e/ui-event.spec.js`. Với cấu hình Noel, chạy trong `frontend/`:

```powershell
npx.cmd playwright test e2e/ui-event.spec.js --workers=2
```

Sau khi đặt `VITE_UI_EVENT=none` và khởi động lại dev server, chạy:

```powershell
$env:QA_EXPECTED_EVENT='none'
npx.cmd playwright test e2e/ui-event.spec.js --workers=2
Remove-Item Env:QA_EXPECTED_EVENT
```

Mặc định bộ kiểm tra dùng địa chỉ từ `playwright.config.js`. Để kiểm tra server đang chạy ở cổng khác, đặt thêm `QA_EVENT_BASE_URL` (ví dụ `http://127.0.0.1:5173`). Chỉ mock API trong trình duyệt kiểm tra, không sửa dữ liệu thật.
