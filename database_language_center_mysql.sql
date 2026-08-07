/*
  Database: LanguageCenterDB
  Database engine: MySQL 8.0.16+
  Target: Entity Framework Core Database First
  Model: synchronized with the class diagram and four main use cases

  WARNING: DROP DATABASE removes the existing schema so this script can be
  rerun cleanly. Back up any data that must be preserved before execution.
*/

DROP DATABASE IF EXISTS LanguageCenterDB;

CREATE DATABASE LanguageCenterDB
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE LanguageCenterDB;

CREATE TABLE Role (
    Id          INT NOT NULL AUTO_INCREMENT,
    RoleCode    VARCHAR(20) NOT NULL,
    RoleName    VARCHAR(50) NOT NULL,
    PRIMARY KEY (Id),
    CONSTRAINT UQ_Role_Code UNIQUE (RoleCode),
    CONSTRAINT UQ_Role_Name UNIQUE (RoleName)
) ENGINE=InnoDB;

CREATE TABLE User (
    Id              INT NOT NULL AUTO_INCREMENT,
    RoleId        INT NOT NULL,
    Username     VARCHAR(100) NOT NULL,
    PasswordHash     VARCHAR(255) NOT NULL,
    FullName           VARCHAR(150) NOT NULL,
    Email           VARCHAR(150) NOT NULL,
    PhoneNumber     VARCHAR(20) NULL,
    Address          VARCHAR(255) NULL,
    Status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CreatedAt         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt     DATETIME NULL,
    PRIMARY KEY (Id),
    CONSTRAINT UQ_User_Username UNIQUE (Username),
    CONSTRAINT UQ_User_Email UNIQUE (Email),
    CONSTRAINT FK_User_Role FOREIGN KEY (RoleId) REFERENCES Role(Id),
    CONSTRAINT CK_User_Status CHECK (Status IN ('ACTIVE', 'LOCKED'))
) ENGINE=InnoDB;

CREATE TABLE Notification (
    Id              INT NOT NULL AUTO_INCREMENT,
    UserId      INT NOT NULL,
    Title          VARCHAR(200) NOT NULL,
    Content         TEXT NOT NULL,
    NotificationType    VARCHAR(30) NOT NULL DEFAULT 'SYSTEM',
    IsRead           BOOLEAN NOT NULL DEFAULT FALSE,
    CreatedAt         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ReadAt         DATETIME NULL,
    PRIMARY KEY (Id),
    CONSTRAINT FK_Notification_User FOREIGN KEY (UserId) REFERENCES User(Id),
    CONSTRAINT CK_Notification_Type CHECK (NotificationType IN ('SYSTEM', 'ENROLLMENT', 'PAYMENT', 'SCHEDULE'))
) ENGINE=InnoDB;

CREATE TABLE Student (
    Id              INT NOT NULL AUTO_INCREMENT,
    UserId      INT NOT NULL,
    StudentCode       VARCHAR(20) NOT NULL,
    DateOfBirth        DATE NULL,
    Gender        VARCHAR(10) NULL,
    Avatar          VARCHAR(500) NULL,
    PRIMARY KEY (Id),
    CONSTRAINT UQ_Student_User UNIQUE (UserId),
    CONSTRAINT UQ_Student_Code UNIQUE (StudentCode),
    CONSTRAINT FK_Student_User FOREIGN KEY (UserId) REFERENCES User(Id),
    CONSTRAINT CK_Student_Gender CHECK (Gender IS NULL OR Gender IN ('MALE', 'FEMALE', 'OTHER'))
) ENGINE=InnoDB;

