# Thiết kế chức năng ôn tập, flashcard và quiz

## 1. Mục tiêu

Bổ sung khu vực tự học cho khách của khóa miễn phí và học viên có quyền truy cập khóa học, gồm:

- Ôn lại các bài học đã có trong chương trình khóa học.
- Học flashcard dạng văn bản.
- Làm bài kiểm tra nhanh (quiz) theo từng bài.

Các chức năng nghe, phát âm, hình ảnh mẫu và theo dõi tiến độ toàn khóa không nằm trong phạm vi này.

## 2. Phạm vi chức năng

### 2.1. Ôn tập bài học

- Tận dụng dữ liệu hiện có trong `course_section` và `course_content`.
- Hiển thị nội dung có `publication_status = 'PUBLISHED'` theo thứ tự chương và bài.
- Học viên có thể xem lại `summary`, `content_html` và tài liệu đính kèm hiện có.
- Không lưu phần trăm hoàn thành, trạng thái đã học hoặc số lần mở bài.
- Từ một bài học, học viên có thể chuyển sang bộ flashcard hoặc quiz gắn với bài đó.

### 2.2. Flashcard văn bản

- Mặt trước: từ hoặc cụm từ.
- Mặt sau: nghĩa hoặc nội dung giải thích.
- Có thể có câu ví dụ dạng văn bản.
- Không sử dụng âm thanh, phát âm hoặc hình ảnh.
- Sau khi xem đáp án, học viên chọn một trong ba mức:
  - `AGAIN`: Chưa nhớ.
  - `HARD`: Khó.
  - `REMEMBERED`: Đã nhớ.
- Hệ thống dùng kết quả trên để xác định ngày ôn tiếp theo.

Lịch ôn MVP đề xuất:

| Kết quả | Lần ôn tiếp theo |
|---|---:|
| Chưa nhớ | Sau 1 ngày |
| Khó | Sau 3 ngày |
| Đã nhớ | Sau 7 ngày |

### 2.3. Quiz

- Mỗi bài học có thể có một hoặc nhiều quiz.
- MVP hỗ trợ câu hỏi một đáp án và câu hỏi đúng/sai.
- Học viên được xem điểm, trạng thái đạt/chưa đạt và lời giải thích sau khi nộp.
- Với học viên đăng nhập, lưu từng lần làm để có thể xem lại kết quả.
- Khách chưa đăng nhập vẫn được làm quiz của khóa miễn phí nhưng kết quả chỉ trả về trong phiên hiện tại, không lưu lịch sử.
- Việc làm quiz không được dùng để tính tiến độ khóa học.

### 2.4. Quyền sử dụng khóa học miễn phí

- Khóa học có `tuition_fee = 0` và đang `PUBLISHED` cho phép mọi người xem nội dung, flashcard và làm quiz đã xuất bản.
- Khách chưa đăng nhập không cần enrollment nhưng không được lưu lịch ôn flashcard hoặc lịch sử làm quiz.
- Người dùng đã đăng nhập được lưu trạng thái flashcard và kết quả quiz theo `student_id`, không cần tạo enrollment miễn phí.
- Quyền truy cập nội dung miễn phí không được phụ thuộc vào việc tồn tại bản ghi thanh toán.

## 3. Dữ liệu hiện có được sử dụng

```text
course
  └─ course_section
       └─ course_content

student
  └─ enrollment
       └─ courseclass
            └─ course
```

- `course_content` là bài/nội dung giáo trình dùng chung cho khóa học.
- `lesson` là buổi học thực tế theo lịch của một lớp, không phải nội dung giáo trình.
- `enrollment` chỉ dùng để kiểm tra quyền truy cập khóa học có học phí.
- `student` được dùng để lưu lịch ôn flashcard và lịch sử quiz, vì khóa miễn phí có thể không mở `courseclass` và không có enrollment.
- Với khách sử dụng khóa miễn phí, hệ thống chỉ đọc nội dung và chấm quiz tức thời, không ghi dữ liệu cá nhân.

## 4. Các bảng cần bổ sung

### 4.1. `flashcard`

Lưu nội dung flashcard của một `course_content`.

| Cột | Ý nghĩa |
|---|---|
| `id` | Khóa chính |
| `course_content_id` | Bài học chứa flashcard |
| `front_text` | Nội dung mặt trước |
| `back_text` | Nội dung mặt sau |
| `example_sentence` | Câu ví dụ, có thể để trống |
| `display_order` | Thứ tự hiển thị |
| `status` | `ACTIVE` hoặc `INACTIVE` |

Ràng buộc quan trọng:

