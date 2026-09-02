package com.ntt.language_center_management.mapper;

import com.ntt.language_center_management.dto.response.EnrollmentResponse;
import com.ntt.language_center_management.dto.response.EnrollmentSummaryResponse;
import com.ntt.language_center_management.entity.Enrollment;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentMapper {

  public EnrollmentResponse toResponse(Enrollment value) {
    var student = value.getStudentId();
    var user = student.getUserId();
    var courseClass = value.getCourseClassId();
    var course = courseClass.getCourseId();

    return new EnrollmentResponse(
        value.getId(), value.getEnrollmentDate(), value.getAmountDue(),
        value.getEnrollmentStatus(), value.getPaymentStatus(), value.getConfirmedAt(),
        value.getCancelledAt(), value.getCancellationReason(), student.getId(),
        student.getStudentCode(), user.getFullName(), user.getEmail(), courseClass.getId(),
        courseClass.getClassCode(), courseClass.getClassName(), course.getId(),
        course.getCourseCode(), course.getCourseName());
  }

  public EnrollmentSummaryResponse toSummaryResponse(Enrollment value) {
    var student = value.getStudentId();
    var courseClass = value.getCourseClassId();

    return new EnrollmentSummaryResponse(
        value.getId(), student.getId(), student.getStudentCode(),
        student.getUserId().getFullName(), courseClass.getId(), courseClass.getClassCode(),
        courseClass.getClassName(), value.getEnrollmentDate(), value.getAmountDue(),
        value.getEnrollmentStatus(), value.getPaymentStatus());
  }
}
