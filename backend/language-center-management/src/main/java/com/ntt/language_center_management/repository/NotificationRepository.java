package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Notification;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

  Page<Notification> findByUserId_IdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

  Optional<Notification> findByIdAndUserId_Id(Integer id, Integer userId);

  long countByUserId_IdAndIsReadFalse(Integer userId);

  @Modifying
  @Query("""
      update Notification notification
      set notification.isRead = true, notification.readAt = :readAt
      where notification.userId.id = :userId and notification.isRead = false
      """)
  int markAllRead(@Param("userId") Integer userId, @Param("readAt") Date readAt);

  @Modifying
  @Query(value = """
      INSERT IGNORE INTO notification
          (user_id, title, content, notification_type, is_read, created_at, read_at, dedup_key)
      SELECT student_user.id,
             'Buổi học đang bắt đầu',
             CONCAT('Lớp ', course_class.class_name, ' bắt đầu lúc ',
                    DATE_FORMAT(class_schedule.start_time, '%H:%i'), '.'),
             'SCHEDULE', FALSE, :createdAt, NULL,
             CONCAT('LESSON_START:', lesson.id, ':', student_user.id)
      FROM lesson lesson
      JOIN classschedule class_schedule ON class_schedule.id = lesson.class_schedule_id
      JOIN courseclass course_class ON course_class.id = class_schedule.course_class_id
      JOIN enrollment enrollment ON enrollment.course_class_id = course_class.id
      JOIN student student ON student.id = enrollment.student_id
      JOIN `user` student_user ON student_user.id = student.user_id
      LEFT JOIN notification existing_notification
        ON existing_notification.dedup_key = CONCAT('LESSON_START:', lesson.id, ':', student_user.id)
      WHERE lesson.status IN ('SCHEDULED', 'IN_PROGRESS')
        AND course_class.status <> 'CANCELLED'
        AND enrollment.enrollment_status = 'CONFIRMED'
        AND enrollment.payment_status = 'PAID'
        AND student_user.status = 'ACTIVE'
        AND TIMESTAMP(lesson.lesson_date, class_schedule.start_time) > :windowStart
        AND TIMESTAMP(lesson.lesson_date, class_schedule.start_time) <= :now
        AND existing_notification.id IS NULL
      ORDER BY lesson.id, student_user.id
      LIMIT 1000
      """, nativeQuery = true)
  int insertDueLessonReminderBatch(
      @Param("windowStart") LocalDateTime windowStart,
      @Param("now") LocalDateTime now,
      @Param("createdAt") LocalDateTime createdAt);
}
