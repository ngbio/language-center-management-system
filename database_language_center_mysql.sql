/*
  Database: LanguageCenterDB
  Database engine: MySQL 8.0.16+

  Naming convention:
  - Table names stay compatible with the current project.
  - Every column uses snake_case.
  - Each relationship has exactly one foreign-key column.

  WARNING: This script drops the existing database and all of its data.
  Back up any data that must be preserved before running it.
*/

DROP DATABASE IF EXISTS LanguageCenterDB;

CREATE DATABASE LanguageCenterDB
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE LanguageCenterDB;

CREATE TABLE role (
    id          INT NOT NULL AUTO_INCREMENT,
    role_code   VARCHAR(20) NOT NULL,
    role_name   VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_role_code UNIQUE (role_code),
    CONSTRAINT uq_role_name UNIQUE (role_name)
) ENGINE=InnoDB;

CREATE TABLE `user` (
    id              INT NOT NULL AUTO_INCREMENT,
    role_id         INT NOT NULL,
    username        VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    email           VARCHAR(150) NOT NULL,
    phone_number    VARCHAR(20) NULL,
    address         VARCHAR(255) NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_user_username UNIQUE (username),
    CONSTRAINT uq_user_email UNIQUE (email),
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT ck_user_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'))
) ENGINE=InnoDB;

CREATE TABLE notification (
    id                  INT NOT NULL AUTO_INCREMENT,
    user_id             INT NOT NULL,
    title               VARCHAR(200) NOT NULL,
    content             TEXT NOT NULL,
    notification_type   VARCHAR(30) NOT NULL DEFAULT 'SYSTEM',
    is_read             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at             DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id) REFERENCES `user`(id),
    CONSTRAINT ck_notification_type
        CHECK (notification_type IN ('SYSTEM', 'ENROLLMENT', 'PAYMENT', 'SCHEDULE'))
) ENGINE=InnoDB;

CREATE TABLE student (
    id              INT NOT NULL AUTO_INCREMENT,
    user_id         INT NOT NULL,
    student_code    VARCHAR(20) NOT NULL,
    date_of_birth   DATE NULL,
    gender          VARCHAR(10) NULL,
    avatar          VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_student_user UNIQUE (user_id),
    CONSTRAINT uq_student_code UNIQUE (student_code),
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES `user`(id),
    CONSTRAINT ck_student_gender
        CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER'))
) ENGINE=InnoDB;

CREATE TABLE teacher (
    id                  INT NOT NULL AUTO_INCREMENT,
    user_id             INT NOT NULL,
    teacher_code        VARCHAR(20) NOT NULL,
    specialization      VARCHAR(150) NULL,
    degree              VARCHAR(200) NULL,
    experience_years    INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_teacher_user UNIQUE (user_id),
    CONSTRAINT uq_teacher_code UNIQUE (teacher_code),
    CONSTRAINT fk_teacher_user FOREIGN KEY (user_id) REFERENCES `user`(id),
    CONSTRAINT ck_teacher_experience_years CHECK (experience_years >= 0)
) ENGINE=InnoDB;

