package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Teacher;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.repository.TeacherRepository;
import java.security.Principal;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class AttendanceAccessPolicy {
  private final TeacherRepository teacherRepository;
  public AttendanceAccessPolicy(TeacherRepository teacherRepository) {
    this.teacherRepository = teacherRepository;
  }

  public Teacher requireAssignedTeacher(Courseclass courseClass, Principal principal) {
    Teacher teacher =
        teacherRepository
            .findByUserId_EmailIgnoreCase(principalName(principal))
            .orElseThrow(() -> new ForbiddenException("Chỉ giảng viên mới được quản lý điểm danh"));
    if (courseClass.getTeacherId() == null || !courseClass.getTeacherId().getId().equals(teacher.getId())) {
      throw new ForbiddenException("Bạn không phải giảng viên phụ trách lớp học này");
    }
    return teacher;
  }

  private String principalName(Principal principal) {
    if (principal == null || !StringUtils.hasText(principal.getName())) {
      throw new ForbiddenException("Không xác định được người dùng hiện tại");
    }
    return principal.getName();
  }
}
