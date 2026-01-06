package com.uade.bookybe.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.uade.bookybe.core.model.Book;
import com.uade.bookybe.core.service.gateway.OpenAIClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptCraftServiceTest {

  @Mock private OpenAIClient openAIClient;

  @InjectMocks private PromptCraftService sut;

  @Captor private ArgumentCaptor<String> systemPromptCaptor;

  @Captor private ArgumentCaptor<String> userPromptCaptor;

  @Test
  void
      buildPrompt_deberiaDelegarEnOpenAIClient_conSystemPromptYUserPrompt_yRetornarCraftedPrompt() {
    // given
    Book book =
        Book.builder()
            .id("b1")
            .title("El Hobbit")
            .author("J.R.R. Tolkien")
            .categories(List.of("Fantasy", "Adventure"))
            .synopsis("Una aventura épica con enanos, un mago y un hobbit.")
            .build();

    String fragment = "En un agujero en el suelo, vivía un hobbit.";
    String style = "photorealistic";

    given(openAIClient.craftPromptWithGPT(anyString(), anyString())).willReturn("CRAFTED_PROMPT");

    // when
    String result = sut.buildPrompt(book, fragment, style);

    // then
    assertEquals("CRAFTED_PROMPT", result);

    then(openAIClient)
        .should()
        .craftPromptWithGPT(systemPromptCaptor.capture(), userPromptCaptor.capture());

    String systemPrompt = systemPromptCaptor.getValue();
    String userPrompt = userPromptCaptor.getValue();

    // System prompt checks (sanity)
    assertNotNull(systemPrompt);
    assertTrue(systemPrompt.contains("360°"));
    assertTrue(systemPrompt.toLowerCase().contains("equirectangular"));

    // User prompt checks (content)
    assertNotNull(userPrompt);
    assertTrue(userPrompt.contains("Metadatos del libro:"));
    assertTrue(userPrompt.contains("- Título: El Hobbit"));
    assertTrue(userPrompt.contains("- Autor: J.R.R. Tolkien"));
    assertTrue(userPrompt.contains("- Géneros: Fantasy, Adventure"));

    assertTrue(userPrompt.contains("Fragmento narrado:"));
    assertTrue(userPrompt.contains("\"" + fragment + "\""));

    assertTrue(userPrompt.contains("Estilo preferido: " + style));
    assertTrue(userPrompt.contains("Devuélveme SOLO el prompt final"));
  }

  @Test
  void buildPrompt_noDeberiaIncluirGeneros_siCategoriesNullOVacia() {
    // given
    Book bookNull = Book.builder().id("b1").title("Libro").author("Autor").categories(null).build();

    Book bookEmpty =
        Book.builder().id("b2").title("Libro2").author("Autor2").categories(List.of()).build();

    given(openAIClient.craftPromptWithGPT(anyString(), anyString())).willReturn("OK");

    // when
    sut.buildPrompt(bookNull, "frag", "style");
    then(openAIClient).should().craftPromptWithGPT(anyString(), userPromptCaptor.capture());
    String prompt1 = userPromptCaptor.getValue();

    // reset interactions to capture again
    clearInvocations(openAIClient);
    given(openAIClient.craftPromptWithGPT(anyString(), anyString())).willReturn("OK");

    sut.buildPrompt(bookEmpty, "frag", "style");
    then(openAIClient).should().craftPromptWithGPT(anyString(), userPromptCaptor.capture());
    String prompt2 = userPromptCaptor.getValue();

    // then
    assertFalse(prompt1.contains("- Géneros:"));
    assertFalse(prompt2.contains("- Géneros:"));
  }

  @Test
  void buildPrompt_noDeberiaIncluirSinopsis_siEsNullOVacia() {
    // given
    Book bookNullSynopsis =
        Book.builder().id("b1").title("Libro").author("Autor").synopsis(null).build();

    Book bookBlankSynopsis =
        Book.builder().id("b2").title("Libro2").author("Autor2").synopsis("   ").build();

    given(openAIClient.craftPromptWithGPT(anyString(), anyString())).willReturn("OK");

    // when
    sut.buildPrompt(bookNullSynopsis, "frag", "style");
    then(openAIClient).should().craftPromptWithGPT(anyString(), userPromptCaptor.capture());
    String prompt1 = userPromptCaptor.getValue();

    clearInvocations(openAIClient);
    given(openAIClient.craftPromptWithGPT(anyString(), anyString())).willReturn("OK");

    sut.buildPrompt(bookBlankSynopsis, "frag", "style");
    then(openAIClient).should().craftPromptWithGPT(anyString(), userPromptCaptor.capture());
    String prompt2 = userPromptCaptor.getValue();

    // then
    assertFalse(prompt1.contains("- Sinopsis:"));
    assertFalse(prompt2.contains("- Sinopsis:"));
  }

  @Test
  void buildPrompt_deberiaTruncarSinopsis_a200CaracteresMasPuntosSuspensivos() {
    // given
    String longSynopsis = "a".repeat(250);

    Book book =
        Book.builder().id("b1").title("Libro").author("Autor").synopsis(longSynopsis).build();

    given(openAIClient.craftPromptWithGPT(anyString(), anyString())).willReturn("OK");

    // when
    sut.buildPrompt(book, "frag", "style");

    // then
    then(openAIClient).should().craftPromptWithGPT(anyString(), userPromptCaptor.capture());
    String userPrompt = userPromptCaptor.getValue();

    assertTrue(userPrompt.contains("- Sinopsis: "));
    // 200 chars + "..."
    String expectedSnippet = "a".repeat(200) + "...";
    assertTrue(userPrompt.contains(expectedSnippet));
    assertFalse(userPrompt.contains("a".repeat(201) + "...")); // sanity
  }

  @Test
  void buildPrompt_noDeberiaIncluirEstilo_siStyleNullOVacioOBlanco() {
    // given
    Book book = Book.builder().id("b1").title("Libro").author("Autor").build();

    given(openAIClient.craftPromptWithGPT(anyString(), anyString())).willReturn("OK");

    // when
    sut.buildPrompt(book, "frag", null);
    then(openAIClient).should().craftPromptWithGPT(anyString(), userPromptCaptor.capture());
    String p1 = userPromptCaptor.getValue();

    clearInvocations(openAIClient);
    given(openAIClient.craftPromptWithGPT(anyString(), anyString())).willReturn("OK");

    sut.buildPrompt(book, "frag", "");
    then(openAIClient).should().craftPromptWithGPT(anyString(), userPromptCaptor.capture());
    String p2 = userPromptCaptor.getValue();

    clearInvocations(openAIClient);
    given(openAIClient.craftPromptWithGPT(anyString(), anyString())).willReturn("OK");

    sut.buildPrompt(book, "frag", "   ");
    then(openAIClient).should().craftPromptWithGPT(anyString(), userPromptCaptor.capture());
    String p3 = userPromptCaptor.getValue();

    // then
    assertFalse(p1.contains("Estilo preferido:"));
    assertFalse(p2.contains("Estilo preferido:"));
    assertFalse(p3.contains("Estilo preferido:"));
  }
}
