/*
  SEED: N5 cơ bản - Làm quen Hiragana và Katakana
  Nguồn: 3 nhóm BTVN do người dùng cung cấp; giữ nguyên thứ tự câu và lựa chọn.
  Kết quả: 1 khóa miễn phí, 3 chương, 3 nội dung bài tập, 3 quiz,
           48 câu SINGLE_CHOICE, 192 lựa chọn (mỗi câu đúng 1 lựa chọn).
  A/B/C/D tương ứng quiz_option.display_order = 1/2/3/4.
  Nội dung chỉ là phần nhập môn kana, không phải trọn chương trình JLPT N5.

  Cách chạy:
  1. Chọn database ứng dụng (ví dụ USE LanguageCenterDB;) trước khi chạy file.
  2. DB phải có các bảng catalog và quiz. DB cũ cần chạy
     migrate_add_learning_review_mysql.sql trước.
  3. Chạy TOÀN BỘ file bằng MySQL client hỗ trợ DELIMITER, trong phiên riêng.
     Tài khoản chạy seed cần quyền CREATE ROUTINE, EXECUTE, ALTER ROUTINE và DML.
  4. Seed chạy một lần. Nếu course_code/slug đã tồn tại, báo lỗi và rollback,
     không sửa/xóa nội dung cũ hay lịch sử học viên.
  5. Các bảng phải dùng InnoDB như schema gốc. Không chạy với --force.
     CREATE/DROP PROCEDURE là DDL ngoài transaction; dữ liệu seed ở trong transaction.
     Nếu client dừng sau lỗi CALL, routine có thể còn lại: kiểm tra lỗi trước khi
     chạy lại CALL seed_n5_kana_quizzes_v1(); rồi DROP PROCEDURE seed_n5_kana_quizzes_v1;.
  File này chỉ tạo dữ liệu nội dung; không tạo enrollment, payment, attempt hoặc flashcard.
  Giá miễn phí (0), quiz đạt từ 50/100, mỗi câu 1 điểm, không giới hạn lần làm.
*/

SET NAMES utf8mb4;

