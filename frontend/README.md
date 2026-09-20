# React + Vite

## Android (Capacitor)

Yêu cầu: Node.js, Android SDK 36 và JDK 21.

1. Tạo `frontend/.env.android.local` với API của máy chạy backend. Điện thoại thật phải dùng IP LAN, không dùng `localhost`:

   ```env
   VITE_API_BASE_URL=http://192.168.1.10:8081/api
   ```

2. Backend phải cho phép origin của Capacitor. Giá trị mặc định đã gồm `https://localhost`; nếu dùng biến môi trường, thêm origin này vào `APP_CORS_ALLOWED_ORIGINS`.
3. Kết nối điện thoại và máy chạy backend cùng mạng Wi-Fi, mở cổng `8081` trên firewall nếu cần.
4. Build APK:

   ```powershell
   npm.cmd run android:apk
   ```

APK debug được tạo tại `android/app/build/outputs/apk/debug/app-debug.apk`.

Sau mỗi lần sửa frontend, lệnh trên tự build Vite bằng mode `android`, đồng bộ Capacitor và đóng lại APK.

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and [`typescript-eslint`](https://typescript-eslint.io) in your project.
