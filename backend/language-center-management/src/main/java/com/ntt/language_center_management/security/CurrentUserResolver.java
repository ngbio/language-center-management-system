package com.ntt.language_center_management.security;

import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.entity.Teacher;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.repository.TeacherRepository;
import com.ntt.language_center_management.repository.UserRepository;
import java.security.Principal;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CurrentUserResolver {
  private final UserRepository userRepository;
  private final StudentRepository studentRepository;
  private final TeacherRepository teacherRepository;

  public CurrentUserResolver(
      UserRepository userRepository,
      StudentRepository studentRepository,
      TeacherRepository teacherRepository) {
    this.userRepository = userRepository;
    this.studentRepository = studentRepository;
    this.teacherRepository = teacherRepository;
  }

  public String requireEmail(Principal principal) {
    if (principal == null || !StringUtils.hasText(principal.getName())) {
      throw new UnauthorizedException("Không thể xác định người dùng hiện tại");
    }
    return principal.getName();
  }

  public User requireUser(Principal principal) {
    return userRepository.findByEmailIgnoreCase(requireEmail(principal))
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản hiện tại"));
  }

  public Student requireStudent(Principal principal) {
    return studentRepository.findByUserId_EmailIgnoreCase(requireEmail(principal))
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ học viên"));
  }

  public Teacher requireTeacher(Principal principal) {
    return teacherRepository.findByUserId_EmailIgnoreCase(requireEmail(principal))
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ giảng viên"));
  }
}
