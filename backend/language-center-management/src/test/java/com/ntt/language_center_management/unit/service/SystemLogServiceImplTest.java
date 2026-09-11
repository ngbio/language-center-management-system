package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.entity.SystemLog;
import com.ntt.language_center_management.enums.SystemLogLevel;
import com.ntt.language_center_management.repository.SystemLogRepository;
import com.ntt.language_center_management.service.impl.SystemLogServiceImpl;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class SystemLogServiceImplTest {
  private SystemLogRepository repository;
  private SystemLogServiceImpl service;

  @BeforeEach
  void setUp() {
    repository = mock(SystemLogRepository.class);
    service = new SystemLogServiceImpl(repository);
  }

  @Test
  void shouldValidateSearchPagingDatesAndLevel() {
    LocalDateTime now = LocalDateTime.now();
    assertThatThrownBy(() -> service.search(null, null, null, null, null, -1, 20))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.search(null, null, null, null, null, 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.search(null, null, null, null, null, 0, 101))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.search(null, null, null, now, now.minusSeconds(1), 0, 20))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.search("INFO", null, null, null, null, 0, 20))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("WARN");
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldMapSearchResultAndUseDescendingCreatedAtPage() {
    SystemLog entry = new SystemLog();
    entry.setId(8L);
    entry.setLevel(SystemLogLevel.ERROR);
    entry.setEventType("HTTP_REQUEST_FAILED");
    entry.setMessage("GET /api failed");
    entry.setRequestId("req-1");
    entry.setCreatedAt(LocalDateTime.of(2026, 9, 11, 10, 0));
    when(repository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(java.util.List.of(entry)));

    var result = service.search(" error ", " HTTP_REQUEST_FAILED ", " req-1 ",
        null, null, 1, 10);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().getFirst().level()).isEqualTo("ERROR");
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(repository).findAll(any(Specification.class), pageable.capture());
    assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
    assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
    assertThat(pageable.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
  }

  @Test
  void shouldRecordWarnForAuthenticationFailureAndErrorForServerFailure() {
    service.recordHttpFailure(401, "GET", "/api/private", "req-1", "a@example.com");
    service.recordHttpFailure(500, "POST", "/api/payments", "req-2", null);

    ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
    verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
    assertThat(captor.getAllValues().get(0).getLevel()).isEqualTo(SystemLogLevel.WARN);
    assertThat(captor.getAllValues().get(0).getEventType()).isEqualTo("AUTHENTICATION_FAILURE");
    assertThat(captor.getAllValues().get(1).getLevel()).isEqualTo(SystemLogLevel.ERROR);
    assertThat(captor.getAllValues().get(1).getEventType()).isEqualTo("HTTP_REQUEST_FAILED");
  }

  @Test
  void shouldNotBreakRequestWhenPersistingSystemLogFails() {
    when(repository.save(any(SystemLog.class))).thenThrow(new RuntimeException("database down"));

    assertThatCode(() -> service.recordHttpFailure(
        400, "GET", "/api/test", "req-1", null)).doesNotThrowAnyException();
  }
}
