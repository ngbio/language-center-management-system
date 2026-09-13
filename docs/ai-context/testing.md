# Testing context

Đã đối chiếu với cấu hình hiện tại tại commit nền `763bd81`; cập nhật gần nhất: 2026-09-13.

## Backend

Test nằm tại:

- Unit: `backend/language-center-management/src/test/java/.../unit/`.
- Integration: `backend/language-center-management/src/test/java/.../integration/`.
- Test config: `src/test/resources/application-test.properties`.

Chạy trong `backend/language-center-management/`:

```powershell
./mvnw.cmd test
```

Khi cần test đích danh:

```powershell
./mvnw.cmd -Dtest=UserServiceImplTest test
./mvnw.cmd -Dtest=EnrollmentControllerIntegrationTest test
```

Ưu tiên test cùng domain trước; chỉ chạy toàn bộ backend khi thay đổi shared security, mapper, transaction, schema hoặc nhiều service.

## Frontend

Chạy trong `frontend/`:

```powershell
npm.cmd run lint
npm.cmd run build
npm.cmd run test:e2e
```

- E2E specs: `frontend/e2e/`.
- Playwright tự khởi động Vite tại `127.0.0.1:4173` theo `playwright.config.js`.
- Một số flow cần backend/database hoạt động; đọc spec trước khi chạy để biết dependency.
- Với thay đổi responsive, kiểm tra tối thiểu 320px, 390px, 768px và một viewport desktop.

## Chọn mức kiểm tra

- Chỉ Markdown: kiểm tra link/path và `git diff --check`.
- CSS/layout: lint, build và kiểm tra viewport liên quan.
- Một React screen: lint, build, test E2E đúng flow nếu có.
- Một service: unit test class đó; thêm integration test nếu đổi contract/transaction/security.
- Security/JWT: unit security + integration endpoint cho `401`, `403`, role đúng.
- Payment/refund: unit service + integration callback/idempotency.
- Schema/entity: test repository/service liên quan và xác minh SQL migration thủ công trên database test nếu có.

## CI

- Workflow chính: `.github/workflows/ci.yml`.
- Deployment: `.github/workflows/cd.yml`.
- Trước khi bàn giao, báo rõ lệnh đã chạy, kết quả và cảnh báo không chặn build.
- Không lặp lại toàn bộ test đã pass nếu source không đổi sau lần chạy đó.

