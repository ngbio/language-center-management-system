# Weekly Progress - Week 02

**Project:** Course Registration System for Language Center
**Week:** 02
**Duration:** 14/07/2026 - 20/07/2026

---

## 🎯 Goals

* Hoàn thiện quá trình phân tích yêu cầu của hệ thống.
* Xác định đầy đủ tác nhân, chức năng và nghiệp vụ chính.
* Thiết kế sơ đồ Use Case tổng quát.
* Xây dựng kiến trúc tổng thể của hệ thống.
* Thiết kế cơ sở dữ liệu ban đầu.
* Tiếp tục hoàn thiện nội dung báo cáo đồ án.

---

## ✅ Completed

### 1. Phân tích chức năng hệ thống

Đã xác định các nhóm người dùng chính của hệ thống:

* Quản trị viên
* Nhân viên tư vấn
* Giảng viên
* Học viên

Đã phân tích các nhóm chức năng chính:

* Quản lý tài khoản và phân quyền.
* Quản lý học viên.
* Quản lý giảng viên.
* Quản lý khóa học.
* Quản lý lớp học.
* Quản lý phòng học và lịch học.
* Đăng ký và hủy đăng ký lớp học.
* Thanh toán học phí.
* Điểm danh học viên.
* Quản lý điểm và kết quả học tập.
* Quản lý thông báo.
* Báo cáo và thống kê.
* Quản lý nhật ký hoạt động hệ thống.

---

### 2. Thiết kế Use Case

Đã xây dựng sơ đồ Use Case tổng quát cho hệ thống.

Các tác nhân được kế thừa từ tác nhân chung `Người dùng`:

* Quản trị viên
* Nhân viên tư vấn
* Giảng viên
* Học viên

Đã xác định:

* Các chức năng của từng tác nhân.
* Các quan hệ giữa tác nhân và Use Case.
* Các quan hệ `include`.
* Các quan hệ `extend`.
* Các chức năng dùng chung giữa nhiều tác nhân.

---

### 3. Thiết kế kiến trúc hệ thống

Đã xác định công nghệ và kiến trúc chính:

* ReactJS cho Frontend.
* Spring Boot cho Backend.
* MySQL cho cơ sở dữ liệu.
* REST API để giao tiếp giữa Frontend và Backend.

Backend được tổ chức theo các thành phần:

* Controller
* Service
* Repository
* Entity
* DTO
* Security

Đã xác định luồng xử lý tổng quát:

```text
Người dùng
    │
    ▼
ReactJS Frontend
    │
    ▼
Spring Boot REST API
    │
    ▼
Business Logic
    │
    ▼
Repository / Hibernate
    │
    ▼
MySQL Database
```

---

### 4. Thiết kế cơ sở dữ liệu

Đã phân tích các bảng dữ liệu chính:

* roles
* users
* students
* teachers
* languages
* levels
* courses
* rooms
* shifts
* classes
* enrollments
* payments
* attendance
* scores
* notifications
* system_logs

Đã xác định bước đầu:

* Khóa chính của từng bảng.
* Khóa ngoại giữa các bảng.
* Quan hệ một-một.
* Quan hệ một-nhiều.
* Quan hệ nhiều-nhiều.
* Các trường trạng thái cần sử dụng.
* Các ràng buộc dữ liệu cơ bản.

Một số quan hệ chính:

```text
Role 1 ─── N User

User 1 ─── 1 Student

User 1 ─── 1 Teacher

Language 1 ─── N Course

Level 1 ─── N Course

Course 1 ─── N Class

Teacher 1 ─── N Class

Room 1 ─── N Class

Student N ─── N Class
        thông qua Enrollment

Enrollment 1 ─── N Payment

Enrollment 1 ─── 1 Score

Class 1 ─── N Attendance

Student 1 ─── N Attendance
```

---

### 5. Hoàn thiện báo cáo đồ án

Đã tiếp tục xây dựng nội dung báo cáo:

#### Chương 1: Tổng quan đề tài

* Giới thiệu đề tài.
* Lý do chọn đề tài.
* Phát biểu bài toán.
* Mục tiêu đề tài.
* Phạm vi đề tài.

