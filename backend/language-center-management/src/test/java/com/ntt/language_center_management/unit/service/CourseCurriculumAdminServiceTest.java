package com.ntt.language_center_management.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.dto.request.CourseContentRequest;
import com.ntt.language_center_management.dto.request.CourseSectionRequest;
import com.ntt.language_center_management.entity.Course;
import com.ntt.language_center_management.entity.CourseContent;
import com.ntt.language_center_management.entity.CourseSection;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.enums.CatalogStatus;
import com.ntt.language_center_management.enums.CourseContentType;
import com.ntt.language_center_management.enums.PublicationStatus;
import com.ntt.language_center_management.mapper.CourseCurriculumMapper;
import com.ntt.language_center_management.repository.*;
import com.ntt.language_center_management.service.impl.CourseCurriculumServiceImpl;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CourseCurriculumAdminServiceTest {
  @Test
  void shouldAppendSectionWhenCreatingForCourse() {
    Fixture fixture = new Fixture();
    Course course = course(1);
    when(fixture.courseRepository.findById(1)).thenReturn(Optional.of(course));
    when(fixture.sectionRepository.findByCourseId_IdOrderByDisplayOrderAsc(1)).thenReturn(List.of());
    when(fixture.sectionRepository.save(any(CourseSection.class))).thenAnswer(value -> value.getArgument(0));

    var response = fixture.service.createSection(1, new CourseSectionRequest(" Phần 1 ", " Mô tả "));

    assertEquals("Phần 1", response.title());
    assertEquals(1, response.displayOrder());
  }

  @Test
  void shouldStartAsDraftWhenCreatingContent() {
    Fixture fixture = new Fixture();
    CourseSection section = section(2, course(1));
    when(fixture.sectionRepository.findById(2)).thenReturn(Optional.of(section));
    when(fixture.contentRepository.findBySectionId_IdOrderByDisplayOrderAsc(2)).thenReturn(List.of());
    when(fixture.contentRepository.save(any(CourseContent.class))).thenAnswer(value -> value.getArgument(0));
    CourseContentRequest request = new CourseContentRequest("Bài 1", null, "<p>Nội dung</p>",
        null, null, null, CourseContentType.LESSON, true);

    var response = fixture.service.createContent(2, request);

    assertEquals(PublicationStatus.DRAFT, response.publicationStatus());
    assertEquals(1, response.displayOrder());
  }

  @Test
  void shouldRejectReorderWhenContentsBelongToDifferentSections() {
    Fixture fixture = new Fixture();
    CourseContent first = content(10, section(1, course(1)));
    CourseContent second = content(11, section(2, course(1)));
    when(fixture.contentRepository.findAllById(List.of(10, 11))).thenReturn(List.of(first, second));

    assertThrows(IllegalArgumentException.class,
        () -> fixture.service.reorderContents(List.of(10, 11)));
  }

  @Test
  void shouldReturnPublishedSectionsInRepositoryOrder() {
    Fixture fixture = new Fixture();
    Course course = publishedCourse(1, BigDecimal.TEN);
    CourseSection second = section(2, course); second.setTitle("Second"); second.setDisplayOrder(2);
    CourseSection first = section(1, course); first.setTitle("First"); first.setDisplayOrder(1);
    when(fixture.courseRepository.findById(1)).thenReturn(Optional.of(course));
    when(fixture.sectionRepository.findByCourseId_IdOrderByDisplayOrderAsc(1))
        .thenReturn(List.of(first, second));

    var result = fixture.service.getPublishedSections(1);

    assertEquals(List.of(1, 2), result.stream().map(value -> value.id()).toList());
  }

  @Test
  void shouldReturnOnlyPreviewContentsWhenPrincipalIsMissing() {
    Fixture fixture = new Fixture();
    CourseSection section = section(2, publishedCourse(1, BigDecimal.TEN));
    CourseContent preview = publishedContent(10, section, true);
    when(fixture.sectionRepository.findByIdAndCourseId_StatusAndCourseId_PublicationStatus(
        2, CatalogStatus.ACTIVE, PublicationStatus.PUBLISHED)).thenReturn(Optional.of(section));
    when(fixture.contentRepository
        .findBySectionId_IdAndPublicationStatusAndIsPreviewTrueOrderByDisplayOrderAsc(
            2, PublicationStatus.PUBLISHED)).thenReturn(List.of(preview));

    var result = fixture.service.getPublishedContents(2, null);

    assertEquals(List.of(10), result.stream().map(value -> value.id()).toList());
    verify(fixture.contentRepository, never())
        .findBySectionId_IdAndPublicationStatusOrderByDisplayOrderAsc(any(), any());
  }

  @Test
  void shouldReturnAllPublishedContentsWhenStudentHasPaidConfirmedEnrollment() {
    Fixture fixture = new Fixture();
    CourseSection section = section(2, publishedCourse(1, BigDecimal.TEN));
    Student student = new Student(7, "HV007");
    when(fixture.sectionRepository.findByIdAndCourseId_StatusAndCourseId_PublicationStatus(
        2, CatalogStatus.ACTIVE, PublicationStatus.PUBLISHED)).thenReturn(Optional.of(section));
    when(fixture.studentRepository.findByUserId_EmailIgnoreCase("student@example.com"))
        .thenReturn(Optional.of(student));
    when(fixture.enrollmentRepository.existsPaidConfirmedAccess(7, 1)).thenReturn(true);
    when(fixture.contentRepository
        .findBySectionId_IdAndPublicationStatusOrderByDisplayOrderAsc(2, PublicationStatus.PUBLISHED))
        .thenReturn(List.of(publishedContent(10, section, true), publishedContent(11, section, false)));

    var result = fixture.service.getPublishedContents(2, () -> "student@example.com");

    assertEquals(List.of(10, 11), result.stream().map(value -> value.id()).toList());
  }

  @Test
  void shouldNotReturnLockedContentWhenPrincipalIsNotStudent() {
    Fixture fixture = new Fixture();
    CourseSection section = section(2, publishedCourse(1, BigDecimal.TEN));
    when(fixture.sectionRepository.findByIdAndCourseId_StatusAndCourseId_PublicationStatus(
        2, CatalogStatus.ACTIVE, PublicationStatus.PUBLISHED)).thenReturn(Optional.of(section));
    when(fixture.studentRepository.findByUserId_EmailIgnoreCase("admin@example.com"))
        .thenReturn(Optional.empty());
    when(fixture.contentRepository
        .findBySectionId_IdAndPublicationStatusAndIsPreviewTrueOrderByDisplayOrderAsc(
            2, PublicationStatus.PUBLISHED)).thenReturn(List.of());

    assertEquals(List.of(), fixture.service.getPublishedContents(2, () -> "admin@example.com"));
    verify(fixture.enrollmentRepository, never()).existsPaidConfirmedAccess(any(), any());
  }

  @Test
  void shouldThrowWhenPublishedCourseOrSectionDoesNotExist() {
    Fixture fixture = new Fixture();
    when(fixture.courseRepository.findById(99)).thenReturn(Optional.empty());
    assertThrows(com.ntt.language_center_management.exception.ResourceNotFoundException.class,
        () -> fixture.service.getPublishedSections(99));
    when(fixture.sectionRepository.findByIdAndCourseId_StatusAndCourseId_PublicationStatus(
        88, CatalogStatus.ACTIVE, PublicationStatus.PUBLISHED)).thenReturn(Optional.empty());
    assertThrows(com.ntt.language_center_management.exception.ResourceNotFoundException.class,
        () -> fixture.service.getPublishedContents(88, null));
  }

  @Test
  void shouldMapAllMediaPublicationAndPreviewFields() {
    CourseSection section = section(2, course(1));
    CourseContent value = publishedContent(10, section, true);
    value.setAudioUrl("audio.mp3"); value.setVideoUrl("video.mp4");
    value.setDocumentUrl("document.pdf"); value.setContentHtml("<p>Content</p>");

    var response = new CourseCurriculumMapper().toContentResponse(value);

    assertEquals("audio.mp3", response.audioUrl());
    assertEquals("video.mp4", response.videoUrl());
    assertEquals("document.pdf", response.documentUrl());
    assertEquals("<p>Content</p>", response.contentHtml());
    assertEquals(CourseContentType.LESSON, response.contentType());
    assertEquals(true, response.preview());
  }

  private static Course course(int id) { Course value = new Course(); value.setId(id); return value; }
  private static CourseSection section(int id, Course course) {
    CourseSection value = new CourseSection(); value.setId(id); value.setCourseId(course); return value;
  }
  private static CourseContent content(int id, CourseSection section) {
    CourseContent value = new CourseContent(); value.setId(id); value.setSectionId(section); return value;
  }
  private static Course publishedCourse(int id, BigDecimal fee) {
    Course value = course(id); value.setStatus(CatalogStatus.ACTIVE);
    value.setPublicationStatus(PublicationStatus.PUBLISHED); value.setTuitionFee(fee); return value;
  }
  private static CourseContent publishedContent(int id, CourseSection section, boolean preview) {
    CourseContent value = content(id, section); value.setTitle("Content " + id);
    value.setContentType(CourseContentType.LESSON); value.setDisplayOrder(id);
    value.setIsPreview(preview); value.setPublicationStatus(PublicationStatus.PUBLISHED); return value;
  }

  private static class Fixture {
    final CourseRepository courseRepository = mock(CourseRepository.class);
    final CourseSectionRepository sectionRepository = mock(CourseSectionRepository.class);
    final CourseContentRepository contentRepository = mock(CourseContentRepository.class);
    final StudentRepository studentRepository = mock(StudentRepository.class);
    final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    final CourseCurriculumServiceImpl service = new CourseCurriculumServiceImpl(courseRepository,
        sectionRepository, contentRepository, new CourseCurriculumMapper(),
        studentRepository, enrollmentRepository);
  }
}
