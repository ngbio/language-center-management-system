package com.ntt.language_center_management.mapper;

import com.ntt.language_center_management.dto.request.CourseClassRequest;
import com.ntt.language_center_management.dto.response.CourseClassResponse;
import com.ntt.language_center_management.entity.Course;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Teacher;
import org.springframework.stereotype.Component;

@Component
public class CourseClassMapper {

  public void updateEntity(
      Courseclass value, CourseClassRequest request, Course course, Teacher teacher) {
    value.setClassCode(request.getClassCode().trim().toUpperCase());
    value.setClassName(request.getClassName().trim());
    value.setStartDate(request.getStartDate());
    value.setEndDate(request.getEndDate());
    value.setMaxStudents(request.getMaxStudents());
    value.setAppliedTuitionFee(request.getAppliedTuitionFee());
    value.setCourseId(course);
    value.setTeacherId(teacher);
  }

  public CourseClassResponse toResponse(Courseclass value, long enrolledStudents) {
    var course = value.getCourseId();
    var level = course.getLevelId();
    var teacher = value.getTeacherId();

    return new CourseClassResponse(
        value.getId(),
        value.getClassCode(),
        value.getClassName(),
        value.getStartDate(),
        value.getEndDate(),
        value.getMaxStudents(),
        enrolledStudents,
        Math.max(value.getMaxStudents() - enrolledStudents, 0),
        value.getAppliedTuitionFee(),
        value.getStatus(),
        course.getId(),
        course.getCourseCode(),
        course.getCourseName(),
        level.getId(),
        level.getLevelCode(),
        teacher == null ? null : teacher.getId(),
        teacher == null ? null : teacher.getTeacherCode(),
        teacher == null ? null : teacher.getUserId().getFullName(),
        value.getCreatedAt(),
        value.getUpdatedAt());
  }
}
