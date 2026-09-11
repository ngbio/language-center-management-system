# Firebase Realtime Chat — Student và Consultant

## Luồng xác thực

1. Student hoặc Consultant đăng nhập bằng JWT của hệ thống.
2. Frontend gọi `POST /api/chat/firebase-token` kèm Bearer token.
3. Backend kiểm tra tài khoản/role và phát Firebase custom token.
4. Student được backend gán cho một Consultant `ACTIVE`.
5. Frontend dùng `signInWithCustomToken` và lắng nghe Realtime Database.
6. Firebase Rules chỉ cho đúng cặp Student–Consultant đọc và gửi tin nhắn.

Không bật quyền đọc/ghi public cho Realtime Database.

## Cấu hình Firebase Console

1. Tạo Firebase project và đăng ký một Web App.
2. Bật Authentication. Custom token không yêu cầu bật Email/Password provider.
3. Tạo Realtime Database tại region gần người dùng.
4. Dán nội dung `firebase-database.rules.json` vào tab Rules và Publish.
5. Trong Project settings > Service accounts, tạo private key JSON.
6. Chuyển toàn bộ JSON thành Base64 một dòng rồi đặt vào biến môi trường backend.

PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("firebase-service-account.json"))
```

Tuyệt đối không commit file service-account JSON hoặc chuỗi Base64 thật.

Nếu muốn publish rules bằng terminal thay vì Firebase Console:

```powershell
npm install --global firebase-tools
firebase login
firebase use --add
firebase deploy --only database
```

File `firebase.json` ở thư mục gốc đã trỏ tới `firebase-database.rules.json`.

## Biến môi trường backend

```properties
FIREBASE_ENABLED=true
FIREBASE_DATABASE_URL=https://PROJECT_ID-default-rtdb.asia-southeast1.firebasedatabase.app
FIREBASE_SERVICE_ACCOUNT_JSON_BASE64=BASE64_SERVICE_ACCOUNT_JSON
```

Khi chưa cấu hình, giữ `FIREBASE_ENABLED=false`. Backend vẫn khởi động bình thường và API chat trả thông báo 503.

## Biến môi trường frontend

Sao chép các giá trị Web App config từ Firebase Console:

```properties
VITE_FIREBASE_API_KEY=...
VITE_FIREBASE_AUTH_DOMAIN=PROJECT_ID.firebaseapp.com
VITE_FIREBASE_DATABASE_URL=https://PROJECT_ID-default-rtdb.asia-southeast1.firebasedatabase.app
VITE_FIREBASE_PROJECT_ID=PROJECT_ID
VITE_FIREBASE_APP_ID=...
```

Firebase Web config không phải private key. Quyền truy cập vẫn do Authentication và Database Rules kiểm soát.

## Cấu trúc dữ liệu

```text
chats/
  {consultantUid}/
    {studentUid}/
      metadata/
      messages/
        {messageId}/
```

- Consultant chỉ đọc nhánh UID của chính mình.
- Student chỉ đọc nhánh của Consultant được ghi trong custom claim.
- Tin nhắn đã tạo không được client sửa hoặc xóa.
- Nội dung tin nhắn giới hạn 2.000 ký tự.

## Giao diện

- Student: bong bóng `Chat` cố định ở góc phải khi đã đăng nhập.
- Consultant: menu `Tin nhắn học viên` tại `/staff/chat`.
- Mở cuộc trò chuyện sẽ đánh dấu số tin chưa đọc về 0.

## Giới hạn hiện tại

Việc phân công dùng danh sách Consultant `ACTIVE` và chọn ổn định theo ID Student tại thời điểm cấp token. Nếu danh sách Consultant thay đổi, hệ thống có thể gán lại Student. Khi cần quản lý phân công lâu dài hoặc bàn giao hội thoại, nên lưu `student_id -> consultant_id` trong MySQL và chỉ thay đổi qua nghiệp vụ bàn giao riêng.