- Foreign key tới `course_content(id)`.
- Unique `(course_content_id, display_order)`.

### 4.2. `flashcard_review`

Lưu trạng thái ôn flashcard của từng học viên. Trạng thái dùng chung khi học viên học lại cùng khóa ở nhiều lớp.

| Cột | Ý nghĩa |
|---|---|
| `id` | Khóa chính |
| `student_id` | Học viên đang ôn thẻ |
| `flashcard_id` | Flashcard được ôn |
| `mastery_level` | `NEW`, `AGAIN`, `HARD`, `REMEMBERED` |
| `repetition_count` | Tổng số lần đánh giá thẻ |
| `interval_days` | Khoảng cách tới lần ôn sau |
| `last_reviewed_at` | Thời điểm ôn gần nhất |
| `next_review_at` | Thời điểm cần ôn tiếp |

Ràng buộc quan trọng:

- Foreign key `student_id` tới `student(id)`.
- Foreign key `flashcard_id` tới `flashcard(id)`.
- Unique `(student_id, flashcard_id)`.
- Không cần bảng lịch sử riêng trong MVP.

### 4.3. `quiz`

| Cột | Ý nghĩa |
|---|---|
| `id` | Khóa chính |
| `course_content_id` | Bài học chứa quiz |
| `title` | Tên quiz |
| `passing_score` | Điểm tối thiểu để đạt, từ 0 đến 100 |
| `max_attempts` | Số lần làm tối đa; `NULL` là không giới hạn |
| `status` | `DRAFT`, `PUBLISHED`, `ARCHIVED` |

### 4.4. `quiz_question`

| Cột | Ý nghĩa |
|---|---|
| `id` | Khóa chính |
| `quiz_id` | Quiz chứa câu hỏi |
| `question_text` | Nội dung câu hỏi |
| `question_type` | `SINGLE_CHOICE` hoặc `TRUE_FALSE` |
| `explanation` | Giải thích sau khi nộp |
| `points` | Điểm của câu hỏi |
| `display_order` | Thứ tự hiển thị |

### 4.5. `quiz_option`

| Cột | Ý nghĩa |
|---|---|
| `id` | Khóa chính |
| `question_id` | Câu hỏi chứa lựa chọn |
| `option_text` | Nội dung lựa chọn |
| `is_correct` | Có phải đáp án đúng không |
| `display_order` | Thứ tự hiển thị |

Backend phải kiểm tra một câu `SINGLE_CHOICE` có đúng một lựa chọn đúng.

### 4.6. `quiz_attempt`

| Cột | Ý nghĩa |
|---|---|
| `id` | Khóa chính |
| `student_id` | Học viên thực hiện quiz |
| `quiz_id` | Quiz được làm |
| `attempt_number` | Số thứ tự lần làm |
| `score` | Điểm theo thang 100 |
| `passed` | Đạt hoặc chưa đạt |
| `started_at` | Thời điểm bắt đầu |
| `submitted_at` | Thời điểm nộp bài |

Ràng buộc quan trọng:

- Foreign key `student_id` tới `student(id)`.
- Foreign key `quiz_id` tới `quiz(id)`.
- Unique `(student_id, quiz_id, attempt_number)`.

### 4.7. `quiz_attempt_answer`

| Cột | Ý nghĩa |
|---|---|
| `id` | Khóa chính |
| `attempt_id` | Lần làm quiz |
| `question_id` | Câu hỏi được trả lời |
| `selected_option_id` | Lựa chọn của học viên |
| `is_correct` | Kết quả chấm |
| `points_awarded` | Điểm nhận được |

Ràng buộc unique `(attempt_id, question_id)`.

## 5. Quan hệ dữ liệu sau khi bổ sung

```text
course_content
  ├─ flashcard
  │    └─ flashcard_review ── student
  └─ quiz
       ├─ quiz_question
       │    └─ quiz_option
       └─ quiz_attempt ── student
            └─ quiz_attempt_answer
```

Tổng số bảng mới của MVP: 7 bảng.

Không thêm `content_progress` hoặc bảng theo dõi tiến độ khóa học.

## 6. Luồng quản trị nội dung

### 6.1. Quản lý flashcard

```text
Admin chọn khóa học
→ Chọn chương
→ Chọn course_content
→ Mở mục Flashcard
→ Thêm/sửa/xóa mềm/sắp xếp thẻ
→ Kích hoạt flashcard
```

Điều kiện:

- Chỉ Admin hoặc vai trò được phân quyền mới được thay đổi nội dung.
- Không xóa cứng flashcard đã có dữ liệu `flashcard_review`; chuyển sang `INACTIVE`.
- `front_text` và `back_text` là bắt buộc.

