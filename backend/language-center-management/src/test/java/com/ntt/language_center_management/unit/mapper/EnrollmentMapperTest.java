package com.ntt.language_center_management.unit.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.ntt.language_center_management.entity.Course;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import com.ntt.language_center_management.mapper.EnrollmentMapper;
import java.math.BigDecimal;
import java.util.Date;
import org.junit.jupiter.api.Test;

class EnrollmentMapperTest {
  private final EnrollmentMapper mapper = new EnrollmentMapper();

  @Test
  void shouldMapDeadlineStatusesAndNestedEnrollmentData() {
    User user = new User(2);
    user.setFullName("Student Name");
    user.setEmail("student@example.com");
    Student student = new Student(3);
    student.setStudentCode("HV000003");
    student.setUserId(user);
    Course course = new Course(4);
    course.setCourseCode("EN-A1");
    course.setCourseName("English A1");
    Courseclass courseClass = new Courseclass(5);
    courseClass.setCourseId(course);
    courseClass.setClassCode("EN-A1-01");
    courseClass.setClassName("Morning class");
    Date createdAt = new Date(1_000);
    Date deadline = new Date(2_000);
    Enrollment enrollment = new Enrollment(6);
    enrollment.setStudentId(student);
    enrollment.setCourseClassId(courseClass);
    enrollment.setEnrollmentDate(createdAt);
    enrollment.setPaymentDeadline(deadline);
    enrollment.setAmountDue(new BigDecimal("3200000"));
    enrollment.setEnrollmentStatus(EnrollmentStatus.CONFIRMED);
    enrollment.setPaymentStatus(EnrollmentPaymentStatus.PENDING);

    var response = mapper.toResponse(enrollment);
    var summary = mapper.toSummaryResponse(enrollment);

    assertThat(response.paymentDeadline()).isSameAs(deadline);
    assertThat(response.enrollmentStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(response.paymentStatus()).isEqualTo(EnrollmentPaymentStatus.PENDING);
    assertThat(response.studentEmail()).isEqualTo("student@example.com");
    assertThat(response.courseCode()).isEqualTo("EN-A1");
    assertThat(summary.paymentDeadline()).isSameAs(deadline);
    assertThat(summary.enrollmentStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(summary.paymentStatus()).isEqualTo(EnrollmentPaymentStatus.PENDING);
    assertThat(summary.classCode()).isEqualTo("EN-A1-01");
  }
}