CREATE TABLE language (
    id              INT NOT NULL AUTO_INCREMENT,
    language_code   VARCHAR(20) NOT NULL,
    language_name   VARCHAR(100) NOT NULL,
    description     VARCHAR(500) NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    CONSTRAINT uq_language_code UNIQUE (language_code),
    CONSTRAINT uq_language_name UNIQUE (language_name),
    CONSTRAINT ck_language_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB;

CREATE TABLE level (
    id              INT NOT NULL AUTO_INCREMENT,
    language_id     INT NOT NULL,
    level_code      VARCHAR(20) NOT NULL,
    level_name      VARCHAR(100) NOT NULL,
    description     VARCHAR(500) NULL,
    display_order   INT NOT NULL DEFAULT 1,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    CONSTRAINT fk_level_language
        FOREIGN KEY (language_id) REFERENCES language(id),
    CONSTRAINT uq_level_language_code UNIQUE (language_id, level_code),
    CONSTRAINT uq_level_language_name UNIQUE (language_id, level_name),
    CONSTRAINT ck_level_display_order CHECK (display_order > 0),
    CONSTRAINT ck_level_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB;

CREATE TABLE course (
    id                  INT NOT NULL AUTO_INCREMENT,
    level_id            INT NOT NULL,
    course_code         VARCHAR(30) NOT NULL,
    course_name         VARCHAR(200) NOT NULL,
    slug                VARCHAR(220) NOT NULL,
    short_description   VARCHAR(500) NULL,
    description         VARCHAR(1000) NULL,
    thumbnail_url       VARCHAR(500) NULL,
    banner_url          VARCHAR(500) NULL,
    target_audience     TEXT NULL,
    prerequisites       TEXT NULL,
    learning_outcomes   TEXT NULL,
    syllabus_summary    TEXT NULL,
    certificate_info    VARCHAR(500) NULL,
    tuition_fee         DECIMAL(18,2) NOT NULL,
    total_sessions      INT NOT NULL,
    duration_hours      INT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    publication_status  VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at        DATETIME NULL,
    is_featured         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_course_code UNIQUE (course_code),
    CONSTRAINT uq_course_slug UNIQUE (slug),
    CONSTRAINT fk_course_level FOREIGN KEY (level_id) REFERENCES level(id),
    CONSTRAINT ck_course_tuition_fee CHECK (tuition_fee >= 0),
    CONSTRAINT ck_course_total_sessions CHECK (total_sessions > 0),
    CONSTRAINT ck_course_duration_hours
        CHECK (duration_hours IS NULL OR duration_hours > 0),
    CONSTRAINT ck_course_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_course_publication_status
        CHECK (publication_status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_course_published_at CHECK (
        publication_status <> 'PUBLISHED' OR published_at IS NOT NULL
    )
) ENGINE=InnoDB;

CREATE TABLE course_section (
    id              INT NOT NULL AUTO_INCREMENT,
    course_id       INT NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT NULL,
    display_order   INT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_course_section_course
        FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT uq_course_section_order UNIQUE (course_id, display_order),
    CONSTRAINT ck_course_section_display_order CHECK (display_order > 0)
) ENGINE=InnoDB;

CREATE TABLE course_content (
    id                  INT NOT NULL AUTO_INCREMENT,
    section_id          INT NOT NULL,
    title               VARCHAR(255) NOT NULL,
    summary             TEXT NULL,
    content_html        LONGTEXT NULL,
    audio_url           VARCHAR(500) NULL,
    video_url           VARCHAR(500) NULL,
    document_url        VARCHAR(500) NULL,
    content_type        VARCHAR(30) NOT NULL DEFAULT 'LESSON',
    display_order       INT NOT NULL DEFAULT 1,
    is_preview          BOOLEAN NOT NULL DEFAULT FALSE,
    publication_status  VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    PRIMARY KEY (id),
    CONSTRAINT fk_course_content_section
        FOREIGN KEY (section_id) REFERENCES course_section(id),
    CONSTRAINT uq_course_content_order UNIQUE (section_id, display_order),
    CONSTRAINT ck_course_content_type
        CHECK (content_type IN ('LESSON', 'VOCABULARY', 'GRAMMAR', 'LISTENING', 'EXERCISE')),
    CONSTRAINT ck_course_content_display_order CHECK (display_order > 0),
    CONSTRAINT ck_course_content_publication_status
        CHECK (publication_status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
) ENGINE=InnoDB;

CREATE TABLE room (
    id          INT NOT NULL AUTO_INCREMENT,
    room_code   VARCHAR(30) NOT NULL,
    room_name   VARCHAR(100) NOT NULL,
    capacity    INT NOT NULL,
    location    VARCHAR(255) NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    CONSTRAINT uq_room_code UNIQUE (room_code),
    CONSTRAINT ck_room_capacity CHECK (capacity > 0),
    CONSTRAINT ck_room_status
        CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'INACTIVE'))
) ENGINE=InnoDB;

CREATE TABLE courseclass (
    id                      INT NOT NULL AUTO_INCREMENT,
    course_id               INT NOT NULL,
    teacher_id              INT NULL,
    class_code              VARCHAR(30) NOT NULL,
    class_name              VARCHAR(200) NOT NULL,
    start_date              DATE NOT NULL,
    end_date                DATE NOT NULL,
    max_students            INT NOT NULL,
    applied_tuition_fee     DECIMAL(18,2) NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_courseclass_code UNIQUE (class_code),
    CONSTRAINT fk_courseclass_course
        FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_courseclass_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher(id),
    CONSTRAINT ck_courseclass_date_range CHECK (end_date > start_date),
    CONSTRAINT ck_courseclass_max_students CHECK (max_students > 0),
    CONSTRAINT ck_courseclass_tuition_fee CHECK (applied_tuition_fee >= 0),
    CONSTRAINT ck_courseclass_status
        CHECK (status IN ('DRAFT', 'OPEN', 'FULL', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
) ENGINE=InnoDB;

CREATE TABLE classschedule (
    id                  INT NOT NULL AUTO_INCREMENT,
    course_class_id     INT NOT NULL,
    room_id             INT NULL,
    day_of_week         TINYINT NOT NULL,
    start_time          TIME NOT NULL,
    end_time            TIME NOT NULL,
    delivery_mode       VARCHAR(20) NOT NULL DEFAULT 'IN_PERSON',
    meeting_url         VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_classschedule_courseclass
        FOREIGN KEY (course_class_id) REFERENCES courseclass(id),
    CONSTRAINT fk_classschedule_room FOREIGN KEY (room_id) REFERENCES room(id),
    CONSTRAINT uq_classschedule_class_time
        UNIQUE (course_class_id, day_of_week, start_time),
    CONSTRAINT ck_classschedule_day_of_week CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_classschedule_time_range CHECK (end_time > start_time),
    CONSTRAINT ck_classschedule_delivery_mode
        CHECK (delivery_mode IN ('IN_PERSON', 'ONLINE')),
    CONSTRAINT ck_classschedule_location CHECK (
        (delivery_mode = 'IN_PERSON' AND room_id IS NOT NULL AND meeting_url IS NULL)
        OR (delivery_mode = 'ONLINE' AND room_id IS NULL AND meeting_url IS NOT NULL)
    )
) ENGINE=InnoDB;

CREATE TABLE lesson (
    id                  INT NOT NULL AUTO_INCREMENT,
    class_schedule_id   INT NOT NULL,
    topic               VARCHAR(255) NULL,
    lesson_date         DATE NOT NULL,
    meeting_url         VARCHAR(500) NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    PRIMARY KEY (id),
    CONSTRAINT fk_lesson_classschedule
        FOREIGN KEY (class_schedule_id) REFERENCES classschedule(id),
    CONSTRAINT uq_lesson_schedule_date
        UNIQUE (class_schedule_id, lesson_date),
    CONSTRAINT ck_lesson_status
        CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
) ENGINE=InnoDB;

CREATE TABLE enrollment (
    id                      INT NOT NULL AUTO_INCREMENT,
    student_id              INT NOT NULL,
    course_class_id         INT NOT NULL,
    enrollment_date         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    amount_due              DECIMAL(18,2) NOT NULL,
    enrollment_status       VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    confirmed_at            DATETIME NULL,
    cancelled_at            DATETIME NULL,
    cancellation_reason     VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_enrollment_student
        FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_enrollment_courseclass
        FOREIGN KEY (course_class_id) REFERENCES courseclass(id),
    CONSTRAINT uq_enrollment_student_class
        UNIQUE (student_id, course_class_id),
    CONSTRAINT ck_enrollment_amount_due CHECK (amount_due >= 0),
    CONSTRAINT ck_enrollment_status
        CHECK (enrollment_status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT ck_enrollment_payment_status
        CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'CANCELLED'))
) ENGINE=InnoDB;

CREATE TABLE payment (
    id                          INT NOT NULL AUTO_INCREMENT,
    enrollment_id               INT NOT NULL,
    transaction_code            VARCHAR(100) NOT NULL,
    method                      VARCHAR(30) NOT NULL,
    amount                      DECIMAL(18,2) NOT NULL,
    status                      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at                  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at                DATETIME NULL,
    reference_code              VARCHAR(150) NULL,
    error_message               VARCHAR(500) NULL,
    successful_enrollment_id    INT GENERATED ALWAYS AS
        (CASE WHEN status = 'PAID' THEN enrollment_id ELSE NULL END) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uq_payment_transaction_code UNIQUE (transaction_code),
    CONSTRAINT uq_payment_one_success_per_enrollment
        UNIQUE (successful_enrollment_id),
    CONSTRAINT fk_payment_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollment(id),
    CONSTRAINT ck_payment_amount CHECK (amount > 0),
    CONSTRAINT ck_payment_method
        CHECK (method IN ('MOMO', 'ZALOPAY')),
    CONSTRAINT ck_payment_status
        CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'CANCELLED'))
) ENGINE=InnoDB;

CREATE TABLE attendance (
    id                  INT NOT NULL AUTO_INCREMENT,
    lesson_id           INT NOT NULL,
    enrollment_id       INT NOT NULL,
    status              VARCHAR(20) NOT NULL,
    note                VARCHAR(500) NULL,
    attendance_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_attendance_lesson FOREIGN KEY (lesson_id) REFERENCES lesson(id),
    CONSTRAINT fk_attendance_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollment(id),
    CONSTRAINT uq_attendance_lesson_enrollment
        UNIQUE (lesson_id, enrollment_id),
    CONSTRAINT ck_attendance_status
        CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED'))
) ENGINE=InnoDB;

CREATE INDEX ix_notification_user_read
    ON notification(user_id, is_read, created_at);
CREATE INDEX ix_level_language_order
    ON level(language_id, display_order);
CREATE INDEX ix_course_level_status
    ON course(level_id, status);
CREATE INDEX ix_course_publication_featured
    ON course(publication_status, is_featured);
CREATE INDEX ix_course_content_section_status
    ON course_content(section_id, publication_status);
CREATE INDEX ix_courseclass_course_status
    ON courseclass(course_id, status);
CREATE INDEX ix_courseclass_teacher
    ON courseclass(teacher_id);
CREATE INDEX ix_classschedule_room_time
    ON classschedule(room_id, day_of_week, start_time, end_time);
CREATE INDEX ix_lesson_date_status
    ON lesson(lesson_date, status);
CREATE INDEX ix_enrollment_class_status
    ON enrollment(course_class_id, enrollment_status);
CREATE INDEX ix_enrollment_student_date
    ON enrollment(student_id, enrollment_date DESC);
CREATE INDEX ix_attendance_lesson
    ON attendance(lesson_id);

INSERT INTO role (role_code, role_name) VALUES
('STUDENT', 'Student'),
('TEACHER', 'Teacher'),
('CONSULTANT', 'Consultant'),
('ADMIN', 'Administrator');

INSERT INTO language (language_code, language_name) VALUES
('EN', 'English'),
('JA', 'Japanese'),
('ZH', 'Chinese');

INSERT INTO level (language_id, level_code, level_name, display_order)
SELECT id, 'A1', 'English A1', 1 FROM language WHERE language_code = 'EN'
UNION ALL SELECT id, 'A2', 'English A2', 2 FROM language WHERE language_code = 'EN'
UNION ALL SELECT id, 'B1', 'English B1', 3 FROM language WHERE language_code = 'EN'
UNION ALL SELECT id, 'B2', 'English B2', 4 FROM language WHERE language_code = 'EN'
UNION ALL SELECT id, 'C1', 'English C1', 5 FROM language WHERE language_code = 'EN'
UNION ALL SELECT id, 'C2', 'English C2', 6 FROM language WHERE language_code = 'EN'
UNION ALL SELECT id, 'N5', 'Japanese N5', 1 FROM language WHERE language_code = 'JA'
UNION ALL SELECT id, 'N4', 'Japanese N4', 2 FROM language WHERE language_code = 'JA'
UNION ALL SELECT id, 'N3', 'Japanese N3', 3 FROM language WHERE language_code = 'JA'
UNION ALL SELECT id, 'N2', 'Japanese N2', 4 FROM language WHERE language_code = 'JA'
UNION ALL SELECT id, 'N1', 'Japanese N1', 5 FROM language WHERE language_code = 'JA'
UNION ALL SELECT id, 'HSK1', 'Chinese HSK1', 1 FROM language WHERE language_code = 'ZH'
UNION ALL SELECT id, 'HSK2', 'Chinese HSK2', 2 FROM language WHERE language_code = 'ZH'
UNION ALL SELECT id, 'HSK3', 'Chinese HSK3', 3 FROM language WHERE language_code = 'ZH'
UNION ALL SELECT id, 'HSK4', 'Chinese HSK4', 4 FROM language WHERE language_code = 'ZH'
UNION ALL SELECT id, 'HSK5', 'Chinese HSK5', 5 FROM language WHERE language_code = 'ZH'
UNION ALL SELECT id, 'HSK6', 'Chinese HSK6', 6 FROM language WHERE language_code = 'ZH';

/*
  Transaction rules to enforce in Spring services:
  1. Lock courseclass before checking capacity and creating enrollment.
  2. Store payment and update enrollment statuses in one transaction.
  3. Validate room and teacher schedule conflicts before opening a class.
  4. Resolve language through course -> level -> language.
  5. Do not cascade-delete business history; change status instead.
*/
