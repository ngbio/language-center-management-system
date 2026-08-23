package com.ntt.language_center_management.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ntt.language_center_management.dto.request.LanguageRequest;
import com.ntt.language_center_management.service.LanguageService;

class LanguageControllerTest {

    @Test
    void saveReturnsFormWhenValidationHasErrors() {
        LanguageService languageService = mock(LanguageService.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        String view = new LanguageController(languageService).save(
                new LanguageRequest(),
                bindingResult,
                mock(Model.class),
                mock(RedirectAttributes.class));

        assertEquals("language-form", view);
        verify(languageService, never()).addOrUpdateLanguage(org.mockito.ArgumentMatchers.any());
    }
}
