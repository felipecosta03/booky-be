package com.uade.bookybe.config;

import com.uade.bookybe.core.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Mock
    private WebRequest webRequest;

    @Test
    void handleNotFoundException_ShouldReturn404() {
        NotFoundException ex = new NotFoundException("Resource not found");
        when(webRequest.getDescription(false)).thenReturn("uri=/test");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleNotFoundException(ex, webRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Resource not found", response.getBody().getMessage());
    }

    @Test
    void handleConflictException_ShouldReturn409() {
        ConflictException ex = new ConflictException("Conflict occurred");
        when(webRequest.getDescription(false)).thenReturn("uri=/test");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleConflictException(ex, webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Conflict occurred", response.getBody().getMessage());
    }

    @Test
    void handleUnauthorizedException_ShouldReturn401() {
        UnauthorizedException ex = new UnauthorizedException("Unauthorized");
        when(webRequest.getDescription(false)).thenReturn("uri=/test");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleUnauthorizedException(ex, webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleBadRequestException_ShouldReturn400() {
        BadRequestException ex = new BadRequestException("Bad request");
        when(webRequest.getDescription(false)).thenReturn("uri=/test");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBadRequestException(ex, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}

