package com.ntt.language_center_management.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    @Test
    void duplicateResourceUsesConsistentConflictResponse() {
        var response = new GlobalExceptionHandler()
                .handleDuplicateResource(new DuplicateResourceException("Email đã tồn tại"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
        assertEquals("Email đã tồn tại", response.getBody().message());
        assertNull(response.getBody().data());
    }
}
