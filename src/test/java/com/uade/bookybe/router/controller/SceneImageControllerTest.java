package com.uade.bookybe.router.controller;

import com.uade.bookybe.core.exception.BookNotFoundException;
import com.uade.bookybe.core.exception.InvalidImageSizeException;
import com.uade.bookybe.core.exception.OpenAIServiceException;
import com.uade.bookybe.core.model.dto.SceneImageRequest;
import com.uade.bookybe.core.model.dto.SceneImageResponse;
import com.uade.bookybe.core.model.SceneImageGeneration;
import com.uade.bookybe.core.service.SceneImageService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SceneImageControllerTest {

    @Mock
    private SceneImageService sceneImageService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private SceneImageController controller;

    private SceneImageRequest request;
    private SceneImageResponse response;

    @BeforeEach
    void setUp() {
        request = new SceneImageRequest();
        request.setText("This is a test scene description for generating an image.");

        response = new SceneImageResponse();
        response.setImageUrl("https://example.com/image.jpg");
        response.setCraftedPrompt("Test prompt");

        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void generateSceneImage_Success() {
        when(sceneImageService.generateSceneImage(anyString(), any(SceneImageRequest.class)))
                .thenReturn(response);

        ResponseEntity<SceneImageResponse> result = 
                controller.generateSceneImage("club1", request, httpRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("https://example.com/image.jpg", result.getBody().getImageUrl());
        verify(sceneImageService).generateSceneImage("club1", request);
    }

    @Test
    void generateSceneImage_InvalidArgument() {
        when(sceneImageService.generateSceneImage(anyString(), any(SceneImageRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid text"));

        ResponseEntity<SceneImageResponse> result = 
                controller.generateSceneImage("club1", request, httpRequest);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void generateSceneImage_BookNotFound() {
        when(sceneImageService.generateSceneImage(anyString(), any(SceneImageRequest.class)))
                .thenThrow(new BookNotFoundException("Book not found"));

        ResponseEntity<SceneImageResponse> result = 
                controller.generateSceneImage("club1", request, httpRequest);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void generateSceneImage_InvalidImageSize() {
        when(sceneImageService.generateSceneImage(anyString(), any(SceneImageRequest.class)))
                .thenThrow(new InvalidImageSizeException("Invalid size"));

        ResponseEntity<SceneImageResponse> result = 
                controller.generateSceneImage("club1", request, httpRequest);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
    }

    @Test
    void generateSceneImage_OpenAIServiceException() {
        when(sceneImageService.generateSceneImage(anyString(), any(SceneImageRequest.class)))
                .thenThrow(new OpenAIServiceException("OpenAI error"));

        ResponseEntity<SceneImageResponse> result = 
                controller.generateSceneImage("club1", request, httpRequest);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatusCode());
    }

    @Test
    void generateSceneImage_UnexpectedException() {
        when(sceneImageService.generateSceneImage(anyString(), any(SceneImageRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<SceneImageResponse> result = 
                controller.generateSceneImage("club1", request, httpRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    void generateSceneImage_RateLimitExceeded() {
        when(sceneImageService.generateSceneImage(anyString(), any(SceneImageRequest.class)))
                .thenReturn(response);

        // Make 11 requests to trigger rate limit (max is 10 per minute)
        for (int i = 0; i < 11; i++) {
            ResponseEntity<SceneImageResponse> result = 
                    controller.generateSceneImage("club1", request, httpRequest);
            
            if (i < 10) {
                assertEquals(HttpStatus.OK, result.getStatusCode());
            } else {
                assertEquals(HttpStatus.TOO_MANY_REQUESTS, result.getStatusCode());
            }
        }
    }

    @Test
    void generateSceneImage_WithXForwardedFor() {
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        when(sceneImageService.generateSceneImage(anyString(), any(SceneImageRequest.class)))
                .thenReturn(response);

        ResponseEntity<SceneImageResponse> result = 
                controller.generateSceneImage("club1", request, httpRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getReadingClubSceneGenerations_Success() {
        SceneImageGeneration gen1 = SceneImageGeneration.builder()
                .id(1L)
                .readingClubId("club1")
                .imageUrl("url1")
                .bookId("book1")
                .fragmentHash("hash1")
                .craftedPrompt("prompt1")
                .size("1024x1024")
                .build();
        SceneImageGeneration gen2 = SceneImageGeneration.builder()
                .id(2L)
                .readingClubId("club1")
                .imageUrl("url2")
                .bookId("book1")
                .fragmentHash("hash2")
                .craftedPrompt("prompt2")
                .size("1024x1024")
                .build();
        List<SceneImageGeneration> generations = Arrays.asList(gen1, gen2);
        when(sceneImageService.getReadingClubSceneGenerations("club1"))
                .thenReturn(generations);

        ResponseEntity<List<Object>> result = 
                controller.getReadingClubSceneGenerations("club1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(2, result.getBody().size());
    }

    @Test
    void getReadingClubSceneGenerations_Error() {
        when(sceneImageService.getReadingClubSceneGenerations("club1"))
                .thenThrow(new RuntimeException("Database error"));

        ResponseEntity<List<Object>> result = 
                controller.getReadingClubSceneGenerations("club1");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    void getReadingClubGenerationCount_Success() {
        when(sceneImageService.getReadingClubGenerationCount("club1"))
                .thenReturn(5L);

        ResponseEntity<Long> result = 
                controller.getReadingClubGenerationCount("club1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(5L, result.getBody());
    }

    @Test
    void getReadingClubGenerationCount_Error() {
        when(sceneImageService.getReadingClubGenerationCount("club1"))
                .thenThrow(new RuntimeException("Database error"));

        ResponseEntity<Long> result = 
                controller.getReadingClubGenerationCount("club1");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }
}

