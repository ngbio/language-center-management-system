/*
  SEED BỔ SUNG 2: N5 cơ bản - Làm quen Hiragana và Katakana.
  Đối chiếu: seed_n5_kana_quizzes_mysql.sql đã đủ phần I–III (48 câu).
  File này thêm phần IV–VI: 3 chương, 3 nội dung, 3 quiz, 46 câu, 184 lựa chọn.
  Sau khi chạy hai file: 6 quiz, 94 câu, 376 lựa chọn.
  Giữ thứ tự câu và lựa chọn A/B/C/D = display_order 1/2/3/4.
  Hiệu chỉnh VI / Bài 1 / Câu 4: シユウ -> シュウ để đáp án shuu đúng.
  Giữ nguyên các lựa chọn nhiễu, kể cả 二 ở IV / Bài 2 / Câu 4.
  dzu dùng cho づ theo quy ước đề; ず dùng zu. Phần VII chưa có dữ liệu.

  Cách chạy:
  1. Chọn đúng database ứng dụng (USE ten_database;).
  2. Phải chạy thành công seed_n5_kana_quizzes_mysql.sql trước.
  3. Chạy TOÀN BỘ file này bằng MySQL client hỗ trợ DELIMITER, trong phiên riêng.
     Cần quyền CREATE ROUTINE, EXECUTE, ALTER ROUTINE và DML; các bảng dùng InnoDB.
  4. Chạy một lần. Nếu đã có chương 4 trở đi hoặc trùng tiêu đề bổ sung,
     báo lỗi và rollback; không chèn trùng, không sửa câu hỏi/lượt làm cũ.
     Không chạy với --force. CREATE/DROP PROCEDURE là DDL ngoài transaction.
     Nếu client dừng ở lỗi CALL, kiểm tra và xử lý nguyên nhân trước khi gọi lại
     CALL seed_n5_kana_quizzes_v2(); rồi DROP PROCEDURE seed_n5_kana_quizzes_v2;.
  5. Cuối file có truy vấn đối chiếu 46 đáp án mới.
  Không tạo enrollment, payment, attempt hoặc flashcard.
*/

SET NAMES utf8mb4;

