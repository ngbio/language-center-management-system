package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.exception.ForbiddenException;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentAccessPolicy {
  public void requireOwner(Enrollment enrollment, Student student) {
    if (!enrollment.getStudentId().getId().equals(student.getId())) {
      throw new ForbiddenException("Bạn không được hủy đăng ký của học viên khác");
    }
  }

  public void requireClassRosterAccess(Courseclass courseClass, User user) {
    String role = user.getRoleId().getRoleCode();

    boolean canView = Set.of("ADMIN", "CONSULTANT").contains(role);
    if ("TEACHER".equals(role)
        && courseClass.getTeacherId() != null
        && courseClass.getTeacherId().getUserId().getId().equals(user.getId())) {
      canView = true;
    }
    if (!canView) {
      throw new ForbiddenException("Bạn không được xem danh sách học viên của lớp này");
    }
  }
}
