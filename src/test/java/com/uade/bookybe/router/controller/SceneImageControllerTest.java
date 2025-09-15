package com.uade.bookybe.router.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uade.bookybe.core.exception.BookNotFoundException;
import com.uade.bookybe.core.exception.InvalidImageSizeException;
import com.uade.bookybe.core.exception.OpenAIServiceException;
import com.uade.bookybe.core.model.dto.SceneImageRequest;
import com.uade.bookybe.core.model.dto.SceneImageResponse;
import com.uade.bookybe.core.service.SceneImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SceneImageController.class)
class SceneImageControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private SceneImageService sceneImageService;

  @Autowired
  private ObjectMapper objectMapper;

  private SceneImageRequest validRequest;
  private SceneImageResponse validResponse;

  @BeforeEach
  void setUp() {
    validRequest = SceneImageRequest.builder()
        .text("El amanecer tiñe de naranja la biblioteca circular con estanterías de roble")
        .style("photorealistic")
        .size("4096x2048")
        .seed(42)
        .returnBase64(false)
        .build();

    validResponse = SceneImageResponse.builder()
        .bookId("123")
        .craftedPrompt("360° equirectangular panorama of ancient library with orange dawn light...")
        .imageUrl("https://example.com/generated-image.png")
        .size("4096x2048")
        .style("photorealistic")
        .seed(42)
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  void generateSceneImage_WithValidRequest_ShouldReturn200() throws Exception {
    // Given
    String bookId = "123";
    when(sceneImageService.generateSceneImage(eq(bookId), any(SceneImageRequest.class)))
        .thenReturn(validResponse);

    // When & Then
    mockMvc.perform(post("/api/books/{bookId}/scene-image", bookId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.book_id").value("123"))
        .andExpect(jsonPath("$.crafted_prompt").value(validResponse.getCraftedPrompt()))
        .andExpect(jsonPath("$.image_url").value("https://example.com/generated-image.png"))
        .andExpect(jsonPath("$.size").value("4096x2048"))
        .andExpect(jsonPath("$.style").value("photorealistic"))
        .andExpect(jsonPath("$.seed").value(42));

    verify(sceneImageService).generateSceneImage(eq(bookId), any(SceneImageRequest.class));
  }

  @Test
  void generateSceneImage_WithInvalidText_ShouldReturn400() throws Exception {
    // Given
    SceneImageRequest invalidRequest = SceneImageRequest.builder()
        .text("") // Empty text
        .build();

    // When & Then
    mockMvc.perform(post("/api/books/{bookId}/scene-image", "123")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void generateSceneImage_WithNonExistentBook_ShouldReturn404() throws Exception {
    // Given
    String bookId = "nonexistent";
    when(sceneImageService.generateSceneImage(eq(bookId), any(SceneImageRequest.class)))
        .thenThrow(new BookNotFoundException(bookId));

    // When & Then
    mockMvc.perform(post("/api/books/{bookId}/scene-image", bookId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isNotFound());

    verify(sceneImageService).generateSceneImage(eq(bookId), any(SceneImageRequest.class));
  }

  @Test
  void generateSceneImage_WithInvalidImageSize_ShouldReturn422() throws Exception {
    // Given
    String bookId = "123";
    when(sceneImageService.generateSceneImage(eq(bookId), any(SceneImageRequest.class)))
        .thenThrow(new InvalidImageSizeException("Invalid size format"));

    // When & Then
    mockMvc.perform(post("/api/books/{bookId}/scene-image", bookId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isUnprocessableEntity());

    verify(sceneImageService).generateSceneImage(eq(bookId), any(SceneImageRequest.class));
  }

  @Test
  void generateSceneImage_WithOpenAIServiceError_ShouldReturn503() throws Exception {
    // Given
    String bookId = "123";
    when(sceneImageService.generateSceneImage(eq(bookId), any(SceneImageRequest.class)))
        .thenThrow(new OpenAIServiceException("OpenAI service unavailable"));

    // When & Then
    mockMvc.perform(post("/api/books/{bookId}/scene-image", bookId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isServiceUnavailable());

    verify(sceneImageService).generateSceneImage(eq(bookId), any(SceneImageRequest.class));
  }

  @Test
  void getBookSceneGenerations_ShouldReturn200() throws Exception {
    // Given
    String bookId = "123";
    when(sceneImageService.getBookSceneGenerations(bookId))
        .thenReturn(java.util.List.of());

    // When & Then
    mockMvc.perform(get("/api/books/{bookId}/scene-generations", bookId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());

    verify(sceneImageService).getBookSceneGenerations(bookId);
  }

  @Test
  void getBookGenerationCount_ShouldReturn200() throws Exception {
    // Given
    String bookId = "123";
    long expectedCount = 5L;
    when(sceneImageService.getBookGenerationCount(bookId)).thenReturn(expectedCount);

    // When & Then
    mockMvc.perform(get("/api/books/{bookId}/scene-generations/count", bookId))
        .andExpect(status().isOk())
        .andExpect(content().string("5"));

    verify(sceneImageService).getBookGenerationCount(bookId);
  }
}