DELIMITER $$
CREATE PROCEDURE seed_n5_kana_quizzes_v1()
BEGIN
    DECLARE v_language_id INT;
    DECLARE v_level_id INT;
    DECLARE v_course_id INT;
    DECLARE v_section_id INT;
    DECLARE v_content_id INT;
    DECLARE v_quiz_id BIGINT;
    DECLARE v_question_id BIGINT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    IF EXISTS (
        SELECT 1 FROM course
        WHERE course_code = 'JA-N5-KANA-BASIC'
           OR slug = 'n5-co-ban-hiragana-katakana'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed N5 kana: course code/slug already exists; no data was changed.';
    END IF;

    -- Dùng mã JA giống các seed N5 hiện có; không thay đổi catalog có sẵn.
    SET v_language_id = (SELECT id FROM language WHERE language_code = 'JA');
    IF v_language_id IS NULL THEN
        INSERT INTO language (language_code, language_name, description, status)
        VALUES ('JA', 'Tiếng Nhật', 'Ngôn ngữ Nhật Bản.', 'ACTIVE');
        SET v_language_id = LAST_INSERT_ID();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM language WHERE id = v_language_id AND status = 'ACTIVE') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Seed N5 kana: language JA must be ACTIVE.';
    END IF;

    SET v_level_id = (SELECT id FROM level WHERE language_id = v_language_id AND level_code = 'N5');
    IF v_level_id IS NULL THEN
        INSERT INTO level (language_id, level_code, level_name, description, display_order, status)
        VALUES (v_language_id, 'N5', 'N5', 'Trình độ sơ cấp tiếng Nhật N5.', 1, 'ACTIVE');
        SET v_level_id = LAST_INSERT_ID();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM level WHERE id = v_level_id AND status = 'ACTIVE') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Seed N5 kana: level N5 must be ACTIVE.';
    END IF;

    INSERT INTO course (
        level_id, course_code, course_name, slug, short_description, description,
        target_audience, prerequisites, learning_outcomes, syllabus_summary,
        tuition_fee, total_sessions, status, publication_status, published_at
    ) VALUES (
        v_level_id, 'JA-N5-KANA-BASIC', 'N5 cơ bản - Làm quen Hiragana và Katakana',
        'n5-co-ban-hiragana-katakana',
        'Khóa miễn phí luyện đọc và nhận diện kana với 3 quiz, 48 câu trắc nghiệm.',
        'Bài tập nhập môn dành cho người chuẩn bị học N5: hai nhóm Hiragana và một nhóm Katakana. Đây không phải toàn bộ chương trình N5.',
        'Người mới bắt đầu học tiếng Nhật.',
        'Không yêu cầu kiến thức tiếng Nhật trước đó.',
        'Nhận diện các kana trong bài tập và ghép cách đọc, cách viết của từ đơn giản.',
        'Hiragana あ–な; Hiragana は–ん; Katakana ア–タ. Mỗi quiz có ba phần: đọc chữ, viết chữ, đọc/viết từ.',
        0, 3, 'ACTIVE', 'PUBLISHED', NOW()
    );
    SET v_course_id = LAST_INSERT_ID();

    -- ============================================================
    -- Quiz 1: BTVN Hàng あ、か、さ、た、な
    -- Đáp án: 1B 2C 3C 4A 5B 6B 7A 8C 9D 10A 11B 12C 13A 14A 15A 16D
    -- ============================================================
    INSERT INTO course_section (course_id, title, description, display_order)
    VALUES (v_course_id, 'Hiragana: Hàng あ、か、さ、た、な', 'Luyện đọc chữ, chọn cách viết và nhận diện từ.', 1);
    SET v_section_id = LAST_INSERT_ID();

    INSERT INTO course_content (
        section_id, title, summary, content_html, content_type, display_order,
        is_preview, publication_status
    ) VALUES (
        v_section_id, 'BTVN Hàng あ、か、さ、た、な',
        '16 câu: 5 câu chọn cách đọc, 5 câu chọn cách viết, 6 câu đọc hoặc viết từ.',
        '<h2>Hướng dẫn làm bài</h2><p>Chọn tab Quiz để làm 16 câu trắc nghiệm. Mỗi câu có 4 lựa chọn theo thứ tự A, B, C, D và chỉ có 1 đáp án đúng.</p><ol><li>Bài 1: Chọn cách đọc đúng (câu 1–5).</li><li>Bài 2: Chọn cách viết đúng (câu 6–10).</li><li>Bài 3: Chọn cách đọc hoặc viết đúng của từ (câu 11–16).</li></ol><p>Nộp bài để xem điểm và giải thích. Có thể làm lại để ôn tập.</p>',
        'EXERCISE', 1, TRUE, 'PUBLISHED'
    );
    SET v_content_id = LAST_INSERT_ID();
    INSERT INTO quiz (course_content_id, title, passing_score, max_attempts, status)
    VALUES (v_content_id, 'BTVN Hàng あ、か、さ、た、な', 50, NULL, 'DRAFT');
    SET v_quiz_id = LAST_INSERT_ID();

    -- Câu 1: お -> B. o
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 1: Chọn cách đọc đúng của お.', 'SINGLE_CHOICE', 'お đọc là o.', 1, 1);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'a', FALSE, 1),
        (v_question_id, 'o', TRUE, 2),
        (v_question_id, 'e', FALSE, 3),
        (v_question_id, 'u', FALSE, 4);

    -- Câu 2: ち -> C. chi
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 2: Chọn cách đọc đúng của ち.', 'SINGLE_CHOICE', 'ち đọc là chi.', 1, 2);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'tsu', FALSE, 1),
        (v_question_id, 'shi', FALSE, 2),
        (v_question_id, 'chi', TRUE, 3),
        (v_question_id, 'ni', FALSE, 4);

    -- Câu 3: く -> C. ku
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 3: Chọn cách đọc đúng của く.', 'SINGLE_CHOICE', 'く đọc là ku.', 1, 3);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ka', FALSE, 1),
        (v_question_id, 'ko', FALSE, 2),
        (v_question_id, 'ku', TRUE, 3),
        (v_question_id, 'nu', FALSE, 4);

    -- Câu 4: ね -> A. ne
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 4: Chọn cách đọc đúng của ね.', 'SINGLE_CHOICE', 'ね đọc là ne.', 1, 4);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ne', TRUE, 1),
        (v_question_id, 'nu', FALSE, 2),
        (v_question_id, 'ta', FALSE, 3),
        (v_question_id, 'ko', FALSE, 4);

    -- Câu 5: し -> B. shi
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 5: Chọn cách đọc đúng của し.', 'SINGLE_CHOICE', 'し đọc là shi.', 1, 5);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'su', FALSE, 1),
        (v_question_id, 'shi', TRUE, 2),
        (v_question_id, 'chi', FALSE, 3),
        (v_question_id, 'sa', FALSE, 4);

    -- Câu 6: ki -> B. き
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 1: Chọn cách viết Hiragana đúng của ki.', 'SINGLE_CHOICE', 'ki viết bằng Hiragana là き.', 1, 6);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'さ', FALSE, 1),
        (v_question_id, 'き', TRUE, 2),
        (v_question_id, 'な', FALSE, 3),
        (v_question_id, 'そ', FALSE, 4);

    -- Câu 7: to -> A. と
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 2: Chọn cách viết Hiragana đúng của to.', 'SINGLE_CHOICE', 'to viết bằng Hiragana là と.', 1, 7);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'と', TRUE, 1),
        (v_question_id, 'あ', FALSE, 2),
        (v_question_id, 'そ', FALSE, 3),
        (v_question_id, 'ち', FALSE, 4);

    -- Câu 8: no -> C. の
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 3: Chọn cách viết Hiragana đúng của no.', 'SINGLE_CHOICE', 'no viết bằng Hiragana là の.', 1, 8);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'す', FALSE, 1),
        (v_question_id, 'と', FALSE, 2),
        (v_question_id, 'の', TRUE, 3),
        (v_question_id, 'こ', FALSE, 4);

    -- Câu 9: a -> D. あ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 4: Chọn cách viết Hiragana đúng của a.', 'SINGLE_CHOICE', 'a viết bằng Hiragana là あ.', 1, 9);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'な', FALSE, 1),
        (v_question_id, 'お', FALSE, 2),
        (v_question_id, 'う', FALSE, 3),
        (v_question_id, 'あ', TRUE, 4);

    -- Câu 10: su -> A. す
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 5: Chọn cách viết Hiragana đúng của su.', 'SINGLE_CHOICE', 'su viết bằng Hiragana là す.', 1, 10);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'す', TRUE, 1),
        (v_question_id, 'え', FALSE, 2),
        (v_question_id, 'い', FALSE, 3),
        (v_question_id, 'か', FALSE, 4);

    -- Câu 11: あかい -> B. akai
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 1: Chọn cách đọc đúng của あかい.', 'SINGLE_CHOICE', 'あかい đọc là akai.', 1, 11);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'okei', FALSE, 1),
        (v_question_id, 'akai', TRUE, 2),
        (v_question_id, 'ukui', FALSE, 3),
        (v_question_id, 'ekoi', FALSE, 4);

    -- Câu 12: いぬ -> C. inu
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 2: Chọn cách đọc đúng của いぬ.', 'SINGLE_CHOICE', 'いぬ đọc là inu.', 1, 12);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ane', FALSE, 1),
        (v_question_id, 'neko', FALSE, 2),
        (v_question_id, 'inu', TRUE, 3),
        (v_question_id, 'ono', FALSE, 4);

    -- Câu 13: にち -> A. nichi
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 3: Chọn cách đọc đúng của にち.', 'SINGLE_CHOICE', 'にち đọc là nichi.', 1, 13);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'nichi', TRUE, 1),
        (v_question_id, 'nitsu', FALSE, 2),
        (v_question_id, 'sechi', FALSE, 3),
        (v_question_id, 'kochi', FALSE, 4);

    -- Câu 14: sushi -> A. すし
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 4: Chọn cách viết Hiragana đúng của sushi.', 'SINGLE_CHOICE', 'sushi viết bằng Hiragana là すし.', 1, 14);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'すし', TRUE, 1),
        (v_question_id, 'こし', FALSE, 2),
        (v_question_id, 'さし', FALSE, 3),
        (v_question_id, 'すい', FALSE, 4);

    -- Câu 15: eki -> A. えき
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 5: Chọn cách viết Hiragana đúng của eki.', 'SINGLE_CHOICE', 'eki viết bằng Hiragana là えき.', 1, 15);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'えき', TRUE, 1),
        (v_question_id, 'うき', FALSE, 2),
        (v_question_id, 'えさ', FALSE, 3),
        (v_question_id, 'うさ', FALSE, 4);

    -- Câu 16: suika -> D. すいか
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 6: Chọn cách viết Hiragana đúng của suika.', 'SINGLE_CHOICE', 'suika viết bằng Hiragana là すいか.', 1, 16);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'すいき', FALSE, 1),
        (v_question_id, 'そいか', FALSE, 2),
        (v_question_id, 'せいか', FALSE, 3),
        (v_question_id, 'すいか', TRUE, 4);

    -- Xuất bản sau khi đủ câu và lựa chọn.
    UPDATE quiz SET status = 'PUBLISHED' WHERE id = v_quiz_id;

    -- ============================================================
    -- Quiz 2: BTVN Hàng は、ま、や、ら、わ、ん
    -- Đáp án: 1B 2C 3A 4B 5C 6D 7D 8A 9B 10C 11D 12C 13D 14A 15B 16C
    -- ============================================================
    INSERT INTO course_section (course_id, title, description, display_order)
    VALUES (v_course_id, 'Hiragana: Hàng は、ま、や、ら、わ、ん', 'Luyện đọc chữ, chọn cách viết và nhận diện từ.', 2);
    SET v_section_id = LAST_INSERT_ID();

    INSERT INTO course_content (
        section_id, title, summary, content_html, content_type, display_order,
        is_preview, publication_status
    ) VALUES (
        v_section_id, 'BTVN Hàng は、ま、や、ら、わ、ん',
        '16 câu: 5 câu chọn cách đọc, 5 câu chọn cách viết, 6 câu đọc hoặc viết từ.',
        '<h2>Hướng dẫn làm bài</h2><p>Chọn tab Quiz để làm 16 câu trắc nghiệm. Mỗi câu có 4 lựa chọn theo thứ tự A, B, C, D và chỉ có 1 đáp án đúng.</p><ol><li>Bài 1: Chọn cách đọc đúng (câu 1–5).</li><li>Bài 2: Chọn cách viết đúng (câu 6–10).</li><li>Bài 3: Chọn cách đọc hoặc viết đúng của từ (câu 11–16).</li></ol><p>Nộp bài để xem điểm và giải thích. Có thể làm lại để ôn tập.</p>',
        'EXERCISE', 1, TRUE, 'PUBLISHED'
    );
    SET v_content_id = LAST_INSERT_ID();
    INSERT INTO quiz (course_content_id, title, passing_score, max_attempts, status)
    VALUES (v_content_id, 'BTVN Hàng は、ま、や、ら、わ、ん', 50, NULL, 'DRAFT');
    SET v_quiz_id = LAST_INSERT_ID();

    -- Câu 1: は -> B. ha
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 1: Chọn cách đọc đúng của は.', 'SINGLE_CHOICE', 'は đọc là ha. Đây là cách đọc chữ đơn lẻ; は dùng làm trợ từ chủ đề được đọc là wa.', 1, 1);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ho', FALSE, 1),
        (v_question_id, 'ha', TRUE, 2),
        (v_question_id, 'ke', FALSE, 3),
        (v_question_id, 'ma', FALSE, 4);

    -- Câu 2: れ -> C. re
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 2: Chọn cách đọc đúng của れ.', 'SINGLE_CHOICE', 'れ đọc là re.', 1, 2);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'wa', FALSE, 1),
        (v_question_id, 'ne', FALSE, 2),
        (v_question_id, 're', TRUE, 3),
        (v_question_id, 'nu', FALSE, 4);

    -- Câu 3: よ -> A. yo
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 3: Chọn cách đọc đúng của よ.', 'SINGLE_CHOICE', 'よ đọc là yo.', 1, 3);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'yo', TRUE, 1),
        (v_question_id, 'yu', FALSE, 2),
        (v_question_id, 'mo', FALSE, 3),
        (v_question_id, 'shi', FALSE, 4);

    -- Câu 4: む -> B. mu
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 4: Chọn cách đọc đúng của む.', 'SINGLE_CHOICE', 'む đọc là mu.', 1, 4);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'su', FALSE, 1),
        (v_question_id, 'mu', TRUE, 2),
        (v_question_id, 'e', FALSE, 3),
        (v_question_id, 'na', FALSE, 4);

    -- Câu 5: わ -> C. wa
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 5: Chọn cách đọc đúng của わ.', 'SINGLE_CHOICE', 'わ đọc là wa.', 1, 5);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ha', FALSE, 1),
        (v_question_id, 'ru', FALSE, 2),
        (v_question_id, 'wa', TRUE, 3),
        (v_question_id, 'wo', FALSE, 4);

    -- Câu 6: wo -> D. を
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 1: Chọn cách viết Hiragana đúng của wo.', 'SINGLE_CHOICE', 'wo viết bằng Hiragana là を. Đề dùng cách chuyển tự wo cho を; khi làm trợ từ, を thường được đọc là o.', 1, 6);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'れ', FALSE, 1),
        (v_question_id, 'え', FALSE, 2),
        (v_question_id, 'わ', FALSE, 3),
        (v_question_id, 'を', TRUE, 4);

    -- Câu 7: ru -> D. る
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 2: Chọn cách viết Hiragana đúng của ru.', 'SINGLE_CHOICE', 'ru viết bằng Hiragana là る.', 1, 7);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ろ', FALSE, 1),
        (v_question_id, 'そ', FALSE, 2),
        (v_question_id, 'よ', FALSE, 3),
        (v_question_id, 'る', TRUE, 4);

    -- Câu 8: me -> A. め
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 3: Chọn cách viết Hiragana đúng của me.', 'SINGLE_CHOICE', 'me viết bằng Hiragana là め.', 1, 8);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'め', TRUE, 1),
        (v_question_id, 'ぬ', FALSE, 2),
        (v_question_id, 'ね', FALSE, 3),
        (v_question_id, 'れ', FALSE, 4);

    -- Câu 9: yu -> B. ゆ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 4: Chọn cách viết Hiragana đúng của yu.', 'SINGLE_CHOICE', 'yu viết bằng Hiragana là ゆ.', 1, 9);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'う', FALSE, 1),
        (v_question_id, 'ゆ', TRUE, 2),
        (v_question_id, 'や', FALSE, 3),
        (v_question_id, 'も', FALSE, 4);

    -- Câu 10: n -> C. ん
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 5: Chọn cách viết Hiragana đúng của n.', 'SINGLE_CHOICE', 'n viết bằng Hiragana là ん.', 1, 10);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'え', FALSE, 1),
        (v_question_id, 'て', FALSE, 2),
        (v_question_id, 'ん', TRUE, 3),
        (v_question_id, 'を', FALSE, 4);

    -- Câu 11: はな -> D. hana
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 1: Chọn cách đọc đúng của はな.', 'SINGLE_CHOICE', 'はな đọc là hana.', 1, 11);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'hota', FALSE, 1),
        (v_question_id, 'hona', FALSE, 2),
        (v_question_id, 'hata', FALSE, 3),
        (v_question_id, 'hana', TRUE, 4);

    -- Câu 12: はたけ -> C. hatake
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 2: Chọn cách đọc đúng của はたけ.', 'SINGLE_CHOICE', 'はたけ đọc là hatake.', 1, 12);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'honaku', FALSE, 1),
        (v_question_id, 'honake', FALSE, 2),
        (v_question_id, 'hatake', TRUE, 3),
        (v_question_id, 'hataku', FALSE, 4);

    -- Câu 13: みせ -> D. mise
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 3: Chọn cách đọc đúng của みせ.', 'SINGLE_CHOICE', 'みせ đọc là mise.', 1, 13);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'mese', FALSE, 1),
        (v_question_id, 'meso', FALSE, 2),
        (v_question_id, 'miso', FALSE, 3),
        (v_question_id, 'mise', TRUE, 4);

    -- Câu 14: yoko -> A. よこ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 4: Chọn cách viết Hiragana đúng của yoko.', 'SINGLE_CHOICE', 'yoko viết bằng Hiragana là よこ.', 1, 14);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'よこ', TRUE, 1),
        (v_question_id, 'やこ', FALSE, 2),
        (v_question_id, 'よに', FALSE, 3),
        (v_question_id, 'やに', FALSE, 4);

    -- Câu 15: sora -> B. そら
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 5: Chọn cách viết Hiragana đúng của sora.', 'SINGLE_CHOICE', 'sora viết bằng Hiragana là そら.', 1, 15);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'すろ', FALSE, 1),
        (v_question_id, 'そら', TRUE, 2),
        (v_question_id, 'せれ', FALSE, 3),
        (v_question_id, 'さり', FALSE, 4);

    -- Câu 16: kani -> C. かに
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 6: Chọn cách viết Hiragana đúng của kani.', 'SINGLE_CHOICE', 'kani viết bằng Hiragana là かに.', 1, 16);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'さこ', FALSE, 1),
        (v_question_id, 'たに', FALSE, 2),
        (v_question_id, 'かに', TRUE, 3),
        (v_question_id, 'かこ', FALSE, 4);

    -- Xuất bản sau khi đủ câu và lựa chọn.
    UPDATE quiz SET status = 'PUBLISHED' WHERE id = v_quiz_id;

    -- ============================================================
    -- Quiz 3: BTVN Hàng ア、カ、サ、タ
    -- Đáp án: 1C 2B 3D 4B 5A 6B 7A 8C 9D 10A 11B 12C 13A 14A 15A 16D
    -- ============================================================
    INSERT INTO course_section (course_id, title, description, display_order)
    VALUES (v_course_id, 'Katakana: Hàng ア、カ、サ、タ', 'Luyện đọc chữ, chọn cách viết và nhận diện từ.', 3);
    SET v_section_id = LAST_INSERT_ID();

    INSERT INTO course_content (
        section_id, title, summary, content_html, content_type, display_order,
        is_preview, publication_status
    ) VALUES (
        v_section_id, 'BTVN Hàng ア、カ、サ、タ',
        '16 câu: 5 câu chọn cách đọc, 5 câu chọn cách viết, 6 câu đọc hoặc viết từ.',
        '<h2>Hướng dẫn làm bài</h2><p>Chọn tab Quiz để làm 16 câu trắc nghiệm. Mỗi câu có 4 lựa chọn theo thứ tự A, B, C, D và chỉ có 1 đáp án đúng.</p><ol><li>Bài 1: Chọn cách đọc đúng (câu 1–5).</li><li>Bài 2: Chọn cách viết đúng (câu 6–10).</li><li>Bài 3: Chọn cách đọc hoặc viết đúng của từ (câu 11–16).</li></ol><p>Nộp bài để xem điểm và giải thích. Có thể làm lại để ôn tập.</p>',
        'EXERCISE', 1, TRUE, 'PUBLISHED'
    );
    SET v_content_id = LAST_INSERT_ID();
    INSERT INTO quiz (course_content_id, title, passing_score, max_attempts, status)
    VALUES (v_content_id, 'BTVN Hàng ア、カ、サ、タ', 50, NULL, 'DRAFT');
    SET v_quiz_id = LAST_INSERT_ID();

    -- Câu 1: エ -> C. e
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 1: Chọn cách đọc đúng của エ.', 'SINGLE_CHOICE', 'エ đọc là e.', 1, 1);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'a', FALSE, 1),
        (v_question_id, 'o', FALSE, 2),
        (v_question_id, 'e', TRUE, 3),
        (v_question_id, 'u', FALSE, 4);

    -- Câu 2: カ -> B. ka
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 2: Chọn cách đọc đúng của カ.', 'SINGLE_CHOICE', 'カ đọc là ka.', 1, 2);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ta', FALSE, 1),
        (v_question_id, 'ka', TRUE, 2),
        (v_question_id, 'to', FALSE, 3),
        (v_question_id, 'su', FALSE, 4);

    -- Câu 3: チ -> D. chi
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 3: Chọn cách đọc đúng của チ.', 'SINGLE_CHOICE', 'チ đọc là chi.', 1, 3);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ki', FALSE, 1),
        (v_question_id, 'u', FALSE, 2),
        (v_question_id, 'te', FALSE, 3),
        (v_question_id, 'chi', TRUE, 4);

    -- Câu 4: ソ -> B. so
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 4: Chọn cách đọc đúng của ソ.', 'SINGLE_CHOICE', 'ソ đọc là so.', 1, 4);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ko', FALSE, 1),
        (v_question_id, 'so', TRUE, 2),
        (v_question_id, 'ke', FALSE, 3),
        (v_question_id, 'shi', FALSE, 4);

    -- Câu 5: ツ -> A. tsu
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 5: Chọn cách đọc đúng của ツ.', 'SINGLE_CHOICE', 'ツ đọc là tsu.', 1, 5);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'tsu', TRUE, 1),
        (v_question_id, 'shi', FALSE, 2),
        (v_question_id, 'ku', FALSE, 3),
        (v_question_id, 'i', FALSE, 4);

    -- Câu 6: ki -> B. キ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 1: Chọn cách viết Katakana đúng của ki.', 'SINGLE_CHOICE', 'ki viết bằng Katakana là キ.', 1, 6);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'チ', FALSE, 1),
        (v_question_id, 'キ', TRUE, 2),
        (v_question_id, 'オ', FALSE, 3),
        (v_question_id, 'ケ', FALSE, 4);

    -- Câu 7: ta -> A. タ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 2: Chọn cách viết Katakana đúng của ta.', 'SINGLE_CHOICE', 'ta viết bằng Katakana là タ.', 1, 7);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'タ', TRUE, 1),
        (v_question_id, 'テ', FALSE, 2),
        (v_question_id, 'イ', FALSE, 3),
        (v_question_id, 'ク', FALSE, 4);

    -- Câu 8: se -> C. セ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 3: Chọn cách viết Katakana đúng của se.', 'SINGLE_CHOICE', 'se viết bằng Katakana là セ.', 1, 8);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ツ', FALSE, 1),
        (v_question_id, 'ス', FALSE, 2),
        (v_question_id, 'セ', TRUE, 3),
        (v_question_id, 'コ', FALSE, 4);

    -- Câu 9: i -> D. イ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 4: Chọn cách viết Katakana đúng của i.', 'SINGLE_CHOICE', 'i viết bằng Katakana là イ.', 1, 9);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'カ', FALSE, 1),
        (v_question_id, 'ア', FALSE, 2),
        (v_question_id, 'ケ', FALSE, 3),
        (v_question_id, 'イ', TRUE, 4);

    -- Câu 10: to -> A. ト
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 5: Chọn cách viết Katakana đúng của to.', 'SINGLE_CHOICE', 'to viết bằng Katakana là ト.', 1, 10);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ト', TRUE, 1),
        (v_question_id, 'ソ', FALSE, 2),
        (v_question_id, 'キ', FALSE, 3),
        (v_question_id, 'エ', FALSE, 4);

    -- Câu 11: アツイ -> B. atsui
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 1: Chọn cách đọc đúng của アツイ.', 'SINGLE_CHOICE', 'アツイ đọc là atsui.', 1, 11);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ashi', FALSE, 1),
        (v_question_id, 'atsui', TRUE, 2),
        (v_question_id, 'okii', FALSE, 3),
        (v_question_id, 'utsui', FALSE, 4);

    -- Câu 12: アサ -> C. asa
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 2: Chọn cách đọc đúng của アサ.', 'SINGLE_CHOICE', 'アサ đọc là asa.', 1, 12);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'oso', FALSE, 1),
        (v_question_id, 'ase', FALSE, 2),
        (v_question_id, 'asa', TRUE, 3),
        (v_question_id, 'kesa', FALSE, 4);

    -- Câu 13: ツキ -> A. tsuki
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 3: Chọn cách đọc đúng của ツキ.', 'SINGLE_CHOICE', 'ツキ đọc là tsuki.', 1, 13);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'tsuki', TRUE, 1),
        (v_question_id, 'suki', FALSE, 2),
        (v_question_id, 'teko', FALSE, 3),
        (v_question_id, 'shiki', FALSE, 4);

    -- Câu 14: osoi -> A. オソイ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 4: Chọn cách viết Katakana đúng của osoi.', 'SINGLE_CHOICE', 'osoi viết bằng Katakana là オソイ.', 1, 14);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'オソイ', TRUE, 1),
        (v_question_id, 'イシウ', FALSE, 2),
        (v_question_id, 'タクイ', FALSE, 3),
        (v_question_id, 'サケエ', FALSE, 4);

    -- Câu 15: akai -> A. アカイ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 5: Chọn cách viết Katakana đúng của akai.', 'SINGLE_CHOICE', 'akai viết bằng Katakana là アカイ.', 1, 15);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'アカイ', TRUE, 1),
        (v_question_id, 'アキウ', FALSE, 2),
        (v_question_id, 'スケイ', FALSE, 3),
        (v_question_id, 'ソテイ', FALSE, 4);

    -- Câu 16: chichi -> D. チチ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 6: Chọn cách viết Katakana đúng của chichi.', 'SINGLE_CHOICE', 'chichi viết bằng Katakana là チチ.', 1, 16);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'シシ', FALSE, 1),
        (v_question_id, 'キキ', FALSE, 2),
        (v_question_id, 'テテ', FALSE, 3),
        (v_question_id, 'チチ', TRUE, 4);

    -- Xuất bản sau khi đủ câu và lựa chọn.
    UPDATE quiz SET status = 'PUBLISHED' WHERE id = v_quiz_id;

    -- Kiểm tra cấu trúc trước COMMIT; mọi lỗi rollback toàn bộ dữ liệu seed.
    IF (SELECT COUNT(*) FROM quiz q
        JOIN course_content c ON c.id = q.course_content_id
        JOIN course_section s ON s.id = c.section_id WHERE s.course_id = v_course_id) <> 3
       OR (SELECT COUNT(*) FROM quiz_question qq
        JOIN quiz q ON q.id = qq.quiz_id
        JOIN course_content c ON c.id = q.course_content_id
        JOIN course_section s ON s.id = c.section_id WHERE s.course_id = v_course_id) <> 48
       OR EXISTS (
        SELECT qq.id FROM quiz_question qq
        JOIN quiz q ON q.id = qq.quiz_id
        JOIN course_content c ON c.id = q.course_content_id
        JOIN course_section s ON s.id = c.section_id
        LEFT JOIN quiz_option o ON o.question_id = qq.id
        WHERE s.course_id = v_course_id
        GROUP BY qq.id HAVING COUNT(o.id) <> 4 OR SUM(o.is_correct) <> 1
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Seed N5 kana: quiz validation failed.';
    END IF;

    COMMIT;
    SELECT v_course_id AS course_id, 'JA-N5-KANA-BASIC' AS course_code,
           3 AS quizzes, 48 AS questions, 192 AS options;
END$$
DELIMITER ;

CALL seed_n5_kana_quizzes_v1();
DROP PROCEDURE seed_n5_kana_quizzes_v1;

-- Bảng đáp án để đối chiếu sau khi seed thành công (48 dòng).
SELECT s.display_order AS quiz_number, q.title AS quiz_title,
       qq.display_order AS question_number, qq.question_text,
       CHAR(64 + o.display_order) AS correct_letter, o.option_text AS correct_answer
FROM course co
JOIN course_section s ON s.course_id = co.id
JOIN course_content c ON c.section_id = s.id
JOIN quiz q ON q.course_content_id = c.id
JOIN quiz_question qq ON qq.quiz_id = q.id
JOIN quiz_option o ON o.question_id = qq.id AND o.is_correct = TRUE
WHERE co.course_code = 'JA-N5-KANA-BASIC'
ORDER BY s.display_order, qq.display_order;
