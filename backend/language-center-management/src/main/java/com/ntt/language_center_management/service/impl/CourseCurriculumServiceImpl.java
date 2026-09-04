package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.response.CourseContentResponse;
import com.ntt.language_center_management.dto.response.CourseSectionResponse;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.mapper.CourseCurriculumMapper;
import com.ntt.language_center_management.repository.CourseContentRepository;
import com.ntt.language_center_management.repository.CourseRepository;
import com.ntt.language_center_management.repository.CourseSectionRepository;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.service.CourseCurriculumService;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourseCurriculumServiceImpl implements CourseCurriculumService {
  private final CourseRepository courseRepository;
  private final CourseSectionRepository courseSectionRepository;
  private final CourseContentRepository courseContentRepository;
  private final CourseCurriculumMapper courseCurriculumMapper;
  private final StudentRepository studentRepository;
  private final EnrollmentRepository enrollmentRepository;

  public CourseCurriculumServiceImpl(
      CourseRepository courseRepository,
      CourseSectionRepository courseSectionRepository,
      CourseContentRepository courseContentRepository,
      CourseCurriculumMapper courseCurriculumMapper,
      StudentRepository studentRepository,
      EnrollmentRepository enrollmentRepository) {
    this.courseRepository = courseRepository;
    this.courseSectionRepository = courseSectionRepository;
    this.courseContentRepository = courseContentRepository;
    this.courseCurriculumMapper = courseCurriculumMapper;
    this.studentRepository = studentRepository;
    this.enrollmentRepository = enrollmentRepository;
  }

  @Override
  public List<CourseSectionResponse> getPublishedSections(Integer courseId) {
    boolean visible =
        courseRepository
            .findById(courseId)
            .filter(
                course ->
                    "ACTIVE".equals(course.getStatus())
                        && "PUBLISHED".equals(course.getPublicationStatus()))
            .isPresent();
    if (!visible) {
      throw new ResourceNotFoundException("Không tìm thấy khóa học");
    }
    return courseSectionRepository.findByCourseId_IdOrderByDisplayOrderAsc(courseId).stream()
        .map(courseCurriculumMapper::toSectionResponse)
        .toList();
  }

  @Override
  public List<CourseContentResponse> getPublishedContents(
      Integer sectionId, Principal principal) {
    var section =
        courseSectionRepository
        .findByIdAndCourseId_StatusAndCourseId_PublicationStatus(
            sectionId, "ACTIVE", "PUBLISHED")
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phần nội dung"));

    var course = section.getCourseId();
    boolean freeCourse =
        course.getTuitionFee() != null
            && course.getTuitionFee().compareTo(BigDecimal.ZERO) == 0;
    boolean purchased = hasPaidEnrollment(principal, course.getId());

    var contents =
        freeCourse || purchased
            ? courseContentRepository
                .findBySectionId_IdAndPublicationStatusOrderByDisplayOrderAsc(
                    sectionId, "PUBLISHED")
            : courseContentRepository
                .findBySectionId_IdAndPublicationStatusAndIsPreviewTrueOrderByDisplayOrderAsc(
                    sectionId, "PUBLISHED");

    return contents.stream()
        .map(courseCurriculumMapper::toContentResponse)
        .toList();
  }

  private boolean hasPaidEnrollment(Principal principal, Integer courseId) {
    if (principal == null || principal.getName() == null) {
      return false;
    }

    return studentRepository
        .findByUserId_EmailIgnoreCase(principal.getName())
        .map(
            student ->
                enrollmentRepository
                    .existsPaidConfirmedAccess(student.getId(), courseId))
        .orElse(false);
  }
}
