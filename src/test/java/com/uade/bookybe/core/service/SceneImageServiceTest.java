package com.uade.bookybe.core.service;

import com.uade.bookybe.config.SceneImageConfig;
import com.uade.bookybe.core.exception.BookNotFoundException;
import com.uade.bookybe.core.exception.InvalidImageSizeException;
import com.uade.bookybe.core.model.Book;
import com.uade.bookybe.core.model.SceneImageGeneration;
import com.uade.bookybe.core.model.dto.ImageResult;
import com.uade.bookybe.core.model.dto.SceneImageRequest;
import com.uade.bookybe.core.model.dto.SceneImageResponse;
import com.uade.bookybe.core.port.SceneImageGenerationRepository;
import com.uade.bookybe.core.service.gateway.OpenAIClient;
import com.uade.bookybe.infraestructure.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SceneImageServiceTest {

  @Mock
  private BookRepository bookRepository;

  @Mock
  private SceneImageGenerationRepository sceneImageGenerationRepository;

  @Mock
  private PromptCraftService promptCraftService;

  @Mock
  private OpenAIClient openAIClient;

  @Mock
  private SceneImageConfig sceneImageConfig;

  @InjectMocks
  private SceneImageService sceneImageService;

  private SceneImageRequest validRequest;
  private Book testBook;
  private Object mockBookEntity;

  @BeforeEach
  void setUp() {
    validRequest = SceneImageRequest.builder()
        .text("El amanecer tiñe de naranja la biblioteca circular con estanterías de roble")
        .style("photorealistic")
        .size("4096x2048")
        .seed(42)
        .returnBase64(false)
        .build();

    testBook = Book.builder()
        .id("123")
        .title("El Hobbit")
        .author("J.R.R. Tolkien")
        .synopsis("Un hobbit emprende una aventura épica")
        .categories(List.of("Fantasy", "Adventure"))
        .build();

    // Mock configuration
    when(sceneImageConfig.getMinTextLength()).thenReturn(15);
    when(sceneImageConfig.getMaxTextLength()).thenReturn(2000);
    when(sceneImageConfig.getDefaultSize()).thenReturn("4096x2048");

    // Create a mock book entity
    mockBookEntity = new Object() {
      public String id = "123";
      public String title = "El Hobbit";
      public String author = "J.R.R. Tolkien";
      public String synopsis = "Un hobbit emprende una aventura épica";
      public List<String> categories = List.of("Fantasy", "Adventure");
    };
  }

  @Test
  void generateSceneImage_WithValidRequest_ShouldGenerateSuccessfully() {
    // Given
    String bookId = "123";
    String craftedPrompt = "360° equirectangular panorama of ancient library...";
    ImageResult imageResult = ImageResult.builder()
        .url("https://example.com/image.png")
        .responseTimeMs(5000L)
        .build();

    when(bookRepository.findById(bookId)).thenReturn(Optional.of(mockBookEntity));
    when(sceneImageGenerationRepository.findByBookIdAndFragmentHash(eq(bookId), any(String.class)))
        .thenReturn(Optional.empty());
    when(promptCraftService.buildPrompt(any(Book.class), eq(validRequest.getText()), eq(validRequest.getStyle())))
        .thenReturn(craftedPrompt);
    when(openAIClient.generateImage(eq(craftedPrompt), eq("4096x2048"), eq(42), eq(false)))
        .thenReturn(imageResult);
    when(sceneImageGenerationRepository.save(any(SceneImageGeneration.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    SceneImageResponse response = sceneImageService.generateSceneImage(bookId, validRequest);

    // Then
    assertNotNull(response);
    assertEquals(bookId, response.getBookId());
    assertEquals(craftedPrompt, response.getCraftedPrompt());
    assertEquals("https://example.com/image.png", response.getImageUrl());
    assertEquals("4096x2048", response.getSize());
    assertEquals("photorealistic", response.getStyle());
    assertEquals(42, response.getSeed());

    verify(bookRepository).findById(bookId);
    verify(promptCraftService).buildPrompt(any(Book.class), eq(validRequest.getText()), eq(validRequest.getStyle()));
    verify(openAIClient).generateImage(eq(craftedPrompt), eq("4096x2048"), eq(42), eq(false));
    verify(sceneImageGenerationRepository).save(any(SceneImageGeneration.class));
  }

  @Test
  void generateSceneImage_WithExistingGeneration_ShouldReturnCached() {
    // Given
    String bookId = "123";
    SceneImageGeneration existingGeneration = SceneImageGeneration.builder()
        .id(1L)
        .bookId(bookId)
        .craftedPrompt("Existing prompt")
        .imageUrl("https://example.com/cached.png")
        .size("4096x2048")
        .style("photorealistic")
        .seed(42)
        .createdAt(LocalDateTime.now())
        .build();

    when(bookRepository.findById(bookId)).thenReturn(Optional.of(mockBookEntity));
    when(sceneImageGenerationRepository.findByBookIdAndFragmentHash(eq(bookId), any(String.class)))
        .thenReturn(Optional.of(existingGeneration));

    // When
    SceneImageResponse response = sceneImageService.generateSceneImage(bookId, validRequest);

    // Then
    assertNotNull(response);
    assertEquals(bookId, response.getBookId());
    assertEquals("Existing prompt", response.getCraftedPrompt());
    assertEquals("https://example.com/cached.png", response.getImageUrl());

    verify(bookRepository).findById(bookId);
    verify(sceneImageGenerationRepository).findByBookIdAndFragmentHash(eq(bookId), any(String.class));
    // Should not call OpenAI services for cached result
    verify(promptCraftService, never()).buildPrompt(any(), any(), any());
    verify(openAIClient, never()).generateImage(any(), any(), any(), anyBoolean());
  }

  @Test
  void generateSceneImage_WithNonExistentBook_ShouldThrowBookNotFoundException() {
    // Given
    String bookId = "nonexistent";
    when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(BookNotFoundException.class, () -> {
      sceneImageService.generateSceneImage(bookId, validRequest);
    });

    verify(bookRepository).findById(bookId);
    verify(promptCraftService, never()).buildPrompt(any(), any(), any());
    verify(openAIClient, never()).generateImage(any(), any(), any(), anyBoolean());
  }

  @Test
  void generateSceneImage_WithInvalidTextLength_ShouldThrowIllegalArgumentException() {
    // Given
    SceneImageRequest shortTextRequest = SceneImageRequest.builder()
        .text("Short")
        .build();

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> {
      sceneImageService.generateSceneImage("123", shortTextRequest);
    });
  }

  @Test
  void generateSceneImage_WithInvalidImageSize_ShouldThrowInvalidImageSizeException() {
    // Given
    SceneImageRequest invalidSizeRequest = SceneImageRequest.builder()
        .text("El amanecer tiñe de naranja la biblioteca circular con estanterías de roble")
        .size("1920x1080") // Invalid 2:1 ratio
        .build();

    // When & Then
    assertThrows(InvalidImageSizeException.class, () -> {
      sceneImageService.generateSceneImage("123", invalidSizeRequest);
    });
  }

  @Test
  void getBookSceneGenerations_ShouldReturnGenerationsList() {
    // Given
    String bookId = "123";
    List<SceneImageGeneration> generations = List.of(
        SceneImageGeneration.builder().id(1L).bookId(bookId).build(),
        SceneImageGeneration.builder().id(2L).bookId(bookId).build()
    );

    when(sceneImageGenerationRepository.findByBookIdOrderByCreatedAtDesc(bookId))
        .thenReturn(generations);

    // When
    List<SceneImageGeneration> result = sceneImageService.getBookSceneGenerations(bookId);

    // Then
    assertNotNull(result);
    assertEquals(2, result.size());
    verify(sceneImageGenerationRepository).findByBookIdOrderByCreatedAtDesc(bookId);
  }

  @Test
  void getBookGenerationCount_ShouldReturnCount() {
    // Given
    String bookId = "123";
    long expectedCount = 5L;

    when(sceneImageGenerationRepository.countByBookId(bookId)).thenReturn(expectedCount);

    // When
    long result = sceneImageService.getBookGenerationCount(bookId);

    // Then
    assertEquals(expectedCount, result);
    verify(sceneImageGenerationRepository).countByBookId(bookId);
  }
}
