-- Apply once to an existing database BEFORE deploying the re-registration fix.
-- Fresh installs already include this constraint in database_language_center_mysql.sql.
-- Keep cancelled rows and their payment/refund/attendance history intact.
-- NULL permits multiple cancelled attempts; slot 1 permits only one active attempt.
ALTER TABLE enrollment
    ADD COLUMN active_slot INT GENERATED ALWAYS AS
        (CASE WHEN enrollment_status IN ('PENDING', 'CONFIRMED') THEN 1 ELSE NULL END) STORED,
    DROP INDEX uq_enrollment_student_class,
    ADD CONSTRAINT uq_enrollment_student_class_active
        UNIQUE (student_id, course_class_id, active_slot);
