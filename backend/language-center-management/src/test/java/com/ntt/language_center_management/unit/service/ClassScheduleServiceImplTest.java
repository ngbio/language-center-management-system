package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ntt.language_center_management.dto.request.ClassScheduleRequest;
import com.ntt.language_center_management.dto.response.ClassScheduleResponse;
import com.ntt.language_center_management.entity.*;
import com.ntt.language_center_management.enums.*;
import com.ntt.language_center_management.exception.*;
import com.ntt.language_center_management.mapper.ClassScheduleMapper;
import com.ntt.language_center_management.repository.*;
import com.ntt.language_center_management.service.impl.ClassScheduleServiceImpl;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClassScheduleServiceImplTest {
  private ClassScheduleRepository schedules;
  private CourseClassRepository classes;
  private LessonRepository lessons;
  private RoomRepository rooms;
  private ClassScheduleMapper mapper;
  private ClassScheduleServiceImpl service;

  @BeforeEach
  void setUp() {
    schedules = mock(ClassScheduleRepository.class); classes = mock(CourseClassRepository.class);
    lessons = mock(LessonRepository.class); rooms = mock(RoomRepository.class);
    mapper = mock(ClassScheduleMapper.class);
    service = new ClassScheduleServiceImpl(
        schedules, classes, lessons, rooms, mapper, "Asia/Ho_Chi_Minh");
  }

  @Test
  void shouldCreateInPersonScheduleOnlyWithActiveRoomAndNoMeetingUrl() {
    Courseclass courseClass = courseClass(); Room room = room();
    when(classes.lockById(1)).thenReturn(Optional.of(courseClass));
    when(rooms.findByIdAndStatus(3, RoomStatus.ACTIVE)).thenReturn(Optional.of(room));
    when(schedules.save(any())).thenAnswer(i -> i.getArgument(0));
    when(mapper.toResponse(any())).thenReturn(mock(ClassScheduleResponse.class));

    service.create(1, request(3, (short) 2, DeliveryMode.IN_PERSON, null));

    verify(classes).lockById(1);
    verify(rooms).findByIdAndStatus(3, RoomStatus.ACTIVE);
    assertThatThrownBy(() -> service.create(1,
        request(null, (short) 2, DeliveryMode.IN_PERSON, null)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.create(1,
        request(3, (short) 2, DeliveryMode.IN_PERSON, "https://meet.example")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldCreateOnlineScheduleOnlyWithMeetingUrlAndNoRoom() {
    when(classes.lockById(1)).thenReturn(Optional.of(courseClass()));
    when(schedules.save(any())).thenAnswer(i -> i.getArgument(0));
    when(mapper.toResponse(any())).thenReturn(mock(ClassScheduleResponse.class));

    service.create(1, request(null, (short) 2, DeliveryMode.ONLINE, "  https://meet.example  "));

    verify(rooms, never()).findByIdAndStatus(anyInt(), any());
    assertThatThrownBy(() -> service.create(1,
        request(3, (short) 2, DeliveryMode.ONLINE, "https://meet.example")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.create(1,
        request(null, (short) 2, DeliveryMode.ONLINE, "  ")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectInvalidTimeOrDayOfWeek() {
    when(classes.lockById(1)).thenReturn(Optional.of(courseClass()));
    ClassScheduleRequest invalidTime = new ClassScheduleRequest(null, (short) 2,
        LocalTime.of(10, 0), LocalTime.of(9, 0), DeliveryMode.ONLINE, "url");
    assertThatThrownBy(() -> service.create(1, invalidTime))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.create(1,
        request(null, (short) 0, DeliveryMode.ONLINE, "url")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectConflictInsideClassOrWithRoomAndTeacher() {
    when(classes.lockById(1)).thenReturn(Optional.of(courseClass()));
    when(schedules.existsClassTimeConflict(any(), eq(1), anyShort(), any(), any()))
        .thenReturn(true);
    assertThatThrownBy(() -> service.create(1,
        request(null, (short) 2, DeliveryMode.ONLINE, "url")))
        .isInstanceOf(DuplicateResourceException.class);

    reset(schedules);
    when(schedules.existsResourceConflict(any(), any(), any(), any(), any(), anyShort(), any(), any()))
        .thenReturn(true);
    assertThatThrownBy(() -> service.create(1,
        request(null, (short) 2, DeliveryMode.ONLINE, "url")))
        .isInstanceOf(DuplicateResourceException.class);
  }

  @Test
  void shouldExcludeCurrentScheduleAndUseLocksWhenUpdating() {
    Courseclass courseClass = courseClass(); Classschedule schedule = schedule(courseClass);
    when(schedules.lockById(9)).thenReturn(Optional.of(schedule));
    when(classes.lockById(1)).thenReturn(Optional.of(courseClass));
    when(schedules.save(schedule)).thenReturn(schedule);
    when(mapper.toResponse(schedule)).thenReturn(mock(ClassScheduleResponse.class));

    service.update(9, request(null, (short) 3, DeliveryMode.ONLINE, "url"));

    verify(schedules).lockById(9);
    verify(classes).lockById(1);
    verify(schedules).existsClassTimeConflict(eq(9), eq(1), eq((short) 3), any(), any());
    verify(schedules).existsResourceConflict(eq(9), any(), any(), any(), any(),
        eq((short) 3), any(), any());
  }

  @Test
  void shouldRejectChangesWhenClassIsFinishedOrLessonsExist() {
    Courseclass cancelled = courseClass(); cancelled.setStatus(ClassStatus.CANCELLED);
    when(classes.lockById(1)).thenReturn(Optional.of(cancelled));
    assertThatThrownBy(() -> service.create(1,
        request(null, (short) 2, DeliveryMode.ONLINE, "url")))
        .isInstanceOf(IllegalArgumentException.class);

    Courseclass draft = courseClass();
    when(classes.lockById(1)).thenReturn(Optional.of(draft));
    when(lessons.countByClassScheduleId_CourseClassId_Id(1)).thenReturn(1L);
    assertThatThrownBy(() -> service.create(1,
        request(null, (short) 2, DeliveryMode.ONLINE, "url")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectDeleteWhenScheduleHasLessonsAndLockAggregate() {
    Courseclass courseClass = courseClass(); Classschedule schedule = schedule(courseClass);
    when(schedules.lockById(9)).thenReturn(Optional.of(schedule));
    when(classes.lockById(1)).thenReturn(Optional.of(courseClass));
    when(lessons.existsByClassScheduleId_Id(9)).thenReturn(true);

    assertThatThrownBy(() -> service.delete(9)).isInstanceOf(IllegalArgumentException.class);
    verify(schedules).lockById(9); verify(classes).lockById(1);
    verify(schedules, never()).delete(any(Classschedule.class));
  }

  @Test
  void shouldThrowWhenClassScheduleOrActiveRoomDoesNotExist() {
    when(classes.lockById(99)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.create(99,
        request(null, (short) 2, DeliveryMode.ONLINE, "url")))
        .isInstanceOf(ResourceNotFoundException.class);
    when(schedules.lockById(98)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.update(98,
        request(null, (short) 2, DeliveryMode.ONLINE, "url")))
        .isInstanceOf(ResourceNotFoundException.class);
    when(classes.lockById(1)).thenReturn(Optional.of(courseClass()));
    when(rooms.findByIdAndStatus(77, RoomStatus.ACTIVE)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.create(1,
        request(77, (short) 2, DeliveryMode.IN_PERSON, null)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  private ClassScheduleRequest request(Integer roomId, short day, DeliveryMode mode, String url) {
    return new ClassScheduleRequest(roomId, day, LocalTime.of(8, 0), LocalTime.of(10, 0), mode, url);
  }
  private Courseclass courseClass() {
    Courseclass value = new Courseclass(1); value.setStatus(ClassStatus.DRAFT);
    value.setMaxStudents(20); value.setStartDate(java.sql.Date.valueOf(LocalDate.now().plusDays(1)));
    value.setEndDate(java.sql.Date.valueOf(LocalDate.now().plusDays(30)));
    Teacher teacher = new Teacher(7); value.setTeacherId(teacher); return value;
  }
  private Room room() { Room room = new Room(3); room.setCapacity(30); room.setStatus(RoomStatus.ACTIVE); return room; }
  private Classschedule schedule(Courseclass owner) {
    Classschedule value = new Classschedule(); value.setId(9); value.setCourseClassId(owner);
    value.setDayOfWeek((short) 2); value.setStartTime(java.sql.Time.valueOf("08:00:00"));
    value.setEndTime(java.sql.Time.valueOf("10:00:00")); return value;
  }
}
