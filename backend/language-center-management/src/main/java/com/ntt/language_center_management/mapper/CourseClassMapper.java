package com.ntt.language_center_management.mapper;

import com.ntt.language_center_management.dto.request.CourseClassRequest;
import com.ntt.language_center_management.dto.response.CourseClassResponse;
import com.ntt.language_center_management.dto.response.ClassScheduleResponse;
import com.ntt.language_center_management.entity.Course;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Teacher;
import org.springframework.stereotype.Component;
import java.util.List;

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
    return toResponse(value, enrolledStudents, List.of());
  }

  public CourseClassResponse toResponse(
      Courseclass value,
      long enrolledStudents,
      List<ClassScheduleResponse> schedules) {
    var course = value.getCourseId();
    var level = course.getLevelId();
    var teacher = value.getTeacherId();

    return CourseClassResponse.builder()
        .id(value.getId())
        .classCode(value.getClassCode())
        .className(value.getClassName())
        .startDate(value.getStartDate())
        .endDate(value.getEndDate())
        .maxStudents(value.getMaxStudents())
        .enrolledStudents(enrolledStudents)
        .availableSeats(Math.max(value.getMaxStudents() - enrolledStudents, 0))
        .appliedTuitionFee(value.getAppliedTuitionFee())
        .status(value.getStatus())
        .courseId(course.getId())
        .courseCode(course.getCourseCode())
        .courseName(course.getCourseName())
        .levelId(level.getId())
        .levelCode(level.getLevelCode())
        .teacherId(teacher == null ? null : teacher.getId())
        .teacherCode(teacher == null ? null : teacher.getTeacherCode())
        .teacherName(teacher == null ? null : teacher.getUserId().getFullName())
        .createdAt(value.getCreatedAt())
        .updatedAt(value.getUpdatedAt())
        .schedules(schedules == null ? List.of() : List.copyOf(schedules))
        .build();
  }
}