#### Chương 2: Cơ sở lý thuyết và công nghệ

* Tổng quan các công nghệ sử dụng.
* Đặc điểm của từng công nghệ.
* Vai trò của công nghệ trong hệ thống.
* Lý do lựa chọn công nghệ.
* So sánh công nghệ truyền thống và hiện đại.

#### Chương 3: Phân tích và thiết kế hệ thống

* Giới thiệu hệ thống.
* Kiến trúc tổng thể.
* Các thành phần của kiến trúc.
* Luồng xử lý tổng quát.
* Các tác nhân và chức năng chính.
* Sơ đồ Use Case tổng quát.

---

## 📚 Technologies

* Java
* Spring Boot
* Spring MVC
* Spring Security
* Spring Data JPA
* Hibernate
* ReactJS
* MySQL
* JWT
* Git
* GitHub
* Figma

---

## 📈 Progress

```text
Overall Progress

████████░░░░░░░░░░░░░░░░░░ 25%
```

Đã hoàn thành bước đầu:

* Phân tích yêu cầu.
* Xác định tác nhân.
* Xác định chức năng hệ thống.
* Vẽ sơ đồ Use Case tổng quát.
* Thiết kế kiến trúc hệ thống.
* Thiết kế cơ sở dữ liệu sơ bộ.
* Viết nội dung Chương 1, Chương 2 và một phần Chương 3.

---

## 🚀 Plan for Next Week

### 1. Viết đặc tả Use Case

Viết đặc tả chi tiết cho các Use Case quan trọng:

* Đăng nhập.
* Quản lý tài khoản.
* Tra cứu khóa học.
* Đăng ký lớp học.
* Hủy đăng ký.
* Thanh toán học phí.
* Quản lý khóa học.
* Quản lý lớp học.
* Xếp lịch học.
* Điểm danh.
* Nhập điểm.
* Xem kết quả học tập.
* Gửi và xem thông báo.
* Xem báo cáo thống kê.

Mỗi đặc tả Use Case cần có:

* Tên Use Case.
* Mã Use Case.
* Tác nhân thực hiện.
* Mục tiêu.
* Mô tả.
* Tiền điều kiện.
* Hậu điều kiện.
* Luồng chính.
* Luồng thay thế.
* Luồng ngoại lệ.
* Quy tắc nghiệp vụ liên quan.

---

### 2. Hoàn thiện các sơ đồ hệ thống

Tiếp tục thiết kế và hoàn thiện:

* Use Case Diagram tổng quát.
* Use Case Diagram theo từng tác nhân.
* Activity Diagram.
* Sequence Diagram.
* Class Diagram.
* Entity Relationship Diagram.
* System Architecture Diagram.
* Database Diagram.
* Sơ đồ luồng xử lý tổng quát.

Ưu tiên xây dựng sơ đồ cho các nghiệp vụ chính:

* Đăng nhập.
* Đăng ký lớp học.
* Thanh toán học phí.
* Xếp lớp.
* Điểm danh.
* Nhập điểm.
* Hủy đăng ký và hoàn tiền.

---

### 3. Thiết kế giao diện hệ thống

Sử dụng Figma để thiết kế giao diện ban đầu.

#### Giao diện chung

* Trang đăng nhập.
* Trang quên mật khẩu.
* Trang thông tin cá nhân.
* Trang thông báo.

#### Giao diện học viên

* Trang chủ.
* Danh sách khóa học.
* Chi tiết khóa học.
* Danh sách lớp học.
* Đăng ký lớp học.
* Lịch học cá nhân.
* Lịch sử đăng ký.
* Lịch sử thanh toán.
* Kết quả điểm danh.
* Kết quả học tập.

#### Giao diện giảng viên

* Danh sách lớp được phân công.
* Lịch giảng dạy.
* Danh sách học viên.
* Điểm danh.
* Nhập điểm.
* Xem thông tin lớp học.

#### Giao diện quản trị viên và nhân viên tư vấn

* Dashboard.
* Quản lý người dùng.
* Quản lý khóa học.
* Quản lý lớp học.
* Quản lý phòng học.
* Quản lý lịch học.
* Quản lý đăng ký.
* Quản lý thanh toán.
* Quản lý thông báo.
* Báo cáo và thống kê.