### 6.2. Quản lý quiz

```text
Admin chọn course_content
→ Tạo quiz ở trạng thái DRAFT
→ Thêm câu hỏi
→ Thêm các lựa chọn
→ Chọn đáp án đúng
→ Thiết lập điểm đạt
→ Kiểm tra tính hợp lệ
→ Chuyển quiz sang PUBLISHED
```

Chỉ cho phép xuất bản khi:

- Quiz có ít nhất một câu hỏi.
- Mỗi câu hỏi có ít nhất hai lựa chọn.
- Câu một đáp án có đúng một đáp án đúng.
- Tổng điểm các câu lớn hơn 0.

## 7. Luồng học viên

### 7.1. Vào khu vực ôn tập của khóa có học phí

```text
Học viên đăng nhập
→ Mở “Khóa học của tôi”
→ Chọn một khóa học có enrollment hợp lệ
→ Chọn “Ôn tập”
→ Backend kiểm tra quyền truy cập
→ Hiển thị course_section và course_content đã PUBLISHED
```

Enrollment hợp lệ được đề xuất là:

- `enrollment_status = 'CONFIRMED'`.
- `payment_status = 'PAID'` với khóa học có học phí.

### 7.2. Vào khóa học miễn phí

```text
Người dùng mở khóa học có tuition_fee = 0
→ Backend kiểm tra khóa học ACTIVE và PUBLISHED
→ Hiển thị course_section, course_content, flashcard và quiz đã xuất bản
→ Không yêu cầu thanh toán
```

Nếu người dùng chưa đăng nhập:

```text
Xem bài và flashcard bình thường
→ Làm quiz qua API chấm tức thời
→ Nhận điểm và lời giải thích
→ Không lưu flashcard_review, quiz_attempt hoặc quiz_attempt_answer
```

Nếu người dùng đã đăng nhập:

```text
Mở khóa học miễn phí lần đầu
→ Backend xác định student từ tài khoản đăng nhập
→ Không yêu cầu hoặc tự tạo enrollment/payment
→ Lưu flashcard_review và quiz_attempt theo student_id
```

### 7.3. Xem lại bài học

```text
Học viên chọn chương
→ Chọn bài
→ Xem lại nội dung có sẵn
→ Chọn Flashcard hoặc Quiz nếu bài có dữ liệu tương ứng
```

Không tạo hoặc cập nhật bản ghi tiến độ khi học viên mở bài.

### 7.4. Ôn flashcard

```text
Học viên mở bộ flashcard của bài
→ Backend trả về các flashcard ACTIVE
→ Ưu tiên thẻ NEW hoặc đến hạn next_review_at
→ Hiển thị mặt trước
→ Học viên yêu cầu xem đáp án
→ Hiển thị mặt sau và câu ví dụ
→ Học viên chọn Chưa nhớ / Khó / Đã nhớ
→ Backend upsert flashcard_review
→ Tính next_review_at
→ Chuyển sang thẻ tiếp theo
```

Backend phải kiểm tra flashcard thuộc khóa miễn phí hoặc học viên có enrollment hợp lệ cho khóa chứa flashcard đó.

Với khách của khóa miễn phí, chỉ hiển thị và lật flashcard. Các nút đánh giá có thể dùng để sắp xếp lại thẻ trong bộ nhớ của trình duyệt nhưng không gọi API lưu `flashcard_review`.

### 7.5. Làm quiz có lưu lịch sử

```text
Học viên chọn quiz PUBLISHED
→ Backend kiểm tra quyền truy cập và số lần làm theo student
→ Tạo quiz_attempt
→ Trả câu hỏi và lựa chọn, không trả is_correct
→ Học viên chọn đáp án
→ Gửi toàn bộ câu trả lời
→ Backend chấm điểm trong một transaction
→ Lưu quiz_attempt_answer
→ Cập nhật score, passed và submitted_at
→ Trả kết quả cùng lời giải thích
```

Không tin tưởng điểm hoặc `is_correct` do frontend gửi lên. Kết quả phải được tính từ dữ liệu đáp án trên server.

### 7.6. Làm quiz miễn phí không đăng nhập

```text
Khách chọn quiz PUBLISHED của khóa miễn phí
→ Backend trả câu hỏi và lựa chọn, không trả is_correct
→ Khách gửi các câu trả lời
→ Backend kiểm tra quiz vẫn thuộc khóa ACTIVE, PUBLISHED và tuition_fee = 0
→ Backend chấm trực tiếp nhưng không tạo quiz_attempt
→ Trả score, passed và lời giải thích
```

