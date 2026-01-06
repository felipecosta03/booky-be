package com.uade.bookybe.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.uade.bookybe.config.SceneImageConfig;
import com.uade.bookybe.core.exception.BookNotFoundException;
import com.uade.bookybe.core.exception.InvalidImageSizeException;
import com.uade.bookybe.core.exception.OpenAIServiceException;
import com.uade.bookybe.core.model.Book;
import com.uade.bookybe.core.model.ReadingClub;
import com.uade.bookybe.core.model.SceneImageGeneration;
import com.uade.bookybe.core.model.dto.ImageResult;
import com.uade.bookybe.core.model.dto.SceneImageRequest;
import com.uade.bookybe.core.model.dto.SceneImageResponse;
import com.uade.bookybe.core.port.SceneImageGenerationRepository;
import com.uade.bookybe.core.service.gateway.OpenAIClient;
import com.uade.bookybe.core.usecase.BookService;
import com.uade.bookybe.core.usecase.ReadingClubService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SceneImageServiceTest {

  @Mock private BookService bookService;
  @Mock private ReadingClubService readingClubService;
  @Mock private SceneImageGenerationRepository sceneImageGenerationRepository;
  @Mock private PromptCraftService promptCraftService;
  @Mock private OpenAIClient openAIClient;
  @Mock private SceneImageConfig sceneImageConfig;

  @InjectMocks private SceneImageService sut;

  private static SceneImageRequest req(
      String text, String size, String style, Integer seed, Boolean returnBase64) {
    return SceneImageRequest.builder()
        .text(text)
        .size(size)
        .style(style)
        .seed(seed)
        .returnBase64(returnBase64)
        .build();
  }

  // ---------------- generateSceneImage ----------------

  @Test
  void generateSceneImage_deberiaLanzarIllegalArgument_siTextoBlank() {
    // given
    SceneImageRequest request = req("   ", null, null, null, null);

    // when + then
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> sut.generateSceneImage("rc1", request));
    assertTrue(ex.getMessage().toLowerCase().contains("text"));

    then(readingClubService).shouldHaveNoInteractions();
    then(bookService).shouldHaveNoInteractions();
    then(openAIClient).shouldHaveNoInteractions();
    then(sceneImageGenerationRepository).shouldHaveNoInteractions();
  }

  @Test
  void generateSceneImage_deberiaLanzarIllegalArgument_siTextoMenorAlMinimo() {
    // given
    given(sceneImageConfig.getMinTextLength()).willReturn(10);

    SceneImageRequest request = req("corto", null, null, null, null); // len 5

    // when + then
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> sut.generateSceneImage("rc1", request));
    assertTrue(ex.getMessage().toLowerCase().contains("at least"));

    then(readingClubService).shouldHaveNoInteractions();
    then(bookService).shouldHaveNoInteractions();
  }

  @Test
  void generateSceneImage_deberiaLanzarIllegalArgument_siTextoMayorAlMaximo() {
    // given
    given(sceneImageConfig.getMinTextLength()).willReturn(1);
    given(sceneImageConfig.getMaxTextLength()).willReturn(5);

    SceneImageRequest request = req("demasiado largo", null, null, null, null); // > 5

    // when + then
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> sut.generateSceneImage("rc1", request));
    assertTrue(ex.getMessage().toLowerCase().contains("not exceed"));

    then(readingClubService).shouldHaveNoInteractions();
    then(bookService).shouldHaveNoInteractions();
  }

  @Test
  void generateSceneImage_deberiaLanzarInvalidImageSize_siSizeNoSoportado() {
    // given
    given(sceneImageConfig.getMinTextLength()).willReturn(1);
    given(sceneImageConfig.getMaxTextLength()).willReturn(2000);

    SceneImageRequest request = req("texto valido", "999x999", null, null, null);

    // when + then
    assertThrows(InvalidImageSizeException.class, () -> sut.generateSceneImage("rc1", request));

    then(readingClubService).shouldHaveNoInteractions();
    then(bookService).shouldHaveNoInteractions();
  }

  @Test
  void generateSceneImage_deberiaLanzarIllegalArgument_siReadingClubNoExiste() {
    // given
    given(sceneImageConfig.getMinTextLength()).willReturn(1);
    given(sceneImageConfig.getMaxTextLength()).willReturn(2000);

    SceneImageRequest request = req("texto valido", null, null, null, null);

    given(readingClubService.getReadingClubById("rc1")).willReturn(Optional.empty());

    // when + then
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> sut.generateSceneImage("rc1", request));
    assertTrue(ex.getMessage().toLowerCase().contains("reading club not found"));

    then(bookService).shouldHaveNoInteractions();
    then(openAIClient).shouldHaveNoInteractions();
  }

  @Test
  void generateSceneImage_deberiaLanzarBookNotFound_siLibroNoExiste() {
    // given
    given(sceneImageConfig.getMinTextLength()).willReturn(1);
    given(sceneImageConfig.getMaxTextLength()).willReturn(2000);

    ReadingClub club = ReadingClub.builder().id("rc1").bookId("b1").build();
    given(readingClubService.getReadingClubById("rc1")).willReturn(Optional.of(club));

    given(bookService.getBookById("b1")).willReturn(Optional.empty());

    SceneImageRequest request = req("texto valido", null, null, null, null);

    // when + then
    assertThrows(BookNotFoundException.class, () -> sut.generateSceneImage("rc1", request));

    then(sceneImageGenerationRepository).shouldHaveNoInteractions();
    then(openAIClient).shouldHaveNoInteractions();
  }

  @Test
  void generateSceneImage_deberiaRetornarExistente_siYaExisteGenerationParaHash() {
    // given
    given(sceneImageConfig.getMinTextLength()).willReturn(1);
    given(sceneImageConfig.getMaxTextLength()).willReturn(2000);

    ReadingClub club = ReadingClub.builder().id("rc1").bookId("b1").build();
    given(readingClubService.getReadingClubById("rc1")).willReturn(Optional.of(club));

    Book book = Book.builder().id("b1").title("T").isbn("x").build();
    given(bookService.getBookById("b1")).willReturn(Optional.of(book));

    SceneImageGeneration existing =
        SceneImageGeneration.builder()
            .bookId("b1")
            .readingClubId("rc1")
            .fragmentHash("hash")
            .craftedPrompt("p")
            .imageUrl("url")
            .imageBase64(null)
            .size("2048x1024")
            .style("photorealistic")
            .seed(1)
            .createdAt(LocalDateTime.now())
            .build();

    given(sceneImageGenerationRepository.findByReadingClubIdAndFragmentHash(eq("rc1"), anyString()))
        .willReturn(Optional.of(existing));

    SceneImageRequest request = req("Texto valido", "2048x1024", "photorealistic", 1, false);

    // when
    SceneImageResponse response = sut.generateSceneImage("rc1", request);

    // then
    assertNotNull(response);
    assertEquals("b1", response.getBookId());
    assertEquals("p", response.getCraftedPrompt());
    assertEquals("url", response.getImageUrl());
    assertEquals("2048x1024", response.getSize());

    then(openAIClient).shouldHaveNoInteractions();
    then(promptCraftService).shouldHaveNoInteractions();
    then(sceneImageGenerationRepository).should(never()).save(any());
  }

  @Test
  void generateSceneImage_deberiaForzarUrl_siSizeGrande_yReturnBase64True() {
    // given
    given(sceneImageConfig.getMinTextLength()).willReturn(1);
    given(sceneImageConfig.getMaxTextLength()).willReturn(2000);

    ReadingClub club = ReadingClub.builder().id("rc1").bookId("b1").build();
    given(readingClubService.getReadingClubById("rc1")).willReturn(Optional.of(club));

    Book book = Book.builder().id("b1").title("Book").isbn("x").build();
    given(bookService.getBookById("b1")).willReturn(Optional.of(book));

    given(sceneImageGenerationRepository.findByReadingClubIdAndFragmentHash(eq("rc1"), anyString()))
        .willReturn(Optional.empty());

    given(promptCraftService.buildPrompt(eq(book), anyString(), eq("photorealistic")))
        .willReturn("crafted");

    ImageResult imageResult =
        ImageResult.builder()
            .url("https://img")
            .base64(null)
            .responseTimeMs(123L)
            .promptTokens(50)
            .costUsd(0.01)
            .build();

    // size grande => 4096x2048; returnBase64 true pero se fuerza false
    given(openAIClient.generateImage(eq("crafted"), eq("4096x2048"), any(), eq(false)))
        .willReturn(imageResult);

    given(sceneImageGenerationRepository.save(any(SceneImageGeneration.class)))
        .willAnswer(inv -> inv.getArgument(0, SceneImageGeneration.class));

    SceneImageRequest request = req("texto valido", "4096x2048", null, 7, true);

    ArgumentCaptor<SceneImageGeneration> captor =
        ArgumentCaptor.forClass(SceneImageGeneration.class);

    // when
    SceneImageResponse response = sut.generateSceneImage("rc1", request);

    // then
    assertNotNull(response);
    assertEquals("b1", response.getBookId());
    assertEquals("crafted", response.getCraftedPrompt());
    assertEquals("https://img", response.getImageUrl());
    assertEquals("4096x2048", response.getSize());
    assertEquals("photorealistic", response.getStyle());
    assertEquals(7, response.getSeed());

    then(sceneImageGenerationRepository).should().save(captor.capture());
    SceneImageGeneration saved = captor.getValue();
    assertEquals("b1", saved.getBookId());
    assertEquals("rc1", saved.getReadingClubId());
    assertEquals("crafted", saved.getCraftedPrompt());
    assertEquals("https://img", saved.getImageUrl());
    assertNull(saved.getImageBase64());
  }

  @Test
  void generateSceneImage_deberiaGenerar_yGuardar_yRetornarResponse_cuandoNoExiste() {
    // given
    given(sceneImageConfig.getMinTextLength()).willReturn(1);
    given(sceneImageConfig.getMaxTextLength()).willReturn(2000);

    ReadingClub club = ReadingClub.builder().id("rc1").bookId("b1").build();
    given(readingClubService.getReadingClubById("rc1")).willReturn(Optional.of(club));

    Book book = Book.builder().id("b1").title("Book").isbn("x").build();
    given(bookService.getBookById("b1")).willReturn(Optional.of(book));

    given(sceneImageGenerationRepository.findByReadingClubIdAndFragmentHash(eq("rc1"), anyString()))
        .willReturn(Optional.empty());

    given(promptCraftService.buildPrompt(eq(book), anyString(), eq("anime")))
        .willReturn("crafted-anime");

    ImageResult imageResult =
        ImageResult.builder()
            .url(null)
            .base64("b64")
            .responseTimeMs(456L)
            .promptTokens(99)
            .costUsd(0.02)
            .build();

    given(openAIClient.generateImage(eq("crafted-anime"), eq("2048x1024"), eq(42), eq(true)))
        .willReturn(imageResult);

    given(sceneImageGenerationRepository.save(any(SceneImageGeneration.class)))
        .willAnswer(inv -> inv.getArgument(0, SceneImageGeneration.class));

    SceneImageRequest request = req("texto valido", "2048x1024", "anime", 42, true);

    // when
    SceneImageResponse response = sut.generateSceneImage("rc1", request);

    // then
    assertNotNull(response);
    assertEquals("b1", response.getBookId());
    assertEquals("crafted-anime", response.getCraftedPrompt());
    assertEquals("2048x1024", response.getSize());
    assertEquals("anime", response.getStyle());
    assertEquals(42, response.getSeed());
    assertEquals("b64", response.getImageBase64());
    assertNotNull(response.getCreatedAt());

    then(openAIClient)
        .should()
        .generateImage(eq("crafted-anime"), eq("2048x1024"), eq(42), eq(true));
    then(sceneImageGenerationRepository).should().save(any(SceneImageGeneration.class));
  }

  @Test
  void generateSceneImage_deberiaLanzarOpenAIServiceException_siCualquierPasoFalla() {
    // given
    given(sceneImageConfig.getMinTextLength()).willReturn(1);
    given(sceneImageConfig.getMaxTextLength()).willReturn(2000);

    ReadingClub club = ReadingClub.builder().id("rc1").bookId("b1").build();
    given(readingClubService.getReadingClubById("rc1")).willReturn(Optional.of(club));

    Book book = Book.builder().id("b1").title("Book").isbn("x").build();
    given(bookService.getBookById("b1")).willReturn(Optional.of(book));

    given(sceneImageGenerationRepository.findByReadingClubIdAndFragmentHash(eq("rc1"), anyString()))
        .willReturn(Optional.empty());

    given(promptCraftService.buildPrompt(any(), anyString(), anyString()))
        .willThrow(new RuntimeException("boom"));

    SceneImageRequest request = req("texto valido", null, null, null, null);

    // when + then
    OpenAIServiceException ex =
        assertThrows(OpenAIServiceException.class, () -> sut.generateSceneImage("rc1", request));
    assertTrue(ex.getMessage().toLowerCase().contains("failed"));
  }

  // ---------------- passthrough methods ----------------

  @Test
  void getBookSceneGenerations_deberiaDelegarEnRepo() {
    // given
    given(sceneImageGenerationRepository.findByBookIdOrderByCreatedAtDesc("b1"))
        .willReturn(List.of(SceneImageGeneration.builder().id(1L).build()));

    // when
    List<SceneImageGeneration> result = sut.getBookSceneGenerations("b1");

    // then
    assertEquals(1, result.size());
    assertEquals(1l, result.get(0).getId());
    then(sceneImageGenerationRepository).should().findByBookIdOrderByCreatedAtDesc("b1");
  }

  @Test
  void getReadingClubSceneGenerations_deberiaDelegarEnRepo() {
    // given
    given(sceneImageGenerationRepository.findByReadingClubIdOrderByCreatedAtDesc("rc1"))
        .willReturn(List.of(SceneImageGeneration.builder().id(1L).build()));

    // when
    List<SceneImageGeneration> result = sut.getReadingClubSceneGenerations("rc1");

    // then
    assertEquals(1, result.size());
    assertEquals(1L, result.get(0).getId());
    then(sceneImageGenerationRepository).should().findByReadingClubIdOrderByCreatedAtDesc("rc1");
  }

  @Test
  void getReadingClubGenerationCount_deberiaDelegarEnRepo() {
    // given
    given(sceneImageGenerationRepository.countByReadingClubId("rc1")).willReturn(5L);

    // when
    long result = sut.getReadingClubGenerationCount("rc1");

    // then
    assertEquals(5L, result);
    then(sceneImageGenerationRepository).should().countByReadingClubId("rc1");
  }

  @Test
  void getBookGenerationCount_deberiaDelegarEnRepo() {
    // given
    given(sceneImageGenerationRepository.countByBookId("b1")).willReturn(2L);

    // when
    long result = sut.getBookGenerationCount("b1");

    // then
    assertEquals(2L, result);
    then(sceneImageGenerationRepository).should().countByBookId("b1");
  }
}
