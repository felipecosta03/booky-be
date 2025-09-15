package com.uade.bookybe.core.service;

import com.uade.bookybe.core.model.Book;
import com.uade.bookybe.core.service.gateway.OpenAIClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptCraftServiceTest {

  @Mock
  private OpenAIClient openAIClient;

  @InjectMocks
  private PromptCraftService promptCraftService;

  private Book testBook;

  @BeforeEach
  void setUp() {
    testBook = Book.builder()
        .id("123")
        .title("El Hobbit")
        .author("J.R.R. Tolkien")
        .synopsis("Un hobbit emprende una aventura épica")
        .categories(List.of("Fantasy", "Adventure"))
        .build();
  }

  @Test
  void buildPrompt_WithValidInputs_ShouldReturnCraftedPrompt() {
    // Given
    String text = "Bilbo se encuentra en una cueva oscura con un anillo brillante en el suelo";
    String style = "photorealistic";
    String expectedPrompt = "360° equirectangular panorama of a dark cave with glowing ring, mystical atmosphere, photorealistic style, seamless edges, high detail, 8k if possible";

    when(openAIClient.craftPromptWithGPT(any(String.class), any(String.class)))
        .thenReturn(expectedPrompt);

    // When
    String result = promptCraftService.buildPrompt(testBook, text, style);

    // Then
    assertNotNull(result);
    assertEquals(expectedPrompt, result);
    verify(openAIClient).craftPromptWithGPT(any(String.class), any(String.class));
  }

  @Test
  void buildPrompt_WhenOpenAIFails_ShouldReturnFallbackPrompt() {
    // Given
    String text = "Una biblioteca antigua con estanterías de roble";
    String style = "photorealistic";

    when(openAIClient.craftPromptWithGPT(any(String.class), any(String.class)))
        .thenThrow(new RuntimeException("OpenAI service unavailable"));

    // When
    String result = promptCraftService.buildPrompt(testBook, text, style);

    // Then
    assertNotNull(result);
    assertTrue(result.contains("360° equirectangular panorama"));
    assertTrue(result.contains("photorealistic style"));
    assertTrue(result.contains("seamless edges"));
    verify(openAIClient).craftPromptWithGPT(any(String.class), any(String.class));
  }

  @Test
  void buildPrompt_WithNullStyle_ShouldUsePhotorealistic() {
    // Given
    String text = "Un castillo medieval en las montañas";
    String expectedPrompt = "Medieval castle panorama with photorealistic rendering";

    when(openAIClient.craftPromptWithGPT(any(String.class), any(String.class)))
        .thenReturn(expectedPrompt);

    // When
    String result = promptCraftService.buildPrompt(testBook, text, null);

    // Then
    assertNotNull(result);
    assertEquals(expectedPrompt, result);
  }

  @Test
  void buildPrompt_WithFantasyBook_ShouldIncludeMagicalElements() {
    // Given
    String text = "Un bosque encantado con luces misteriosas";

    when(openAIClient.craftPromptWithGPT(any(String.class), any(String.class)))
        .thenThrow(new RuntimeException("Use fallback"));

    // When
    String result = promptCraftService.buildPrompt(testBook, text, "artistic");

    // Then
    assertNotNull(result);
    assertTrue(result.contains("magical elements"));
    assertTrue(result.contains("mystical ambiance"));
  }
}
