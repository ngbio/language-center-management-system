# Phân tích Use Case

Tài liệu này mô tả các actor và use case của **hệ thống đăng ký khóa học tại trung tâm ngoại ngữ**. Nội dung được xây dựng dựa trên sơ đồ Use Case của hệ thống, trong đó các chức năng được phân chia theo bốn nhóm người dùng chính: **Học viên**, **Giảng viên**, **Nhân viên tư vấn** và **Admin**.

---

## 1. Phạm vi Use Case

Hệ thống hỗ trợ các nghiệp vụ chính liên quan đến tra cứu khóa học, đăng ký lớp học, thanh toán học phí, quản lý quá trình học tập và quản trị dữ liệu tại trung tâm ngoại ngữ.

Tài liệu tập trung đặc tả chi tiết bốn use case tiêu biểu:

- Đăng ký lớp học.
- Thanh toán học phí.
- Điểm danh học viên.
- Quản lý khóa học.

Các chức năng còn lại được liệt kê nhằm thể hiện phạm vi tổng thể của hệ thống và mối quan hệ giữa các actor.

---

## 2. Actor

| Actor | Mô tả |
|---|---|
| Người dùng | Actor tổng quát, đại diện cho các chức năng chung như đăng nhập, đăng xuất, quên mật khẩu và đổi mật khẩu |
| Học viên | Tra cứu khóa học, đăng ký hoặc hủy đăng ký lớp học, thanh toán học phí, xem lịch học và kết quả điểm danh |
| Giảng viên | Xem lịch giảng dạy, xem danh sách học viên và thực hiện điểm danh |
| Nhân viên tư vấn | Tra cứu thông tin học viên, đăng ký lớp thay cho học viên và quản lý học viên |
| Admin | Quản lý tài khoản, học viên, giảng viên, khóa học, xem dashboard và báo cáo thống kê |

### Quan hệ kế thừa giữa các actor

Các actor **Học viên**, **Giảng viên**, **Nhân viên tư vấn** và **Admin** kế thừa actor tổng quát **Người dùng**.

```text
Người dùng
├── Học viên
├── Giảng viên
├── Nhân viên tư vấn
└── Admin
```

Điều này có nghĩa là bốn actor trên đều có thể thực hiện các chức năng chung của Người dùng, đồng thời mỗi actor có thêm các chức năng riêng tương ứng với vai trò của mình.

---

## 3. Danh sách Use Case

### 3.1. Chức năng chung

| Mã | Tên Use Case | Actor chính | Ghi chú |
|---|---|---|---|
| UC-C01 | Đăng nhập | Người dùng | Xác thực tài khoản và xác định quyền truy cập |
| UC-C02 | Đăng xuất | Người dùng | Kết thúc phiên đăng nhập |
| UC-C03 | Quên mật khẩu | Người dùng | Khôi phục quyền truy cập tài khoản |
| UC-C04 | Đổi mật khẩu | Người dùng | Thay đổi mật khẩu hiện tại |

### 3.2. Chức năng của Học viên

| Mã | Tên Use Case | Actor chính | Ghi chú |
|---|---|---|---|
| UC-S01 | Xem danh sách khóa học | Học viên | Tra cứu các khóa học đang cung cấp |
| UC-S02 | Xem lịch học | Học viên | Xem lịch của các lớp đã đăng ký |
| UC-S03 | Xem lịch sử đăng ký | Học viên | Xem các lần đăng ký lớp học |
| UC-S04 | Đăng ký lớp học | Học viên | Bao gồm kiểm tra điều kiện đăng ký |
| UC-S05 | Hủy đăng ký | Học viên | Hủy đăng ký khi đáp ứng điều kiện |
| UC-S06 | Thanh toán học phí | Học viên | Thanh toán cho đăng ký lớp học hợp lệ |
| UC-S07 | Xem kết quả điểm danh | Học viên | Theo dõi tình trạng tham gia các buổi học |

### 3.3. Chức năng của Giảng viên

| Mã | Tên Use Case | Actor chính | Ghi chú |
|---|---|---|---|
| UC-T01 | Xem lịch giảng dạy | Giảng viên | Xem các lớp và buổi học được phân công |
| UC-T02 | Xem danh sách học viên | Giảng viên | Xem học viên trong lớp phụ trách |
| UC-T03 | Điểm danh học viên | Giảng viên | Bao gồm xem danh sách học viên |

