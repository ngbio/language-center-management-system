package com.ntt.language_center_management.unit.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ntt.language_center_management.controller.admin.AdminLanguageApiController;
import com.ntt.language_center_management.dto.request.LanguageRequest;
import com.ntt.language_center_management.service.LanguageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AdminLanguageApiControllerTest {

  @Test
  void shouldClearClientIdAndReturnCreatedWhenCreatingLanguage() {
    LanguageService languageService = mock(LanguageService.class);
    LanguageRequest request = new LanguageRequest();
    request.setId(99);

    var response = new AdminLanguageApiController(languageService).create(request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(201, response.getBody().status());
    assertEquals(null, request.getId());
    verify(languageService).addOrUpdateLanguage(request);
  }
}