DELIMITER $$
CREATE PROCEDURE seed_n5_kana_quizzes_v2()
BEGIN
    DECLARE v_course_id INT DEFAULT NULL;
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

    SELECT id INTO v_course_id FROM course
    WHERE course_code = 'JA-N5-KANA-BASIC'
      AND slug = 'n5-co-ban-hiragana-katakana'
    FOR UPDATE;
    IF v_course_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed N5 kana 2: run seed_n5_kana_quizzes_mysql.sql first.';
    END IF;

    IF (SELECT COUNT(*) FROM course_section WHERE course_id = v_course_id
        AND display_order BETWEEN 1 AND 3) <> 3 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed N5 kana 2: original sections 1-3 are required.';
    END IF;

    IF EXISTS (SELECT 1 FROM course_section WHERE course_id = v_course_id
        AND (display_order >= 4 OR title IN ('Katakana: Hàng ナ、ハ、マ、ヤ、ラ、ワ、ン', 'Kana: Âm đục - Biến âm', 'Kana: Trường âm, âm ghép, âm ngắt')))
       OR EXISTS (
        SELECT 1 FROM quiz q
        JOIN course_content c ON c.id = q.course_content_id
        JOIN course_section s ON s.id = c.section_id
        WHERE s.course_id = v_course_id
          AND q.title IN ('BTVN Hàng ナ、ハ、マ、ヤ、ラ、ワ、ン', 'BTVN Âm đục - Biến âm', 'BTVN Trường âm, âm ghép, âm ngắt')
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seed N5 kana 2: supplemental content/order already exists; no data was changed.';
    END IF;

    -- ============================================================
    -- Phần IV: BTVN Hàng ナ、ハ、マ、ヤ、ラ、ワ、ン
    -- Đáp án: 1B 2A 3C 4C 5B 6D 7A 8C 9B 10A 11D 12B 13A 14C 15D 16A
    -- ============================================================
    INSERT INTO course_section (course_id, title, description, display_order)
    VALUES (v_course_id, 'Katakana: Hàng ナ、ハ、マ、ヤ、ラ、ワ、ン', 'Luyện đọc chữ, chọn cách viết và nhận diện từ.', 4);
    SET v_section_id = LAST_INSERT_ID();

    INSERT INTO course_content (
        section_id, title, summary, content_html, content_type, display_order,
        is_preview, publication_status
    ) VALUES (
        v_section_id, 'BTVN Hàng ナ、ハ、マ、ヤ、ラ、ワ、ン',
        '16 câu: 5 câu chọn cách đọc, 5 câu chọn cách viết, 6 câu đọc hoặc viết từ.',
        '<h2>Hướng dẫn làm bài</h2><p>Chọn tab Quiz để làm 16 câu trắc nghiệm. Mỗi câu có 4 lựa chọn A, B, C, D và chỉ 1 đáp án đúng.</p><ol><li>Bài 1: Chọn cách đọc đúng (câu 1–5).</li><li>Bài 2: Chọn cách viết đúng (câu 6–10).</li><li>Bài 3: Chọn cách đọc hoặc viết đúng của từ (câu 11–16).</li></ol><p>Nộp bài để xem điểm và giải thích. Có thể làm lại để ôn tập.</p>',
        'EXERCISE', 1, TRUE, 'PUBLISHED'
    );
    SET v_content_id = LAST_INSERT_ID();
    INSERT INTO quiz (course_content_id, title, passing_score, max_attempts, status)
    VALUES (v_content_id, 'BTVN Hàng ナ、ハ、マ、ヤ、ラ、ワ、ン', 50, NULL, 'DRAFT');
    SET v_quiz_id = LAST_INSERT_ID();

    -- Câu 1: ロ -> B. ro
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 1: Chọn cách đọc đúng của ロ.', 'SINGLE_CHOICE', 'ロ đọc là ro.', 1, 1);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ko', FALSE, 1),
        (v_question_id, 'ro', TRUE, 2),
        (v_question_id, 'yo', FALSE, 3),
        (v_question_id, 'wo', FALSE, 4);

    -- Câu 2: ヨ -> A. yo
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 2: Chọn cách đọc đúng của ヨ.', 'SINGLE_CHOICE', 'ヨ đọc là yo.', 1, 2);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'yo', TRUE, 1),
        (v_question_id, 'mo', FALSE, 2),
        (v_question_id, 'ho', FALSE, 3),
        (v_question_id, 'wo', FALSE, 4);

    -- Câu 3: ネ -> C. ne
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 3: Chọn cách đọc đúng của ネ.', 'SINGLE_CHOICE', 'ネ đọc là ne.', 1, 3);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ho', FALSE, 1),
        (v_question_id, 'nu', FALSE, 2),
        (v_question_id, 'ne', TRUE, 3),
        (v_question_id, 'me', FALSE, 4);

    -- Câu 4: ヒ -> C. hi
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 4: Chọn cách đọc đúng của ヒ.', 'SINGLE_CHOICE', 'ヒ đọc là hi.', 1, 4);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'i', FALSE, 1),
        (v_question_id, 'se', FALSE, 2),
        (v_question_id, 'hi', TRUE, 3),
        (v_question_id, 'na', FALSE, 4);

    -- Câu 5: ワ -> B. wa
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 5: Chọn cách đọc đúng của ワ.', 'SINGLE_CHOICE', 'ワ đọc là wa.', 1, 5);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'u', FALSE, 1),
        (v_question_id, 'wa', TRUE, 2),
        (v_question_id, 'wo', FALSE, 3),
        (v_question_id, 'ku', FALSE, 4);

    -- Câu 6: nu -> D. ヌ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 1: Chọn cách viết Katakana đúng của nu.', 'SINGLE_CHOICE', 'nu viết bằng Katakana là ヌ.', 1, 6);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ス', FALSE, 1),
        (v_question_id, 'メ', FALSE, 2),
        (v_question_id, 'タ', FALSE, 3),
        (v_question_id, 'ヌ', TRUE, 4);

    -- Câu 7: ma -> A. マ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 2: Chọn cách viết Katakana đúng của ma.', 'SINGLE_CHOICE', 'ma viết bằng Katakana là マ.', 1, 7);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'マ', TRUE, 1),
        (v_question_id, 'ム', FALSE, 2),
        (v_question_id, 'ス', FALSE, 3),
        (v_question_id, 'ナ', FALSE, 4);

    -- Câu 8: re -> C. レ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 3: Chọn cách viết Katakana đúng của re.', 'SINGLE_CHOICE', 're viết bằng Katakana là レ.', 1, 8);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ル', FALSE, 1),
        (v_question_id, 'ラ', FALSE, 2),
        (v_question_id, 'レ', TRUE, 3),
        (v_question_id, 'フ', FALSE, 4);

    -- Câu 9: mi -> B. ミ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 4: Chọn cách viết Katakana đúng của mi.', 'SINGLE_CHOICE', 'mi viết bằng Katakana là ミ.', 1, 9);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, '二', FALSE, 1),
        (v_question_id, 'ミ', TRUE, 2),
        (v_question_id, 'メ', FALSE, 3),
        (v_question_id, 'モ', FALSE, 4);

    -- Câu 10: yu -> A. ユ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 5: Chọn cách viết Katakana đúng của yu.', 'SINGLE_CHOICE', 'yu viết bằng Katakana là ユ.', 1, 10);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ユ', TRUE, 1),
        (v_question_id, 'コ', FALSE, 2),
        (v_question_id, 'ヨ', FALSE, 3),
        (v_question_id, 'ロ', FALSE, 4);

    -- Câu 11: イヌ -> D. inu
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 1: Chọn cách đọc đúng của イヌ.', 'SINGLE_CHOICE', 'イヌ đọc là inu.', 1, 11);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'eta', FALSE, 1),
        (v_question_id, 'enu', FALSE, 2),
        (v_question_id, 'ita', FALSE, 3),
        (v_question_id, 'inu', TRUE, 4);

    -- Câu 12: ツノ -> B. tsuno
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 2: Chọn cách đọc đúng của ツノ.', 'SINGLE_CHOICE', 'ツノ đọc là tsuno.', 1, 12);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'shino', FALSE, 1),
        (v_question_id, 'tsuno', TRUE, 2),
        (v_question_id, 'shin', FALSE, 3),
        (v_question_id, 'tsun', FALSE, 4);

    -- Câu 13: スシ -> A. sushi
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 3: Chọn cách đọc đúng của スシ.', 'SINGLE_CHOICE', 'スシ đọc là sushi.', 1, 13);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'sushi', TRUE, 1),
        (v_question_id, 'sutsu', FALSE, 2),
        (v_question_id, 'nushi', FALSE, 3),
        (v_question_id, 'suso', FALSE, 4);

    -- Câu 14: natsu -> C. ナツ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 4: Chọn cách viết Katakana đúng của natsu.', 'SINGLE_CHOICE', 'natsu viết bằng Katakana là ナツ.', 1, 14);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ナシ', FALSE, 1),
        (v_question_id, 'ナソ', FALSE, 2),
        (v_question_id, 'ナツ', TRUE, 3),
        (v_question_id, 'ナン', FALSE, 4);

    -- Câu 15: fune -> D. フネ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 5: Chọn cách viết Katakana đúng của fune.', 'SINGLE_CHOICE', 'fune viết bằng Katakana là フネ.', 1, 15);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'レネ', FALSE, 1),
        (v_question_id, 'レホ', FALSE, 2),
        (v_question_id, 'フホ', FALSE, 3),
        (v_question_id, 'フネ', TRUE, 4);

    -- Câu 16: mame -> A. マメ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 6: Chọn cách viết Katakana đúng của mame.', 'SINGLE_CHOICE', 'mame viết bằng Katakana là マメ.', 1, 16);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'マメ', TRUE, 1),
        (v_question_id, 'ムメ', FALSE, 2),
        (v_question_id, 'マヌ', FALSE, 3),
        (v_question_id, 'ムヌ', FALSE, 4);

    IF (SELECT COUNT(*) FROM quiz_question WHERE quiz_id = v_quiz_id) <> 16
       OR EXISTS (
        SELECT qq.id FROM quiz_question qq
        LEFT JOIN quiz_option o ON o.question_id = qq.id
        WHERE qq.quiz_id = v_quiz_id
        GROUP BY qq.id HAVING COUNT(o.id) <> 4 OR SUM(o.is_correct) <> 1
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Seed N5 kana 2: quiz validation failed.';
    END IF;
    UPDATE quiz SET status = 'PUBLISHED' WHERE id = v_quiz_id;

    -- ============================================================
    -- Phần V: BTVN Âm đục - Biến âm
    -- Đáp án: 1C 2A 3C 4C 5B 6C 7B 8D 9C 10A 11B 12D 13C 14A 15C 16B
    -- ============================================================
    INSERT INTO course_section (course_id, title, description, display_order)
    VALUES (v_course_id, 'Kana: Âm đục - Biến âm', 'Luyện đọc chữ, chọn cách viết và nhận diện từ.', 5);
    SET v_section_id = LAST_INSERT_ID();

    INSERT INTO course_content (
        section_id, title, summary, content_html, content_type, display_order,
        is_preview, publication_status
    ) VALUES (
        v_section_id, 'BTVN Âm đục - Biến âm',
        '16 câu: 5 câu chọn cách đọc, 5 câu chọn cách viết, 6 câu đọc hoặc viết từ.',
        '<h2>Hướng dẫn làm bài</h2><p>Chọn tab Quiz để làm 16 câu trắc nghiệm. Mỗi câu có 4 lựa chọn A, B, C, D và chỉ 1 đáp án đúng.</p><ol><li>Bài 1: Chọn cách đọc đúng (câu 1–5).</li><li>Bài 2: Chọn cách viết đúng (câu 6–10).</li><li>Bài 3: Chọn cách đọc hoặc viết đúng của từ (câu 11–16).</li></ol><p>Nộp bài để xem điểm và giải thích. Có thể làm lại để ôn tập.</p>',
        'EXERCISE', 1, TRUE, 'PUBLISHED'
    );
    SET v_content_id = LAST_INSERT_ID();
    INSERT INTO quiz (course_content_id, title, passing_score, max_attempts, status)
    VALUES (v_content_id, 'BTVN Âm đục - Biến âm', 50, NULL, 'DRAFT');
    SET v_quiz_id = LAST_INSERT_ID();

    -- Câu 1: だ -> C. da
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 1: Chọn cách đọc đúng của だ.', 'SINGLE_CHOICE', 'だ đọc là da.', 1, 1);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ga', FALSE, 1),
        (v_question_id, 'pa', FALSE, 2),
        (v_question_id, 'da', TRUE, 3),
        (v_question_id, 'ba', FALSE, 4);

    -- Câu 2: ぽ -> A. po
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 2: Chọn cách đọc đúng của ぽ.', 'SINGLE_CHOICE', 'ぽ đọc là po.', 1, 2);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'po', TRUE, 1),
        (v_question_id, 'bo', FALSE, 2),
        (v_question_id, 'go', FALSE, 3),
        (v_question_id, 'do', FALSE, 4);

    -- Câu 3: ジ -> C. ji
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 3: Chọn cách đọc đúng của ジ.', 'SINGLE_CHOICE', 'ジ đọc là ji.', 1, 3);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'bi', FALSE, 1),
        (v_question_id, 'pi', FALSE, 2),
        (v_question_id, 'ji', TRUE, 3),
        (v_question_id, 'gi', FALSE, 4);

    -- Câu 4: ペ -> C. pe
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 4: Chọn cách đọc đúng của ペ.', 'SINGLE_CHOICE', 'ペ đọc là pe.', 1, 4);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'be', FALSE, 1),
        (v_question_id, 'de', FALSE, 2),
        (v_question_id, 'pe', TRUE, 3),
        (v_question_id, 'ge', FALSE, 4);

    -- Câu 5: プ -> B. pu
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 5: Chọn cách đọc đúng của プ.', 'SINGLE_CHOICE', 'プ đọc là pu.', 1, 5);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'bu', FALSE, 1),
        (v_question_id, 'pu', TRUE, 2),
        (v_question_id, 'zu', FALSE, 3),
        (v_question_id, 'gu', FALSE, 4);

    -- Câu 6: pi -> C. ピ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 1: Chọn cách viết Katakana đúng của pi.', 'SINGLE_CHOICE', 'pi viết bằng Katakana là ピ.', 1, 6);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'パ', FALSE, 1),
        (v_question_id, 'ポ', FALSE, 2),
        (v_question_id, 'ピ', TRUE, 3),
        (v_question_id, 'ぺ', FALSE, 4);

    -- Câu 7: ge -> B. ゲ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 2: Chọn cách viết Katakana đúng của ge.', 'SINGLE_CHOICE', 'ge viết bằng Katakana là ゲ.', 1, 7);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'グ', FALSE, 1),
        (v_question_id, 'ゲ', TRUE, 2),
        (v_question_id, 'ズ', FALSE, 3),
        (v_question_id, 'ブ', FALSE, 4);

    -- Câu 8: pa -> D. ぱ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 3: Chọn cách viết Hiragana đúng của pa.', 'SINGLE_CHOICE', 'pa viết bằng Hiragana là ぱ.', 1, 8);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ぺ', FALSE, 1),
        (v_question_id, 'ぴ', FALSE, 2),
        (v_question_id, 'ぷ', FALSE, 3),
        (v_question_id, 'ぱ', TRUE, 4);

    -- Câu 9: dzu -> C. づ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 4: Chọn cách viết Hiragana đúng của dzu.', 'SINGLE_CHOICE', 'dzu viết bằng Hiragana là づ. Đề dùng dzu để phân biệt づ với ず (zu).', 1, 9);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ず', FALSE, 1),
        (v_question_id, 'じ', FALSE, 2),
        (v_question_id, 'づ', TRUE, 3),
        (v_question_id, 'で', FALSE, 4);

    -- Câu 10: zo -> A. ぞ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 5: Chọn cách viết Hiragana đúng của zo.', 'SINGLE_CHOICE', 'zo viết bằng Hiragana là ぞ.', 1, 10);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ぞ', TRUE, 1),
        (v_question_id, 'ず', FALSE, 2),
        (v_question_id, 'ど', FALSE, 3),
        (v_question_id, 'ぜ', FALSE, 4);

    -- Câu 11: みず -> B. mizu
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 1: Chọn cách đọc đúng của みず.', 'SINGLE_CHOICE', 'みず đọc là mizu.', 1, 11);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'misu', FALSE, 1),
        (v_question_id, 'mizu', TRUE, 2),
        (v_question_id, 'midzu', FALSE, 3),
        (v_question_id, 'migu', FALSE, 4);

    -- Câu 12: サラダ -> D. sarada
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 2: Chọn cách đọc đúng của サラダ.', 'SINGLE_CHOICE', 'サラダ đọc là sarada.', 1, 12);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'safuta', FALSE, 1),
        (v_question_id, 'safuda', FALSE, 2),
        (v_question_id, 'sarata', FALSE, 3),
        (v_question_id, 'sarada', TRUE, 4);

    -- Câu 13: ごぜん -> C. gozen
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 3: Chọn cách đọc đúng của ごぜん.', 'SINGLE_CHOICE', 'ごぜん đọc là gozen.', 1, 13);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'gozon', FALSE, 1),
        (v_question_id, 'koson', FALSE, 2),
        (v_question_id, 'gozen', TRUE, 3),
        (v_question_id, 'kozon', FALSE, 4);

    -- Câu 14: posuto -> A. ポスト
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 4: Chọn cách viết Katakana đúng của posuto.', 'SINGLE_CHOICE', 'posuto viết bằng Katakana là ポスト.', 1, 14);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ポスト', TRUE, 1),
        (v_question_id, 'ピスト', FALSE, 2),
        (v_question_id, 'ポスタ', FALSE, 3),
        (v_question_id, 'ピスタ', FALSE, 4);

    -- Câu 15: enpitsu -> C. えんぴつ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 5: Chọn cách viết Hiragana đúng của enpitsu.', 'SINGLE_CHOICE', 'enpitsu viết bằng Hiragana là えんぴつ.', 1, 15);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'えんびつ', FALSE, 1),
        (v_question_id, 'えんびす', FALSE, 2),
        (v_question_id, 'えんぴつ', TRUE, 3),
        (v_question_id, 'えんぴす', FALSE, 4);

    -- Câu 16: purezento -> B. プレゼント
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 6: Chọn cách viết Katakana đúng của purezento.', 'SINGLE_CHOICE', 'purezento viết bằng Katakana là プレゼント.', 1, 16);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ブレセント', FALSE, 1),
        (v_question_id, 'プレゼント', TRUE, 2),
        (v_question_id, 'ペレセント', FALSE, 3),
        (v_question_id, 'ペレゼント', FALSE, 4);

    IF (SELECT COUNT(*) FROM quiz_question WHERE quiz_id = v_quiz_id) <> 16
       OR EXISTS (
        SELECT qq.id FROM quiz_question qq
        LEFT JOIN quiz_option o ON o.question_id = qq.id
        WHERE qq.quiz_id = v_quiz_id
        GROUP BY qq.id HAVING COUNT(o.id) <> 4 OR SUM(o.is_correct) <> 1
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Seed N5 kana 2: quiz validation failed.';
    END IF;
    UPDATE quiz SET status = 'PUBLISHED' WHERE id = v_quiz_id;

    -- ============================================================
    -- Phần VI: BTVN Trường âm, âm ghép, âm ngắt
    -- Đáp án: 1C 2B 3D 4D 5A 6B 7B 8C 9D 10B 11A 12C 13C 14B
    -- ============================================================
    INSERT INTO course_section (course_id, title, description, display_order)
    VALUES (v_course_id, 'Kana: Trường âm, âm ghép, âm ngắt', 'Luyện đọc chữ, chọn cách viết và nhận diện từ.', 6);
    SET v_section_id = LAST_INSERT_ID();

    INSERT INTO course_content (
        section_id, title, summary, content_html, content_type, display_order,
        is_preview, publication_status
    ) VALUES (
        v_section_id, 'BTVN Trường âm, âm ghép, âm ngắt',
        '14 câu: 4 câu chọn cách đọc, 4 câu chọn cách viết, 6 câu đọc hoặc viết từ.',
        '<h2>Hướng dẫn làm bài</h2><p>Chọn tab Quiz để làm 14 câu trắc nghiệm. Mỗi câu có 4 lựa chọn A, B, C, D và chỉ 1 đáp án đúng.</p><ol><li>Bài 1: Chọn cách đọc đúng (câu 1–4).</li><li>Bài 2: Chọn cách viết đúng (câu 5–8).</li><li>Bài 3: Chọn cách đọc hoặc viết đúng của từ (câu 9–14).</li></ol><p>Nộp bài để xem điểm và giải thích. Có thể làm lại để ôn tập.</p>',
        'EXERCISE', 1, TRUE, 'PUBLISHED'
    );
    SET v_content_id = LAST_INSERT_ID();
    INSERT INTO quiz (course_content_id, title, passing_score, max_attempts, status)
    VALUES (v_content_id, 'BTVN Trường âm, âm ghép, âm ngắt', 50, NULL, 'DRAFT');
    SET v_quiz_id = LAST_INSERT_ID();

    -- Câu 1: ぎょ -> C. gyo
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 1: Chọn cách đọc đúng của ぎょ.', 'SINGLE_CHOICE', 'ぎょ đọc là gyo.', 1, 1);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'gya', FALSE, 1),
        (v_question_id, 'gyu', FALSE, 2),
        (v_question_id, 'gyo', TRUE, 3),
        (v_question_id, 'giyo', FALSE, 4);

    -- Câu 2: ちゃ -> B. cha
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 2: Chọn cách đọc đúng của ちゃ.', 'SINGLE_CHOICE', 'ちゃ đọc là cha.', 1, 2);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'chiya', FALSE, 1),
        (v_question_id, 'cha', TRUE, 2),
        (v_question_id, 'chou', FALSE, 3),
        (v_question_id, 'chu', FALSE, 4);

    -- Câu 3: ミュウ -> D. myuu
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 3: Chọn cách đọc đúng của ミュウ.', 'SINGLE_CHOICE', 'ミュウ đọc là myuu.', 1, 3);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'chu', FALSE, 1),
        (v_question_id, 'chiyu', FALSE, 2),
        (v_question_id, 'ryu', FALSE, 3),
        (v_question_id, 'myuu', TRUE, 4);

    -- Câu 4: シュウ -> D. shuu
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 1 · Câu 4: Chọn cách đọc đúng của シュウ.', 'SINGLE_CHOICE', 'シュウ đọc là shuu. Dùng chữ ュ nhỏ để tạo âm ghép shu; ウ kéo dài nguyên âm.', 1, 4);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'hiyo', FALSE, 1),
        (v_question_id, 'hyou', FALSE, 2),
        (v_question_id, 'kyou', FALSE, 3),
        (v_question_id, 'shuu', TRUE, 4);

    -- Câu 5: kyou -> A. きょう
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 1: Chọn cách viết Hiragana đúng của kyou.', 'SINGLE_CHOICE', 'kyou viết bằng Hiragana là きょう.', 1, 5);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'きょう', TRUE, 1),
        (v_question_id, 'きよ', FALSE, 2),
        (v_question_id, 'きよう', FALSE, 3),
        (v_question_id, 'きいよう', FALSE, 4);

    -- Câu 6: rya -> B. りゃ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 2: Chọn cách viết Hiragana đúng của rya.', 'SINGLE_CHOICE', 'rya viết bằng Hiragana là りゃ.', 1, 6);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'らあ', FALSE, 1),
        (v_question_id, 'りゃ', TRUE, 2),
        (v_question_id, 'りあ', FALSE, 3),
        (v_question_id, 'らゃ', FALSE, 4);

    -- Câu 7: ja -> B. ジャ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 3: Chọn cách viết Katakana đúng của ja.', 'SINGLE_CHOICE', 'ja viết bằng Katakana là ジャ.', 1, 7);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'シャ', FALSE, 1),
        (v_question_id, 'ジャ', TRUE, 2),
        (v_question_id, 'ヅヤ', FALSE, 3),
        (v_question_id, 'ヅャ', FALSE, 4);

    -- Câu 8: pyuu -> C. ピュウ
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 2 · Câu 4: Chọn cách viết Katakana đúng của pyuu.', 'SINGLE_CHOICE', 'pyuu viết bằng Katakana là ピュウ.', 1, 8);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ビュウ', FALSE, 1),
        (v_question_id, 'ビョウ', FALSE, 2),
        (v_question_id, 'ピュウ', TRUE, 3),
        (v_question_id, 'ピャウ', FALSE, 4);

    -- Câu 9: ひこうき -> D. hikouki
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 1: Chọn cách đọc đúng của ひこうき.', 'SINGLE_CHOICE', 'ひこうき đọc là hikouki.', 1, 9);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'hokoki', FALSE, 1),
        (v_question_id, 'hikuuki', FALSE, 2),
        (v_question_id, 'hikiuki', FALSE, 3),
        (v_question_id, 'hikouki', TRUE, 4);

    -- Câu 10: コンピューター -> B. konpyuutaa
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 2: Chọn cách đọc đúng của コンピューター.', 'SINGLE_CHOICE', 'コンピューター đọc là konpyuutaa.', 1, 10);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'kosopiyuta', FALSE, 1),
        (v_question_id, 'konpyuutaa', TRUE, 2),
        (v_question_id, 'konbyuuta', FALSE, 3),
        (v_question_id, 'konpyutai', FALSE, 4);

    -- Câu 11: せっけん -> A. sekken
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 3: Chọn cách đọc đúng của せっけん.', 'SINGLE_CHOICE', 'せっけん đọc là sekken.', 1, 11);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'sekken', TRUE, 1),
        (v_question_id, 'kessen', FALSE, 2),
        (v_question_id, 'setsuken', FALSE, 3),
        (v_question_id, 'seken', FALSE, 4);

    -- Câu 12: jisshuusei -> C. じっしゅうせい
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 4: Chọn cách viết Hiragana đúng của jisshuusei.', 'SINGLE_CHOICE', 'jisshuusei viết bằng Hiragana là じっしゅうせい.', 1, 12);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'じいしゅうせえ', FALSE, 1),
        (v_question_id, 'じっしゅせ', FALSE, 2),
        (v_question_id, 'じっしゅうせい', TRUE, 3),
        (v_question_id, 'じしゅうせい', FALSE, 4);

    -- Câu 13: sakkaa -> C. サッカー
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 5: Chọn cách viết Katakana đúng của sakkaa.', 'SINGLE_CHOICE', 'sakkaa viết bằng Katakana là サッカー.', 1, 13);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'サカア', FALSE, 1),
        (v_question_id, 'シャカー', FALSE, 2),
        (v_question_id, 'サッカー', TRUE, 3),
        (v_question_id, 'サツカア', FALSE, 4);

    -- Câu 14: basuketto -> B. バスケット
    INSERT INTO quiz_question (quiz_id, question_text, question_type, explanation, points, display_order)
    VALUES (v_quiz_id, 'Bài 3 · Câu 6: Chọn cách viết Katakana đúng của basuketto.', 'SINGLE_CHOICE', 'basuketto viết bằng Katakana là バスケット.', 1, 14);
    SET v_question_id = LAST_INSERT_ID();
    INSERT INTO quiz_option (question_id, option_text, is_correct, display_order) VALUES
        (v_question_id, 'ハスケート', FALSE, 1),
        (v_question_id, 'バスケット', TRUE, 2),
        (v_question_id, 'パッスケート', FALSE, 3),
        (v_question_id, 'バスケーット', FALSE, 4);

    IF (SELECT COUNT(*) FROM quiz_question WHERE quiz_id = v_quiz_id) <> 14
       OR EXISTS (
        SELECT qq.id FROM quiz_question qq
        LEFT JOIN quiz_option o ON o.question_id = qq.id
        WHERE qq.quiz_id = v_quiz_id
        GROUP BY qq.id HAVING COUNT(o.id) <> 4 OR SUM(o.is_correct) <> 1
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Seed N5 kana 2: quiz validation failed.';
    END IF;
    UPDATE quiz SET status = 'PUBLISHED' WHERE id = v_quiz_id;

    -- Cập nhật giới thiệu để phản ánh ba phần bổ sung.
    UPDATE course SET
        short_description = 'Khóa miễn phí luyện đọc và nhận diện kana với 6 quiz, 94 câu trắc nghiệm.',
        description = 'Bài tập nhập môn dành cho người chuẩn bị học N5: Hiragana, Katakana, âm đục, biến âm, trường âm, âm ghép và âm ngắt. Đây không phải toàn bộ chương trình N5.',
        syllabus_summary = 'Hiragana あ–な; Hiragana は–ん; Katakana ア–タ; Katakana ナ–ン; âm đục - biến âm; trường âm, âm ghép, âm ngắt. Mỗi quiz có ba phần: đọc chữ, viết chữ, đọc/viết từ.',
        total_sessions = total_sessions + 3
    WHERE id = v_course_id;

    COMMIT;
    SELECT v_course_id AS course_id, 'JA-N5-KANA-BASIC' AS course_code,
           3 AS added_quizzes, 46 AS added_questions, 184 AS added_options;
END$$
DELIMITER ;

CALL seed_n5_kana_quizzes_v2();
DROP PROCEDURE seed_n5_kana_quizzes_v2;

-- Bảng đáp án bổ sung (46 dòng).
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
  AND s.display_order BETWEEN 4 AND 6
ORDER BY s.display_order, qq.display_order;
