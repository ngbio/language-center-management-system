package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.request.AttendanceBulkRequest;
import com.ntt.language_center_management.dto.request.AttendanceUpdateRequest;
import com.ntt.language_center_management.dto.response.AttendanceResponse;
import com.ntt.language_center_management.dto.response.AttendanceSheetResponse;
import com.ntt.language_center_management.dto.response.ClassAttendanceSummaryResponse;
import java.security.Principal;
import java.util.List;

public interface AttendanceService {
  AttendanceSheetResponse getSheet(Integer lessonId, Principal principal);

  AttendanceSheetResponse saveBulk(
      Integer lessonId, AttendanceBulkRequest request, Principal principal);

  AttendanceResponse update(
      Integer attendanceId, AttendanceUpdateRequest request, Principal principal);

  List<AttendanceResponse> getMine(Principal principal);

  ClassAttendanceSummaryResponse getClassSummary(Integer classId, Principal principal);
}