### 3.4. Chức năng của Nhân viên tư vấn

| Mã | Tên Use Case | Actor chính | Ghi chú |
|---|---|---|---|
| UC-CS01 | Tra cứu thông tin học viên | Nhân viên tư vấn | Tìm kiếm và xem thông tin học viên |
| UC-CS02 | Đăng ký lớp cho học viên | Nhân viên tư vấn | Bao gồm tra cứu thông tin học viên |
| UC-CS03 | Quản lý học viên | Nhân viên tư vấn | Theo dõi và cập nhật thông tin học viên |

### 3.5. Chức năng của Admin

| Mã | Tên Use Case | Actor chính | Ghi chú |
|---|---|---|---|
| UC-A01 | Xem dashboard | Admin | Xem số liệu tổng quan của hệ thống |
| UC-A02 | Quản lý tài khoản | Admin | Bao gồm khóa và mở khóa tài khoản |
| UC-A03 | Quản lý học viên | Admin | Quản lý dữ liệu học viên |
| UC-A04 | Quản lý giảng viên | Admin | Quản lý dữ liệu giảng viên |
| UC-A05 | Quản lý khóa học | Admin | Xem, thêm, cập nhật và thay đổi trạng thái khóa học |
| UC-A06 | Báo cáo thống kê | Admin | Theo dõi và tổng hợp số liệu hoạt động |

---

## 4. Quan hệ giữa các Use Case

| Use Case nguồn | Quan hệ | Use Case đích | Ý nghĩa |
|---|---|---|---|
| Đăng ký lớp học | `<<include>>` | Kiểm tra điều kiện đăng ký lớp học | Luôn phải kiểm tra điều kiện trước khi tạo đăng ký |
| Thanh toán học phí | `<<extend>>` | Đăng ký lớp học | Thanh toán phát sinh sau khi học viên đăng ký lớp và có nhu cầu thanh toán |
| Điểm danh học viên | `<<include>>` | Xem danh sách học viên | Giảng viên cần xem danh sách lớp để thực hiện điểm danh |
| Đăng ký lớp cho học viên | `<<include>>` | Tra cứu thông tin học viên | Nhân viên tư vấn phải xác định đúng học viên trước khi đăng ký |

> **Lưu ý:** `<<include>>` thể hiện chức năng bắt buộc phải được thực hiện trong use case chính. `<<extend>>` thể hiện chức năng bổ sung, chỉ xảy ra khi đáp ứng điều kiện nhất định.

---

## 5. Use Case Diagram

![Sơ đồ Use Case của hệ thống đăng ký khóa học](./images/UseCase.png)

Khi đưa tài liệu vào repository, lưu ảnh sơ đồ tại:

```text
docs/images/UseCase.png
```

Nếu file `use-case.md` nằm trực tiếp trong thư mục `docs`, đường dẫn ảnh `./images/UseCase.png` sẽ hiển thị đúng trên GitHub.

---

## 6. Đặc tả các Use Case chính

### 6.1. UC01 - Đăng ký lớp học

| Trường | Nội dung |
|---|---|
| Use Case ID | UC01 |
| Tên Use Case | Đăng ký lớp học |
| Actor chính | Học viên |
| Actor phụ | Không có |
| Mô tả vắn tắt | Cho phép học viên chọn và đăng ký một lớp đang mở thuộc khóa học mong muốn |
| Điều kiện trước | Học viên đã đăng nhập; tài khoản đang hoạt động; lớp học đang mở đăng ký |
| Điều kiện sau | Thông tin đăng ký được ghi nhận với trạng thái `Chờ thanh toán` |

**Luồng hoạt động chính:**

1. Học viên xem danh sách khóa học.
2. Học viên chọn một khóa học.
3. Hệ thống hiển thị các lớp đang mở thuộc khóa học.
4. Học viên chọn lớp muốn đăng ký.
5. Hệ thống thực hiện kiểm tra điều kiện đăng ký lớp học.
6. Hệ thống hiển thị thông tin lớp, lịch học, giảng viên và học phí.
7. Học viên kiểm tra thông tin đăng ký.
8. Học viên nhấn nút **Xác nhận đăng ký**.
9. Hệ thống tạo thông tin đăng ký với trạng thái `Chờ thanh toán`.
10. Hệ thống thông báo **Đăng ký lớp học thành công**.
11. Use Case kết thúc.