---

### 4. Tạo cơ sở dữ liệu MySQL

Thực hiện tạo database chính thức cho hệ thống.

Các công việc cần thực hiện:

* Đặt tên database.
* Tạo các bảng dữ liệu.
* Khai báo khóa chính.
* Khai báo khóa ngoại.
* Thiết lập kiểu dữ liệu.
* Thiết lập `NOT NULL`.
* Thiết lập `UNIQUE`.
* Thiết lập `DEFAULT`.
* Thiết lập các ràng buộc `CHECK`.
* Tạo chỉ mục cho các trường thường xuyên tìm kiếm.
* Thêm dữ liệu mẫu.
* Kiểm tra quan hệ giữa các bảng.

Tên database dự kiến:

```sql
language_center_course_registration
```

---

### 5. Xác định và mô tả nghiệp vụ hệ thống

Phân tích chi tiết các quy tắc nghiệp vụ.

#### Nghiệp vụ đăng ký lớp học

* Học viên phải đăng nhập.
* Tài khoản học viên phải đang hoạt động.
* Lớp học phải còn thời gian đăng ký.
* Lớp học phải còn chỗ trống.
* Học viên không được đăng ký trùng một lớp.
* Học viên không được đăng ký các lớp trùng lịch.
* Trình độ học viên phải phù hợp với khóa học.
* Học viên phải hoàn thành điều kiện tiên quyết nếu có.

#### Nghiệp vụ xếp lớp

* Sĩ số không được vượt quá số lượng tối đa.
* Số lượng học viên không được vượt quá sức chứa phòng.
* Giảng viên không được dạy hai lớp trùng thời gian.
* Phòng học không được sử dụng cho hai lớp cùng lúc.
* Một lớp phải có giảng viên, phòng và ca học hợp lệ.

#### Nghiệp vụ thanh toán

* Số tiền thanh toán phải bằng học phí cần thanh toán.
* Một đăng ký có thể có một hoặc nhiều giao dịch thanh toán.
* Chỉ các giao dịch thành công mới được tính là đã thanh toán.
* Không được xác nhận thanh toán hai lần cho cùng một giao dịch.
* Học viên chỉ được hoàn tiền khi đáp ứng điều kiện hủy đăng ký.

#### Nghiệp vụ hủy đăng ký

* Học viên chỉ được hủy trước thời hạn quy định.
* Không được hủy lớp đã bắt đầu học, trừ trường hợp đặc biệt.
* Nếu đã thanh toán, hệ thống phải xác định số tiền được hoàn.
* Sau khi hủy, sĩ số lớp học phải được cập nhật.

#### Nghiệp vụ điểm danh

* Chỉ giảng viên phụ trách hoặc quản trị viên mới được điểm danh.
* Điểm danh được thực hiện theo từng buổi học.
* Một học viên chỉ có một trạng thái điểm danh trong một buổi.
* Trạng thái có thể gồm:

  * Có mặt
  * Vắng mặt
  * Đi muộn
  * Có phép

#### Nghiệp vụ nhập điểm

* Chỉ giảng viên phụ trách lớp được nhập điểm.
* Điểm phải nằm trong giới hạn hợp lệ.
* Điểm tổng kết được tính theo công thức quy định.
* Chỉ công bố kết quả khi dữ liệu điểm đã đầy đủ.
* Sau khi công bố, việc chỉnh sửa điểm phải được kiểm soát.

---

## 📌 Expected Results for Next Week

Sau khi hoàn thành tuần tiếp theo, dự kiến đạt được:

* Bộ đặc tả Use Case cho các chức năng chính.
* Bộ sơ đồ phân tích và thiết kế hệ thống.
* Bản thiết kế giao diện đầu tiên trên Figma.
* Database MySQL hoàn chỉnh ở mức ban đầu.
* Danh sách quy tắc nghiệp vụ rõ ràng.
* Dữ liệu mẫu để kiểm tra database.
* Nội dung Chương 3 được hoàn thiện thêm.
* Sẵn sàng chuyển sang giai đoạn tạo project Spring Boot và ReactJS.
