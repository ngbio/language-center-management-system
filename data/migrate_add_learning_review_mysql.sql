-- Run once against the existing selected database after backup.
-- No production execution is performed by the application. DDL is not transactional in MySQL.
-- Learning review MVP: shared text cards and quizzes, student-owned practice history.
CREATE TABLE flashcard (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_content_id INT NOT NULL,
    front_text VARCHAR(500) NOT NULL,
    back_text TEXT NOT NULL,
    example_sentence TEXT NULL,
    display_order INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_flashcard_content FOREIGN KEY (course_content_id) REFERENCES course_content(id),
    CONSTRAINT uq_flashcard_order UNIQUE (course_content_id, display_order),
    CONSTRAINT ck_flashcard_order CHECK (display_order > 0),
    CONSTRAINT ck_flashcard_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB;

CREATE TABLE flashcard_review (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    flashcard_id BIGINT NOT NULL,
    mastery_level VARCHAR(20) NOT NULL,
    repetition_count INT NOT NULL DEFAULT 0,
    interval_days INT NOT NULL DEFAULT 0,
    last_reviewed_at DATETIME NULL,
    next_review_at DATETIME NULL,
    CONSTRAINT fk_card_review_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_card_review_card FOREIGN KEY (flashcard_id) REFERENCES flashcard(id),
    CONSTRAINT uq_card_review UNIQUE (student_id, flashcard_id),
    CONSTRAINT ck_card_review_level CHECK (mastery_level IN ('NEW', 'AGAIN', 'HARD', 'REMEMBERED')),
    CONSTRAINT ck_card_review_counts CHECK (repetition_count >= 0 AND interval_days >= 0),
    INDEX ix_card_review_due (student_id, next_review_at)
) ENGINE=InnoDB;

CREATE TABLE quiz (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_content_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    passing_score DECIMAL(5,2) NOT NULL DEFAULT 50,
    max_attempts INT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    CONSTRAINT fk_quiz_content FOREIGN KEY (course_content_id) REFERENCES course_content(id),
    CONSTRAINT ck_quiz_score CHECK (passing_score BETWEEN 0 AND 100),
    CONSTRAINT ck_quiz_attempts CHECK (max_attempts IS NULL OR max_attempts > 0),
    CONSTRAINT ck_quiz_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
) ENGINE=InnoDB;

CREATE TABLE quiz_question (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(30) NOT NULL,
    explanation TEXT NULL,
    points DECIMAL(6,2) NOT NULL DEFAULT 1,
    display_order INT NOT NULL,
    CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(id),
    CONSTRAINT uq_question_order UNIQUE (quiz_id, display_order),
    CONSTRAINT ck_question_points CHECK (points > 0),
    CONSTRAINT ck_question_order CHECK (display_order > 0),
    CONSTRAINT ck_question_type CHECK (question_type IN ('SINGLE_CHOICE', 'TRUE_FALSE'))
) ENGINE=InnoDB;

CREATE TABLE quiz_option (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL,
    CONSTRAINT fk_option_question FOREIGN KEY (question_id) REFERENCES quiz_question(id),
    CONSTRAINT uq_option_order UNIQUE (question_id, display_order),
    CONSTRAINT ck_option_order CHECK (display_order > 0)
) ENGINE=InnoDB;

CREATE TABLE quiz_attempt (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    quiz_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,
    score DECIMAL(5,2) NULL,
    passed BOOLEAN NULL,
    started_at DATETIME NOT NULL,
    submitted_at DATETIME NULL,
    CONSTRAINT fk_attempt_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_attempt_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(id),
    CONSTRAINT uq_quiz_attempt UNIQUE (student_id, quiz_id, attempt_number),
    CONSTRAINT ck_attempt_number CHECK (attempt_number > 0),
    CONSTRAINT ck_attempt_score CHECK (score IS NULL OR score BETWEEN 0 AND 100)
) ENGINE=InnoDB;

CREATE TABLE quiz_attempt_answer (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_option_id BIGINT NULL,
    is_correct BOOLEAN NOT NULL,
    points_awarded DECIMAL(6,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_answer_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempt(id),
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES quiz_question(id),
    CONSTRAINT fk_answer_option FOREIGN KEY (selected_option_id) REFERENCES quiz_option(id),
    CONSTRAINT uq_attempt_answer UNIQUE (attempt_id, question_id),
    CONSTRAINT ck_answer_points CHECK (points_awarded >= 0)
) ENGINE=InnoDB;
