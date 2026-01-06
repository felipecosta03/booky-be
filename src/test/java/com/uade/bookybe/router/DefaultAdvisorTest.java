package com.uade.bookybe.router;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DefaultAdvisorTest {

    @InjectMocks
    private DefaultAdvisor advisor;

    @Test
    void handleException_ReturnsBadRequest() {
        Exception exception = new RuntimeException("Test exception");

        ResponseEntity<Exception> response = advisor.handleException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test exception", response.getBody().getMessage());
    }

    @Test
    void handleException_WithNullPointerException() {
        Exception exception = new NullPointerException("Null pointer");

        ResponseEntity<Exception> response = advisor.handleException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Null pointer", response.getBody().getMessage());
    }

    @Test
    void handleException_WithIllegalArgumentException() {
        Exception exception = new IllegalArgumentException("Invalid argument");

        ResponseEntity<Exception> response = advisor.handleException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid argument", response.getBody().getMessage());
    }
}

