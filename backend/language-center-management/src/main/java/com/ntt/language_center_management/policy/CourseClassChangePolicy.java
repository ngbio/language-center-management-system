package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.policy.EnrollmentPolicy;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.dto.request.CourseClassRequest;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class CourseClassChangePolicy {
  private final CourseClassRepository courseClassRepository;
  private final EnrollmentRepository enrollmentRepository;
  public CourseClassChangePolicy(CourseClassRepository courseClassRepository, EnrollmentRepository enrollmentRepository) {
    this.courseClassRepository = courseClassRepository;
    this.enrollmentRepository = enrollmentRepository;
  }

  public void validateCode(String code, Integer id) {
    String normalized = code.trim().toUpperCase();
    boolean exists =
        id == null
            ? courseClassRepository.existsByClassCodeIgnoreCase(normalized)
            : courseClassRepository.existsByClassCodeIgnoreCaseAndIdNot(normalized, id);
    if (exists) {
      throw new DuplicateResourceException("Mã lớp đã tồn tại");
    }
  }

  public void validateDates(CourseClassRequest request) {
    if (request.getMaxStudents() < 1) {
      throw new IllegalArgumentException("Sĩ số tối đa phải lớn hơn 0");
    }
    if (request.getAppliedTuitionFee() == null
        || request.getAppliedTuitionFee().signum() < 0) {
      throw new IllegalArgumentException("Học phí áp dụng không được âm");
    }
    if (!request.getStartDate().before(request.getEndDate())) {
      throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
    }
  }
  public void validateCapacity(int maxStudents, long enrolled) {
    if (maxStudents < enrolled) {
      throw new IllegalArgumentException("Sĩ số tối đa không được nhỏ hơn số đăng ký hiện tại");
    }
  }

  public void validateDeletion(Courseclass value) {
    if (value.getStatus() != ClassStatus.DRAFT) {
      throw new IllegalArgumentException("Chỉ có thể xóa lớp ở trạng thái DRAFT");
    }
    if (enrollmentRepository.existsByCourseClassId_Id(value.getId())) {
      throw new IllegalArgumentException("Không thể xóa lớp đã có lịch sử đăng ký");
    }
  }

}
