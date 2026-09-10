package com.ntt.language_center_management.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.dto.request.CourseContentRequest;
import com.ntt.language_center_management.dto.request.CourseSectionRequest;
import com.ntt.language_center_management.entity.Course;
import com.ntt.language_center_management.entity.CourseContent;
import com.ntt.language_center_management.entity.CourseSection;
import com.ntt.language_center_management.enums.CourseContentType;
import com.ntt.language_center_management.enums.PublicationStatus;
import com.ntt.language_center_management.mapper.CourseCurriculumMapper;
import com.ntt.language_center_management.repository.*;
import com.ntt.language_center_management.service.impl.CourseCurriculumServiceImpl;
import java.util.List;
import java.util.Optional;
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

  private static Course course(int id) { Course value = new Course(); value.setId(id); return value; }
  private static CourseSection section(int id, Course course) {
    CourseSection value = new CourseSection(); value.setId(id); value.setCourseId(course); return value;
  }
  private static CourseContent content(int id, CourseSection section) {
    CourseContent value = new CourseContent(); value.setId(id); value.setSectionId(section); return value;
  }

  private static class Fixture {
    final CourseRepository courseRepository = mock(CourseRepository.class);
    final CourseSectionRepository sectionRepository = mock(CourseSectionRepository.class);
    final CourseContentRepository contentRepository = mock(CourseContentRepository.class);
    final CourseCurriculumServiceImpl service = new CourseCurriculumServiceImpl(courseRepository,
        sectionRepository, contentRepository, new CourseCurriculumMapper(),
        mock(StudentRepository.class), mock(EnrollmentRepository.class));
  }
}