Luồng này vẫn chấm ở backend để không làm lộ đáp án đúng. Rate limit nên được áp dụng cho endpoint nộp quiz công khai.

## 8. API đã triển khai

Endpoint sử dụng prefix `/api`; frontend ánh xạ tại `frontend/src/configs/Apis.js`. Response có cấu trúc `{ status, message, data }`.

### Học viên

| Method | Endpoint | Mục đích |
|---|---|---|
| `GET` | `/api/students/me/learning/courses/{id}` | Lấy chương và nội dung bài được phép ôn |
| `GET` | `/api/students/me/learning/contents/{id}/flashcards` | Lấy flashcard, ưu tiên thẻ mới/đến hạn |
| `PUT` | `/api/students/me/learning/flashcards/{id}/review` | Lưu mức độ nhớ |
| `GET` | `/api/students/me/learning/flashcards/due` | Lấy thẻ đã có lịch ôn và đến hạn |
| `GET` | `/api/students/me/learning/contents/{id}/quizzes` | Lấy quiz và câu hỏi, không chứa đáp án đúng |
| `POST` | `/api/students/me/learning/quizzes/{id}/attempts` | Bắt đầu hoặc tiếp tục lần chưa nộp |
| `POST` | `/api/students/me/learning/attempts/{id}/submit` | Nộp và chấm quiz |
| `GET` | `/api/students/me/learning/quizzes/{id}/attempts` | Lịch sử và chi tiết kết quả của student hiện tại |

### Công khai cho khóa miễn phí

| Method | Endpoint | Mục đích |
|---|---|---|
| `GET` | `/api/public/learning/courses/{id}` | Lấy nội dung ôn tập của khóa miễn phí |
| `GET` | `/api/public/learning/contents/{id}/flashcards` | Xem flashcard, không lưu trạng thái |
| `GET` | `/api/public/learning/contents/{id}/quizzes` | Lấy quiz cùng câu hỏi đã xuất bản |
| `POST` | `/api/public/learning/quizzes/{id}/evaluate` | Chấm quiz tức thời, không lưu attempt |

### Quản trị

| Method | Endpoint | Mục đích |
|---|---|---|
| `GET/POST` | `/api/admin/learning/contents/{id}/flashcards` | Xem hoặc tạo flashcard |
| `PUT/DELETE` | `/api/admin/learning/flashcards/{id}` | Sửa hoặc ngừng sử dụng flashcard |
| `PUT` | `/api/admin/learning/contents/{id}/flashcards/order` | Sắp xếp toàn bộ thẻ của bài |
| `GET/POST` | `/api/admin/learning/contents/{id}/quizzes` | Xem hoặc tạo quiz |
| `PUT/DELETE` | `/api/admin/learning/quizzes/{id}` | Sửa hoặc lưu trữ quiz |
| `POST` | `/api/admin/learning/quizzes/{id}/questions` | Thêm câu hỏi và lựa chọn |
| `PUT/DELETE` | `/api/admin/learning/questions/{id}` | Sửa hoặc xóa câu hỏi của quiz nháp chưa có lượt làm |

## 9. Quy tắc bảo mật và toàn vẹn dữ liệu

- Nội dung trả phí chỉ được truy cập khi student hiện tại có enrollment hợp lệ cho khóa học.
- Dữ liệu `flashcard_review` và `quiz_attempt` chỉ được truy cập bởi chính student sở hữu bản ghi.
- Nội dung công khai chỉ được trả khi khóa học `ACTIVE`, `PUBLISHED` và có `tuition_fee = 0`.
- Endpoint công khai không được ghi vào `flashcard_review`, `quiz_attempt` hoặc `quiz_attempt_answer`.
- Khi truy cập khóa trả phí, enrollment, flashcard, quiz và course content phải thuộc cùng một khóa học.
- Foreign key đơn lẻ không đảm bảo được quan hệ xuyên nhiều bảng này; backend phải kiểm tra trước khi đọc hoặc ghi.
- Chỉ trả quiz có `status = 'PUBLISHED'` cho học viên.
- Không trả trường `quiz_option.is_correct` trước khi học viên nộp bài.
- Không cho sửa một `quiz_attempt` đã có `submitted_at`.
- Chấm và lưu toàn bộ bài quiz trong cùng một database transaction.
- Admin nên lưu trữ/ngừng kích hoạt nội dung đã phát sinh lịch sử thay vì xóa cứng.

## 10. Phương án “chỉ ôn bài đã được giáo viên dạy”

MVP mặc định cho phép ôn toàn bộ `course_content` đã xuất bản của khóa học.

Nếu sau này cần chỉ hiển thị nội dung đã được dạy trong lớp, bổ sung bảng tùy chọn:

