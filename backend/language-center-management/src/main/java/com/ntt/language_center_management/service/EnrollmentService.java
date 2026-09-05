package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.request.CancelEnrollmentRequest;
import com.ntt.language_center_management.dto.request.CreateEnrollmentRequest;
import com.ntt.language_center_management.dto.request.TransferEnrollmentRequest;
import com.ntt.language_center_management.dto.response.EnrollmentResponse;
import com.ntt.language_center_management.dto.response.EnrollmentSummaryResponse;
import com.ntt.language_center_management.dto.response.CourseResponse;
import com.ntt.language_center_management.dto.response.CourseClassResponse;
import com.ntt.language_center_management.dto.response.ClassScheduleResponse;
import java.security.Principal;
import java.util.List;

public interface EnrollmentService {

  EnrollmentResponse enrollMe(CreateEnrollmentRequest request, Principal principal);

  EnrollmentResponse enrollByStaff(CreateEnrollmentRequest request);

  List<EnrollmentSummaryResponse> getMyEnrollments(Principal principal);

  List<CourseResponse> getMyCourses(Principal principal);

  List<CourseClassResponse> getMyClasses(Principal principal);

  List<ClassScheduleResponse> getMySchedules(Principal principal);

  List<EnrollmentSummaryResponse> getClassEnrollments(Integer classId, Principal principal);

  EnrollmentResponse requestCancel(
      Integer enrollmentId, CancelEnrollmentRequest request, Principal principal);

  EnrollmentResponse changeStatus(Integer enrollmentId, String status);

  EnrollmentResponse transfer(Integer enrollmentId, TransferEnrollmentRequest request);
}