**Luồng thay thế:**

- Tại bước 4: Nếu học viên chưa chọn lớp, hệ thống yêu cầu chọn lớp học.
- Tại bước 5: Nếu học viên không đủ điều kiện, hệ thống hiển thị nguyên nhân và không tạo đăng ký.
- Tại bước 8: Nếu học viên không xác nhận, hệ thống quay lại trang thông tin lớp.
- Nếu học viên chưa thanh toán, đăng ký tiếp tục được lưu với trạng thái `Chờ thanh toán`.

**Luồng ngoại lệ:**

- Nếu lớp vừa đủ sĩ số trước khi thông tin đăng ký được lưu, hệ thống thông báo **Lớp học đã đủ sĩ số**.
- Nếu quá trình lưu đăng ký thất bại, hệ thống không thay đổi sĩ số lớp và thông báo học viên thử lại.

---

### 6.2. UC02 - Thanh toán học phí

| Trường | Nội dung |
|---|---|
| Use Case ID | UC02 |
| Tên Use Case | Thanh toán học phí |
| Actor chính | Học viên |
| Actor phụ | Không có |
| Mô tả vắn tắt | Cho phép học viên thanh toán học phí cho một đăng ký lớp học hợp lệ |
| Điều kiện trước | Học viên đã đăng nhập; có đăng ký lớp học hợp lệ và đăng ký chưa được thanh toán |
| Điều kiện sau | Thông tin giao dịch được lưu; trạng thái thanh toán và trạng thái đăng ký được cập nhật phù hợp với kết quả thanh toán |

**Luồng hoạt động chính:**

1. Học viên truy cập danh sách lớp học đã đăng ký.
2. Học viên chọn đăng ký cần thanh toán.
3. Hệ thống hiển thị thông tin lớp học, học phí và trạng thái thanh toán.
4. Học viên chọn phương thức thanh toán.
5. Học viên kiểm tra và xác nhận thông tin thanh toán.
6. Hệ thống xử lý yêu cầu thanh toán.
7. Hệ thống nhận kết quả thanh toán thành công.
8. Hệ thống cập nhật trạng thái thanh toán thành `Đã thanh toán`.
9. Hệ thống cập nhật đăng ký thành `Đã xác nhận`.
10. Hệ thống thông báo **Thanh toán học phí thành công**.
11. Use Case kết thúc.

**Luồng thay thế:**

- Tại bước 5: Nếu học viên không xác nhận, hệ thống hủy yêu cầu thanh toán và giữ nguyên trạng thái đăng ký.

**Luồng ngoại lệ:**

- Nếu thanh toán thất bại, hệ thống giữ trạng thái `Chờ thanh toán` và thông báo cho học viên.
- Nếu đăng ký đã được thanh toán trước đó, hệ thống không tạo thêm giao dịch.
- Nếu xảy ra lỗi khi lưu giao dịch, hệ thống thông báo lỗi và không cập nhật đăng ký thành `Đã xác nhận`.

---

### 6.3. UC03 - Điểm danh học viên

| Trường | Nội dung |
|---|---|
| Use Case ID | UC03 |
| Tên Use Case | Điểm danh học viên |
| Actor chính | Giảng viên |
| Actor phụ | Không có |
| Mô tả vắn tắt | Cho phép giảng viên ghi nhận tình trạng tham gia của học viên trong từng buổi học |
| Điều kiện trước | Giảng viên đã đăng nhập; được phân công giảng dạy lớp; buổi học tồn tại trong lịch giảng dạy |
| Điều kiện sau | Tình trạng tham gia của từng học viên trong buổi học được lưu vào hệ thống |

**Luồng hoạt động chính:**

1. Giảng viên truy cập chức năng **Quản lý điểm danh**.
2. Hệ thống hiển thị các lớp do giảng viên phụ trách.
3. Giảng viên chọn lớp và buổi học cần điểm danh.
4. Hệ thống hiển thị danh sách học viên của lớp.
5. Giảng viên chọn trạng thái `Có mặt`, `Vắng mặt` hoặc `Đi muộn` cho từng học viên.
6. Giảng viên nhập ghi chú nếu cần thiết.
7. Giảng viên nhấn nút **Lưu điểm danh**.
8. Hệ thống kiểm tra tính hợp lệ của dữ liệu.
9. Hệ thống lưu kết quả điểm danh.
10. Hệ thống thông báo **Điểm danh thành công**.
11. Use Case kết thúc.