```text
lesson_course_content
  - lesson_id
  - course_content_id
  - PRIMARY KEY (lesson_id, course_content_id)
```

Khi đó chỉ hiển thị nội dung nếu:

```text
lesson.status = 'COMPLETED'
AND lesson thuộc courseclass của enrollment
AND lesson đã liên kết với course_content
```

Bảng này không theo dõi tiến độ cá nhân; nó chỉ ánh xạ nội dung giáo trình với buổi học thực tế.

## 11. Tiêu chí nghiệm thu MVP

- Học viên hợp lệ xem được danh sách bài đã xuất bản của khóa học.
- Khách chưa đăng nhập xem được bài, flashcard và làm quiz của khóa miễn phí.
- Kết quả quiz của khách được chấm ở backend nhưng không tạo lịch sử trong database.
- Người dùng đăng nhập làm nội dung miễn phí được lưu lịch sử theo student mà không cần enrollment hoặc payment.
- Việc mở bài không tạo dữ liệu tiến độ.
- Admin quản lý được flashcard văn bản theo từng bài.
- Học viên lật thẻ, đánh giá mức độ nhớ và nhận được lịch ôn tiếp theo.
- Admin tạo và xuất bản được quiz hợp lệ.
- Đáp án đúng không bị lộ trước khi nộp.
- Backend tự chấm điểm và lưu lịch sử từng lần làm.
- Học viên không thể truy cập lịch ôn flashcard hoặc kết quả quiz của student khác.
- Không có chức năng âm thanh, phát âm, hình ảnh mẫu hoặc phần trăm hoàn thành khóa học.

## 12. Sử dụng và triển khai

### Database

- Database tạo mới: dùng `database_language_center_mysql.sql`, đã gồm 7 bảng tự học.
- Database đang tồn tại: chọn đúng database và chạy `migrate_add_learning_review_mysql.sql` một lần trước khi sử dụng tính năng.
- Migration không xóa dữ liệu cũ. Không chạy lại schema tạo mới lên database đang sử dụng.
- Hibernate vẫn dùng `ddl-auto=none`; ứng dụng không tự chạy migration.
- Chưa tự thực thi migration trên database của người dùng.

### Giao diện

- Học viên: “Khóa học của tôi” → “Vào học”, mở `/on-tap/:courseId`.
- Khách: trang chi tiết khóa miễn phí → “Ôn tập, flashcard và quiz”.
- Admin: Khóa học → Giáo trình → liên kết “Flashcard và quiz” dưới bài học.
- Tạo quiz nháp, thêm câu hỏi và đáp án, sau đó mở “Cài đặt quiz” để xuất bản.
- Câu đúng/sai có đúng 2 lựa chọn; mọi câu có đúng 1 đáp án đúng.
- Nội dung bài HTML được hiển thị trong iframe sandbox; chỉ mở liên kết tài liệu HTTP/HTTPS.

### Quy tắc lịch sử và giới hạn

- Lịch sử và mức độ nhớ dùng chung theo student, không tách theo lớp hoặc enrollment.
- Điểm được chuẩn hóa về thang 100; câu bỏ trống được 0 điểm.
- Backend từ chối câu hỏi trùng, câu hỏi/lựa chọn không thuộc quiz và lượt làm không thuộc học viên.
- Lượt chưa nộp được tiếp tục khi chọn “Làm quiz”; lượt đã nộp không thể nộp lại.
- Quiz đã có attempt không cho sửa câu hỏi, điểm đạt hoặc số lần tối đa. Tạo quiz mới nếu cần đổi đề.
- Sửa câu hỏi của quiz chưa có attempt cần chuyển về DRAFT trước.
- Khách làm quiz miễn phí không áp dụng giới hạn lượt theo tài khoản; có rate limit 20 lần nộp/phút/IP/backend instance.
- Nếu triển khai nhiều instance hoặc sau proxy, cấu hình rate limit tại gateway phù hợp; bộ giới hạn hiện tại dùng IP kết nối trực tiếp.
- Không có dữ liệu flashcard/quiz mẫu tự sinh. Admin tạo nội dung trên giao diện.

### Kiểm tra

- Backend: `mvn.cmd test` tại `backend/language-center-management` (dùng Maven cài sẵn nếu wrapper chưa chạy được).
- Frontend: `npm.cmd run lint`, `npm.cmd run build`.
- E2E: `npm.cmd run test:e2e -- e2e/learning.spec.js`; mock API kiểm tra khách/student/admin và viewport 320, 390, 768, 1280px.
- E2E mock không thay thế kiểm tra migration và transaction trên MySQL test.
