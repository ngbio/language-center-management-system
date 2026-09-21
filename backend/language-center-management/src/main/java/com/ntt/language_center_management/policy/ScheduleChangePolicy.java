package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.dto.request.ClassScheduleRequest;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.repository.LessonRepository;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class ScheduleChangePolicy {
  private final LessonRepository lessonRepository;
  public ScheduleChangePolicy(LessonRepository lessonRepository) {
    this.lessonRepository = lessonRepository;
  }

  public void ensureClassAllowsScheduleChanges(Courseclass courseClass) {
    if (courseClass.getStatus() == ClassStatus.COMPLETED
        || courseClass.getStatus() == ClassStatus.CANCELLED) {
      throw new IllegalArgumentException("Không thể thay đổi lịch của lớp đã kết thúc hoặc đã hủy");
    }
  }

  public void ensureLessonsNotGenerated(Integer classId) {
    if (lessonRepository.countByClassScheduleId_CourseClassId_Id(classId) > 0) {
      throw new IllegalArgumentException("Không thể thay đổi lịch sau khi đã sinh buổi học");
    }
  }
  public void validateTimes(ClassScheduleRequest request) {
    if (request.dayOfWeek() < 1 || request.dayOfWeek() > 7) {
      throw new IllegalArgumentException("Ngày trong tuần phải từ 1 đến 7");
    }
    if (!request.startTime().isBefore(request.endTime())) {
      throw new IllegalArgumentException("Giờ bắt đầu phải trước giờ kết thúc");
    }

  }

  public void validateDeletion(Integer id) {
    if (lessonRepository.existsByClassScheduleId_Id(id)) {
      throw new IllegalArgumentException("Không thể xóa lịch đã sinh buổi học");
    }
  }

}
