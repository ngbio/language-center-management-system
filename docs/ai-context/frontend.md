# Frontend context

Đã đối chiếu với source tại commit nền `763bd81`; cập nhật gần nhất: 2026-09-13.

## Stack và điểm vào

- React 19 SPA, React Router, Axios, Vite.
- Root: `frontend/`.
- Entry: `src/main.jsx` → `src/App.jsx`.
- Route map: `src/App.jsx`.
- API map và Axios clients: `src/configs/Apis.js`.
- Chuẩn hóa response/error: `src/utils/api.js`.
- Session JWT trong localStorage: `src/utils/authSession.js`.
- Base API: `VITE_API_BASE_URL`, mặc định `http://localhost:8081/api`.

## Khu vực giao diện

- Public/Student/Teacher dùng `components/public/PublicLayout.jsx`.
- Admin dùng `components/AdminLayout.jsx` và `ProtectedAdminRoute.jsx`.
- Consultant dùng `components/StaffLayout.jsx` và `ProtectedStaffRoute.jsx`.
- Màn hình nằm trong `src/screens/public`, `teacher`, `admin`, `staff`.
- Component dùng chung nằm trong `src/components`.

## CSS

- `src/index.css`: reset và global primitives.
- `src/App.css`: Admin/Staff, table, form, modal, dashboard; breakpoint workspace chính `780px`.
- `src/styles/PublicSite.css`: Public/Student/Teacher; menu mobile chính `900px`, phone `620px` và `420px`.
- CSS chuyên biệt: `Attendance.css`, `Chat.css`, `StudentCourses.css`, `StudentNotifications.css`, `ImageUpload.css`, `ChangePasswordScreen.css`.
- Trước khi thêm selector mới, dùng `rg -n "className|selector" frontend/src` để tránh trùng.

## Authentication phía client

- Một session dùng các key `token`, `role`, `email`.
- `authApis()` gắn Bearer token và xóa session khi nhận `401`.
- Role hợp lệ trong UI: `ADMIN`, `CONSULTANT`, `STUDENT`, `TEACHER`.
- Route guard phía frontend chỉ phục vụ UX; quyền thật phải được backend kiểm tra.

## Tích hợp ngoài

- Firebase Realtime Database chat: `src/services/firebaseChat.js`.
- Cấu hình Firebase lấy từ các biến `VITE_FIREBASE_*` trong `.env`.
- Upload ảnh gọi endpoint `/uploads/images`; backend lưu qua Cloudinary.

## Cách đọc theo tác vụ

- Route/navigation: đọc `App.jsx` và layout tương ứng.
- API call: tìm key endpoint trong `Apis.js`, sau đó mở đúng screen/component sử dụng key đó.
- Responsive: mở layout + file CSS của khu vực; kiểm tra ở 320, 390, 768 và desktop.
- Chat: đọc `firebaseChat.js`, `Chat.css` và screen/component chat liên quan.
- Không quét tất cả screen nếu chỉ sửa một route.

## Lệnh thường dùng

Chạy trong `frontend/`:

```powershell
npm.cmd run dev
npm.cmd run lint
npm.cmd run build
npm.cmd run test:e2e
```

Trên PowerShell của máy này nên dùng `npm.cmd` vì execution policy có thể chặn `npm.ps1`.