**Luồng thay thế:**

- Tại bước 3: Nếu buổi học đã được điểm danh, hệ thống hiển thị kết quả cũ để giảng viên xem hoặc cập nhật.
- Tại bước 7: Nếu còn học viên chưa được chọn trạng thái, hệ thống yêu cầu giảng viên bổ sung.
- Nếu giảng viên hủy thao tác, dữ liệu điểm danh cũ được giữ nguyên.

**Luồng ngoại lệ:**

- Nếu giảng viên không phụ trách lớp đã chọn, hệ thống từ chối thao tác.
- Nếu buổi học không tồn tại hoặc đã bị hủy, hệ thống không cho phép điểm danh.
- Nếu lưu dữ liệu thất bại, hệ thống giữ dữ liệu trên giao diện và cho phép giảng viên thử lại.

---

### 6.4. UC04 - Quản lý khóa học

| Trường | Nội dung |
|---|---|
| Use Case ID | UC04 |
| Tên Use Case | Quản lý khóa học |
| Actor chính | Admin |
| Actor phụ | Không có |
| Mô tả vắn tắt | Cho phép Admin xem danh sách, thêm mới, cập nhật và thay đổi trạng thái khóa học |
| Điều kiện trước | Admin đã đăng nhập và có quyền quản lý khóa học |
| Điều kiện sau | Thông tin khóa học được thêm mới, cập nhật hoặc thay đổi trạng thái thành công |

**Luồng hoạt động chính:**

1. Admin truy cập chức năng **Quản lý khóa học**.
2. Hệ thống hiển thị danh sách khóa học.
3. Admin chọn thao tác thêm mới hoặc cập nhật khóa học.
4. Hệ thống hiển thị biểu mẫu thông tin khóa học.
5. Admin nhập hoặc chỉnh sửa mã khóa học, tên khóa học, ngôn ngữ, trình độ, thời lượng, học phí và mô tả.
6. Admin chọn trạng thái hoạt động của khóa học.
7. Admin nhấn nút **Lưu**.
8. Hệ thống kiểm tra tính hợp lệ của dữ liệu.
9. Hệ thống lưu thông tin khóa học.
10. Hệ thống thông báo thao tác thành công.
11. Use Case kết thúc.

**Luồng thay thế:**

- Tại bước 3: Nếu Admin chọn xem chi tiết, hệ thống hiển thị toàn bộ thông tin khóa học.
- Tại bước 3: Nếu Admin chọn thay đổi trạng thái, hệ thống yêu cầu xác nhận trước khi cập nhật.
- Tại bước 8: Nếu thiếu thông tin bắt buộc, hệ thống đánh dấu trường bị thiếu và yêu cầu bổ sung.
- Nếu mã khóa học đã tồn tại, hệ thống yêu cầu nhập mã khác.
- Nếu Admin hủy thao tác, hệ thống quay lại danh sách và không thay đổi dữ liệu.

**Luồng ngoại lệ:**

- Nếu ngôn ngữ hoặc trình độ được chọn không tồn tại, hệ thống từ chối lưu khóa học.
- Nếu lưu dữ liệu thất bại, hệ thống giữ nguyên dữ liệu cũ và thông báo cho Admin thử lại.

---

## 7. Ghi chú đồng bộ với các tài liệu khác

- Các đặc tả trong tài liệu này phải thống nhất với sơ đồ Use Case, thiết kế cơ sở dữ liệu và giao diện của hệ thống.
- Luồng `Đăng ký lớp học → Thanh toán học phí` là luồng nghiệp vụ chính của Học viên.
- Trước khi tạo đăng ký, hệ thống phải kiểm tra điều kiện đăng ký như trạng thái lớp, sĩ số, lịch học và các điều kiện liên quan.
- Chi tiết xử lý của từng nghiệp vụ có thể được mô tả thêm bằng Activity Diagram và Sequence Diagram.
- Thiết kế bảng, khóa chính và khóa ngoại liên quan đến các use case được trình bày trong tài liệu thiết kế cơ sở dữ liệu.