CREATE TABLE Teacher (
    Id                  INT NOT NULL AUTO_INCREMENT,
    UserId          INT NOT NULL,
    TeacherCode         VARCHAR(20) NOT NULL,
    Specialization           VARCHAR(150) NULL,
    Degree             VARCHAR(200) NULL,
    ExperienceYears     INT NOT NULL DEFAULT 0,
    PRIMARY KEY (Id),
    CONSTRAINT UQ_Teacher_User UNIQUE (UserId),
    CONSTRAINT UQ_Teacher_Code UNIQUE (TeacherCode),
    CONSTRAINT FK_Teacher_User FOREIGN KEY (UserId) REFERENCES User(Id),
    CONSTRAINT CK_Teacher_ExperienceYears CHECK (ExperienceYears >= 0)
) ENGINE=InnoDB;

CREATE TABLE Language (
    Id              INT NOT NULL AUTO_INCREMENT,
    LanguageCode       VARCHAR(20) NOT NULL,
    LanguageName      VARCHAR(100) NOT NULL,
    Description            VARCHAR(500) NULL,
    Status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (Id),
    CONSTRAINT UQ_Language_Code UNIQUE (LanguageCode),
    CONSTRAINT UQ_Language_Name UNIQUE (LanguageName),
    CONSTRAINT CK_Language_Status CHECK (Status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB;

CREATE TABLE Level (
    Id              INT NOT NULL AUTO_INCREMENT,
    LanguageId       INT NOT NULL,
    LevelCode       VARCHAR(20) NOT NULL,
    LevelName      VARCHAR(100) NOT NULL,
    Description            VARCHAR(500) NULL,
    DisplayOrder           INT NOT NULL DEFAULT 1,
    Status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (Id),
    CONSTRAINT FK_Level_Language FOREIGN KEY (LanguageId) REFERENCES Language(Id),
    CONSTRAINT UQ_Level_Language_Code UNIQUE (LanguageId, LevelCode),
    CONSTRAINT UQ_Level_Language_Name UNIQUE (LanguageId, LevelName),
    CONSTRAINT CK_Level_DisplayOrder CHECK (DisplayOrder > 0),
    CONSTRAINT CK_Level_Status CHECK (Status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB;

CREATE TABLE Course (
    Id              INT NOT NULL AUTO_INCREMENT,
    LevelId       INT NOT NULL,
    CourseCode       VARCHAR(30) NOT NULL,
    CourseName      VARCHAR(200) NOT NULL,
    Description            VARCHAR(1000) NULL,
    TuitionFee          DECIMAL(18,2) NOT NULL,
    TotalSessions      INT NOT NULL,
    DurationHours    INT NULL,
    Status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CreatedAt         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt     DATETIME NULL,
    PRIMARY KEY (Id),
    CONSTRAINT UQ_Course_Code UNIQUE (CourseCode),
    CONSTRAINT FK_Course_Level FOREIGN KEY (LevelId) REFERENCES Level(Id),
    CONSTRAINT CK_Course_TuitionFee CHECK (TuitionFee >= 0),
    CONSTRAINT CK_Course_TotalSessions CHECK (TotalSessions > 0),
    CONSTRAINT CK_Course_DurationHours CHECK (DurationHours IS NULL OR DurationHours > 0),
    CONSTRAINT CK_Course_Status CHECK (Status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB;

CREATE TABLE Room (
    Id              INT NOT NULL AUTO_INCREMENT,
    RoomCode         VARCHAR(30) NOT NULL,
    RoomName        VARCHAR(100) NOT NULL,
    Capacity         INT NOT NULL,
    Location         VARCHAR(255) NULL,
    Status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (Id),
    CONSTRAINT UQ_Room_Code UNIQUE (RoomCode),
    CONSTRAINT CK_Room_Capacity CHECK (Capacity > 0),
    CONSTRAINT CK_Room_Status CHECK (Status IN ('ACTIVE', 'MAINTENANCE', 'INACTIVE'))
) ENGINE=InnoDB;

CREATE TABLE CourseClass (
    Id              INT NOT NULL AUTO_INCREMENT,
    CourseId       INT NOT NULL,
    TeacherId     INT NULL,
    ClassCode           VARCHAR(30) NOT NULL,
    ClassName          VARCHAR(200) NOT NULL,
    StartDate      DATE NOT NULL,
    EndDate     DATE NOT NULL,
    MaxStudents       INT NOT NULL,
    AppliedTuitionFee    DECIMAL(18,2) NOT NULL,
    Status       VARCHAR(30) NOT NULL DEFAULT 'UPCOMING',
    CreatedAt         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt     DATETIME NULL,
    PRIMARY KEY (Id),
    CONSTRAINT UQ_CourseClass_Code UNIQUE (ClassCode),
    CONSTRAINT FK_CourseClass_Course FOREIGN KEY (CourseId) REFERENCES Course(Id),
    CONSTRAINT FK_CourseClass_Teacher FOREIGN KEY (TeacherId) REFERENCES Teacher(Id),
    CONSTRAINT CK_CourseClass_DateRange CHECK (EndDate >= StartDate),
    CONSTRAINT CK_CourseClass_MaxStudents CHECK (MaxStudents > 0),
    CONSTRAINT CK_CourseClass_TuitionFee CHECK (AppliedTuitionFee >= 0),
    CONSTRAINT CK_CourseClass_Status CHECK (Status IN ('UPCOMING', 'OPEN_FOR_ENROLLMENT', 'FULL', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
) ENGINE=InnoDB;

CREATE TABLE ClassSchedule (
    Id              INT NOT NULL AUTO_INCREMENT,
    CourseClassId        INT NOT NULL,
    RoomId      INT NULL,
    DayOfWeek    TINYINT NOT NULL,
    StartTime       TIME NOT NULL,
    EndTime      TIME NOT NULL,
    DeliveryMode        VARCHAR(20) NOT NULL DEFAULT 'IN_PERSON',
    MeetingUrl     VARCHAR(500) NULL,
    PRIMARY KEY (Id),
    CONSTRAINT FK_ClassSchedule_CourseClass FOREIGN KEY (CourseClassId) REFERENCES CourseClass(Id),
    CONSTRAINT FK_ClassSchedule_Room FOREIGN KEY (RoomId) REFERENCES Room(Id),
    CONSTRAINT UQ_ClassSchedule_Class_Time UNIQUE (CourseClassId, DayOfWeek, StartTime),
    CONSTRAINT CK_ClassSchedule_DayOfWeek CHECK (DayOfWeek BETWEEN 2 AND 8),
    CONSTRAINT CK_ClassSchedule_TimeRange CHECK (EndTime > StartTime),
    CONSTRAINT CK_ClassSchedule_DeliveryMode CHECK (DeliveryMode IN ('IN_PERSON', 'ONLINE')),
    CONSTRAINT CK_ClassSchedule_Location CHECK (
        (DeliveryMode = 'IN_PERSON' AND RoomId IS NOT NULL)
        OR (DeliveryMode = 'ONLINE' AND MeetingUrl IS NOT NULL)
    )
) ENGINE=InnoDB;

CREATE TABLE Lesson (
    Id              INT NOT NULL AUTO_INCREMENT,
    ClassScheduleId       INT NOT NULL,
    Topic           VARCHAR(255) NULL,
    LessonDate         DATE NOT NULL,
    MeetingUrl     VARCHAR(500) NULL,
    Status       VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    PRIMARY KEY (Id),
    CONSTRAINT FK_Lesson_ClassSchedule FOREIGN KEY (ClassScheduleId) REFERENCES ClassSchedule(Id),
    CONSTRAINT UQ_Lesson_Schedule_Date UNIQUE (ClassScheduleId, LessonDate),
    CONSTRAINT CK_Lesson_Status CHECK (Status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
) ENGINE=InnoDB;

CREATE TABLE Enrollment (
    Id                      INT NOT NULL AUTO_INCREMENT,
    StudentId               INT NOT NULL,
    CourseClassId                INT NOT NULL,
    EnrollmentDate              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    AmountDue           DECIMAL(18,2) NOT NULL,
    EnrollmentStatus         VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    PaymentStatus      VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    ConfirmedAt             DATETIME NULL,
    CancelledAt                 DATETIME NULL,
    CancellationReason                 VARCHAR(500) NULL,
    PRIMARY KEY (Id),
    CONSTRAINT FK_Enrollment_Student FOREIGN KEY (StudentId) REFERENCES Student(Id),
    CONSTRAINT FK_Enrollment_CourseClass FOREIGN KEY (CourseClassId) REFERENCES CourseClass(Id),
    CONSTRAINT UQ_Enrollment_Student_Class UNIQUE (StudentId, CourseClassId),
    CONSTRAINT UQ_Enrollment_Id_Class UNIQUE (Id, CourseClassId),
    CONSTRAINT CK_Enrollment_AmountDue CHECK (AmountDue >= 0),
    CONSTRAINT CK_Enrollment_Status CHECK (EnrollmentStatus IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT CK_Enrollment_PaymentStatus CHECK (PaymentStatus IN ('UNPAID', 'PAID', 'PAYMENT_FAILED', 'REFUNDED'))
) ENGINE=InnoDB;

CREATE TABLE Payment (
    Id                      INT NOT NULL AUTO_INCREMENT,
    EnrollmentId          INT NOT NULL,
    TransactionCode              VARCHAR(100) NOT NULL,
    Method              VARCHAR(30) NOT NULL,
    Amount                  DECIMAL(18,2) NOT NULL,
    Status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CreatedAt             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CompletedAt         DATETIME NULL,
    ReferenceCode             VARCHAR(150) NULL,
    ErrorMessage              VARCHAR(500) NULL,
    SuccessfulEnrollmentId       INT GENERATED ALWAYS AS
                            (CASE WHEN Status = 'SUCCESS' THEN EnrollmentId ELSE NULL END) STORED,
    PRIMARY KEY (Id),
    CONSTRAINT UQ_Payment_TransactionCode UNIQUE (TransactionCode),
    CONSTRAINT UQ_Payment_OneSuccessPerEnrollment UNIQUE (SuccessfulEnrollmentId),
    CONSTRAINT FK_Payment_Enrollment FOREIGN KEY (EnrollmentId) REFERENCES Enrollment(Id),
    CONSTRAINT CK_Payment_Amount CHECK (Amount > 0),
    CONSTRAINT CK_Payment_Method CHECK (Method IN ('CASH', 'BANK_TRANSFER', 'E_WALLET', 'CARD')),
    CONSTRAINT CK_Payment_Status CHECK (Status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED'))
) ENGINE=InnoDB;

CREATE TABLE Attendance (
    Id                  INT NOT NULL AUTO_INCREMENT,
    LessonId           INT NOT NULL,
    EnrollmentId      INT NOT NULL,
    Status           VARCHAR(20) NOT NULL,
    Note              VARCHAR(500) NULL,
    AttendanceTime    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (Id),
    CONSTRAINT FK_Attendance_Lesson FOREIGN KEY (LessonId) REFERENCES Lesson(Id),
    CONSTRAINT FK_Attendance_Enrollment FOREIGN KEY (EnrollmentId) REFERENCES Enrollment(Id),
    CONSTRAINT UQ_Attendance_Lesson_Enrollment UNIQUE (LessonId, EnrollmentId),
    CONSTRAINT CK_Attendance_Status CHECK (Status IN ('PRESENT', 'ABSENT', 'LATE'))
) ENGINE=InnoDB;

CREATE INDEX IX_Notification_User_IsRead ON Notification(UserId, IsRead, CreatedAt);
CREATE INDEX IX_Level_Language_DisplayOrder ON Level(LanguageId, DisplayOrder);
CREATE INDEX IX_Course_Level_Status ON Course(LevelId, Status);
CREATE INDEX IX_CourseClass_Course_Status ON CourseClass(CourseId, Status);
CREATE INDEX IX_CourseClass_Teacher ON CourseClass(TeacherId);
CREATE INDEX IX_ClassSchedule_Room ON ClassSchedule(RoomId);
CREATE INDEX IX_Lesson_Date_Status ON Lesson(LessonDate, Status);
CREATE INDEX IX_Enrollment_CourseClass_Status ON Enrollment(CourseClassId, EnrollmentStatus);
CREATE INDEX IX_Enrollment_Student_Date ON Enrollment(StudentId, EnrollmentDate DESC);
CREATE INDEX IX_Attendance_Lesson ON Attendance(LessonId);

INSERT INTO Role (RoleCode, RoleName) VALUES
('STUDENT', 'Student'),
('TEACHER', 'Teacher'),
('ADMIN', 'Administrator');

INSERT INTO Language (LanguageCode, LanguageName) VALUES
('EN', 'English'),
('JA', 'Japanese'),
('ZH', 'Chinese');

INSERT INTO Level (LanguageId, LevelCode, LevelName, DisplayOrder)
SELECT Id, 'A1', 'English A1', 1 FROM Language WHERE LanguageCode = 'EN'
UNION ALL SELECT Id, 'A2', 'English A2', 2 FROM Language WHERE LanguageCode = 'EN'
UNION ALL SELECT Id, 'B1', 'English B1', 3 FROM Language WHERE LanguageCode = 'EN'
UNION ALL SELECT Id, 'B2', 'English B2', 4 FROM Language WHERE LanguageCode = 'EN'
UNION ALL SELECT Id, 'C1', 'English C1', 5 FROM Language WHERE LanguageCode = 'EN'
UNION ALL SELECT Id, 'C2', 'English C2', 6 FROM Language WHERE LanguageCode = 'EN'
UNION ALL SELECT Id, 'N5', 'Japanese N5', 1 FROM Language WHERE LanguageCode = 'JA'
UNION ALL SELECT Id, 'N4', 'Japanese N4', 2 FROM Language WHERE LanguageCode = 'JA'
UNION ALL SELECT Id, 'N3', 'Japanese N3', 3 FROM Language WHERE LanguageCode = 'JA'
UNION ALL SELECT Id, 'N2', 'Japanese N2', 4 FROM Language WHERE LanguageCode = 'JA'
UNION ALL SELECT Id, 'N1', 'Japanese N1', 5 FROM Language WHERE LanguageCode = 'JA'
UNION ALL SELECT Id, 'HSK1', 'Chinese HSK1', 1 FROM Language WHERE LanguageCode = 'ZH'
UNION ALL SELECT Id, 'HSK2', 'Chinese HSK2', 2 FROM Language WHERE LanguageCode = 'ZH'
UNION ALL SELECT Id, 'HSK3', 'Chinese HSK3', 3 FROM Language WHERE LanguageCode = 'ZH'
UNION ALL SELECT Id, 'HSK4', 'Chinese HSK4', 4 FROM Language WHERE LanguageCode = 'ZH'
UNION ALL SELECT Id, 'HSK5', 'Chinese HSK5', 5 FROM Language WHERE LanguageCode = 'ZH'
UNION ALL SELECT Id, 'HSK6', 'Chinese HSK6', 6 FROM Language WHERE LanguageCode = 'ZH';

/*
  Service transaction rules:
  1. Enrollment: lock CourseClass with SELECT ... FOR UPDATE, then check that
     enrollment is open, the student is not already enrolled, and capacity remains.
  2. Successful payment: insert Payment and update both Enrollment status fields
     in the same transaction.
  3. Attendance: Lesson -> ClassSchedule -> CourseClass -> TeacherId must match
     the current teacher. Enrollment must be CONFIRMED and belong to the same
     CourseClass reached through the Lesson's ClassSchedule.
  4. Resolve a course language through Course -> Level -> Language. Course does not
     store LanguageId directly, preventing inconsistent language-level pairs.
  5. Do not cascade-delete business records; mark them inactive with Status.
*/