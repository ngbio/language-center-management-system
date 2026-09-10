package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.enums.CatalogStatus;
import com.ntt.language_center_management.enums.PublicationStatus;

import com.ntt.language_center_management.dto.response.CourseContentResponse;
import com.ntt.language_center_management.dto.response.CourseSectionResponse;
import com.ntt.language_center_management.dto.response.AdminCourseContentResponse;
import com.ntt.language_center_management.dto.request.CourseContentRequest;
import com.ntt.language_center_management.dto.request.CourseSectionRequest;
import com.ntt.language_center_management.entity.CourseContent;
import com.ntt.language_center_management.entity.CourseSection;
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
import java.util.HashSet;
import java.util.Set;
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
                    course.getStatus() == CatalogStatus.ACTIVE
                        && course.getPublicationStatus() == PublicationStatus.PUBLISHED)
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
            sectionId, CatalogStatus.ACTIVE, PublicationStatus.PUBLISHED)
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
                    sectionId, PublicationStatus.PUBLISHED)
            : courseContentRepository
                .findBySectionId_IdAndPublicationStatusAndIsPreviewTrueOrderByDisplayOrderAsc(
                    sectionId, PublicationStatus.PUBLISHED);

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

  @Override
  public List<CourseSectionResponse> getAdminSections(Integer courseId) {
    requireCourse(courseId);
    return courseSectionRepository.findByCourseId_IdOrderByDisplayOrderAsc(courseId).stream()
        .map(courseCurriculumMapper::toSectionResponse).toList();
  }

  @Override
  @Transactional
  public CourseSectionResponse createSection(Integer courseId, CourseSectionRequest request) {
    var sections = courseSectionRepository.findByCourseId_IdOrderByDisplayOrderAsc(courseId);
    CourseSection section = new CourseSection();
    section.setCourseId(requireCourse(courseId));
    applySection(section, request);
    section.setDisplayOrder(sections.stream().mapToInt(CourseSection::getDisplayOrder).max().orElse(0) + 1);
    return courseCurriculumMapper.toSectionResponse(courseSectionRepository.save(section));
  }

  @Override
  @Transactional
  public CourseSectionResponse updateSection(Integer id, CourseSectionRequest request) {
    CourseSection section = requireSection(id);
    applySection(section, request);
    return courseCurriculumMapper.toSectionResponse(courseSectionRepository.save(section));
  }

  @Override
  @Transactional
  public void deleteSection(Integer id) {
    CourseSection section = requireSection(id);
    Integer courseId = section.getCourseId().getId();
    courseSectionRepository.delete(section);
    courseSectionRepository.flush();
    normalizeSections(courseSectionRepository.findByCourseId_IdOrderByDisplayOrderAsc(courseId));
  }

  @Override
  @Transactional
  public List<CourseSectionResponse> reorderSections(List<Integer> ids) {
    List<CourseSection> sections = courseSectionRepository.findAllById(ids);
    validateReorder(ids, sections.stream().map(CourseSection::getId).toList(), "phần nội dung");
    Set<Integer> courseIds = sections.stream().map(value -> value.getCourseId().getId()).collect(java.util.stream.Collectors.toSet());
    if (courseIds.size() != 1 || courseSectionRepository.findByCourseId_IdOrderByDisplayOrderAsc(courseIds.iterator().next()).size() != ids.size())
      throw new IllegalArgumentException("Danh sách sắp xếp phải chứa toàn bộ phần của cùng một khóa học");
    moveSectionsToTemporaryOrder(sections);
    for (int index = 0; index < ids.size(); index++) findSection(sections, ids.get(index)).setDisplayOrder(index + 1);
    return courseSectionRepository.saveAllAndFlush(sections).stream()
        .sorted(java.util.Comparator.comparingInt(CourseSection::getDisplayOrder))
        .map(courseCurriculumMapper::toSectionResponse).toList();
  }

  @Override
  public List<AdminCourseContentResponse> getAdminContents(Integer sectionId) {
    requireSection(sectionId);
    return courseContentRepository.findBySectionId_IdOrderByDisplayOrderAsc(sectionId).stream()
        .map(this::toAdminContent).toList();
  }

  @Override
  @Transactional
  public AdminCourseContentResponse createContent(Integer sectionId, CourseContentRequest request) {
    var contents = courseContentRepository.findBySectionId_IdOrderByDisplayOrderAsc(sectionId);
    CourseContent content = new CourseContent();
    content.setSectionId(requireSection(sectionId));
    applyContent(content, request);
    content.setDisplayOrder(contents.stream().mapToInt(CourseContent::getDisplayOrder).max().orElse(0) + 1);
    content.setPublicationStatus(PublicationStatus.DRAFT);
    return toAdminContent(courseContentRepository.save(content));
  }

  @Override
  @Transactional
  public AdminCourseContentResponse updateContent(Integer id, CourseContentRequest request) {
    CourseContent content = requireContent(id);
    applyContent(content, request);
    return toAdminContent(courseContentRepository.save(content));
  }

  @Override
  @Transactional
  public void deleteContent(Integer id) {
    CourseContent content = requireContent(id);
    Integer sectionId = content.getSectionId().getId();
    courseContentRepository.delete(content);
    courseContentRepository.flush();
    normalizeContents(courseContentRepository.findBySectionId_IdOrderByDisplayOrderAsc(sectionId));
  }

  @Override
  @Transactional
  public AdminCourseContentResponse changePublicationStatus(Integer id, PublicationStatus status) {
    CourseContent content = requireContent(id);
    content.setPublicationStatus(status);
    return toAdminContent(courseContentRepository.save(content));
  }

  @Override
  @Transactional
  public List<AdminCourseContentResponse> reorderContents(List<Integer> ids) {
    List<CourseContent> contents = courseContentRepository.findAllById(ids);
    validateReorder(ids, contents.stream().map(CourseContent::getId).toList(), "nội dung");
    Set<Integer> sectionIds = contents.stream().map(value -> value.getSectionId().getId()).collect(java.util.stream.Collectors.toSet());
    if (sectionIds.size() != 1 || courseContentRepository.findBySectionId_IdOrderByDisplayOrderAsc(sectionIds.iterator().next()).size() != ids.size())
      throw new IllegalArgumentException("Danh sách sắp xếp phải chứa toàn bộ nội dung của cùng một phần");
    moveContentsToTemporaryOrder(contents);
    for (int index = 0; index < ids.size(); index++) findContent(contents, ids.get(index)).setDisplayOrder(index + 1);
    return courseContentRepository.saveAllAndFlush(contents).stream()
        .sorted(java.util.Comparator.comparingInt(CourseContent::getDisplayOrder))
        .map(this::toAdminContent).toList();
  }

  private com.ntt.language_center_management.entity.Course requireCourse(Integer id) {
    return courseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));
  }
  private CourseSection requireSection(Integer id) {
    return courseSectionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phần nội dung"));
  }
  private CourseContent requireContent(Integer id) {
    return courseContentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nội dung"));
  }
  private void applySection(CourseSection value, CourseSectionRequest request) {
    value.setTitle(request.title().trim()); value.setDescription(trim(request.description()));
  }
  private void applyContent(CourseContent value, CourseContentRequest request) {
    value.setTitle(request.title().trim()); value.setSummary(trim(request.summary()));
    value.setContentHtml(trim(request.contentHtml())); value.setAudioUrl(trim(request.audioUrl()));
    value.setVideoUrl(trim(request.videoUrl())); value.setDocumentUrl(trim(request.documentUrl()));
    value.setContentType(request.contentType()); value.setIsPreview(request.preview());
  }
  private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
  private AdminCourseContentResponse toAdminContent(CourseContent value) {
    return new AdminCourseContentResponse(value.getId(), value.getSectionId().getId(), value.getTitle(),
        value.getSummary(), value.getContentHtml(), value.getAudioUrl(), value.getVideoUrl(),
        value.getDocumentUrl(), value.getContentType(), value.getDisplayOrder(), value.getIsPreview(),
        value.getPublicationStatus());
  }
  private void validateReorder(List<Integer> requested, List<Integer> found, String label) {
    if (new HashSet<>(requested).size() != requested.size() || !new HashSet<>(found).equals(new HashSet<>(requested)))
      throw new IllegalArgumentException("Danh sách " + label + " không hợp lệ hoặc chứa ID trùng lặp");
  }
  private CourseSection findSection(List<CourseSection> values, Integer id) {
    return values.stream().filter(value -> value.getId().equals(id)).findFirst().orElseThrow();
  }
  private CourseContent findContent(List<CourseContent> values, Integer id) {
    return values.stream().filter(value -> value.getId().equals(id)).findFirst().orElseThrow();
  }
  private void moveSectionsToTemporaryOrder(List<CourseSection> values) {
    values.forEach(value -> value.setDisplayOrder(value.getDisplayOrder() + 100000));
    courseSectionRepository.saveAllAndFlush(values);
  }
  private void moveContentsToTemporaryOrder(List<CourseContent> values) {
    values.forEach(value -> value.setDisplayOrder(value.getDisplayOrder() + 100000));
    courseContentRepository.saveAllAndFlush(values);
  }
  private void normalizeSections(List<CourseSection> values) {
    moveSectionsToTemporaryOrder(values);
    for (int index = 0; index < values.size(); index++) values.get(index).setDisplayOrder(index + 1);
    courseSectionRepository.saveAllAndFlush(values);
  }
  private void normalizeContents(List<CourseContent> values) {
    moveContentsToTemporaryOrder(values);
    for (int index = 0; index < values.size(); index++) values.get(index).setDisplayOrder(index + 1);
    courseContentRepository.saveAllAndFlush(values);
  }
}
