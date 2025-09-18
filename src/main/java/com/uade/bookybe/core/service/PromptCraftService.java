package com.uade.bookybe.core.service;

import com.uade.bookybe.core.model.Book;
import com.uade.bookybe.core.service.gateway.OpenAIClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptCraftService {

  private final OpenAIClient openAIClient;

  private static final String SYSTEM_PROMPT =
"""
Eres un experto en dirección de arte para la generación de imágenes inmersivas 360° en formato equirectangular (proporción 2:1) destinadas a experiencias VR.
Escribe un único párrafo descriptivo y detallado del escenario que refleje la narrativa del libro, manteniendo coherencia con la época, género y tono de la historia.
Incluye detalles sobre el entorno, materiales, colores, iluminación, clima, profundidad, perspectiva y nivel de detalle atmosférico.
Evita referencias a texto sobreimpreso, marcas registradas o instrucciones de cámara explícitas.
Al final agrega un bloque breve con sugerencias técnicas para el renderizado: "360 panorama, equirectangular 2:1, seamless edges, immersive VR, ultra high detail, 8k if possible".
""";

  /** Build a comprehensive prompt for 360° image generation */
  public String buildPrompt(Book book, String text, String style) {
    log.debug("Building prompt for book: {} with style: {}", book.getTitle(), style);

    String userPrompt = buildUserPrompt(book, text, style);

    String craftedPrompt = openAIClient.craftPromptWithGPT(SYSTEM_PROMPT, userPrompt);
    log.debug("Successfully crafted prompt of {} characters", craftedPrompt.length());
    return craftedPrompt;
  }

  private String buildUserPrompt(Book book, String text, String style) {
    StringBuilder prompt = new StringBuilder();

    // Book metadata
    prompt.append("Metadatos del libro:\n");
    prompt.append("- Título: ").append(book.getTitle()).append("\n");
    prompt.append("- Autor: ").append(book.getAuthor()).append("\n");

    if (book.getCategories() != null && !book.getCategories().isEmpty()) {
      prompt.append("- Géneros: ").append(String.join(", ", book.getCategories())).append("\n");
    }

    if (book.getSynopsis() != null && !book.getSynopsis().trim().isEmpty()) {
      prompt.append("- Sinopsis: ").append(truncateText(book.getSynopsis(), 200)).append("\n");
    }

    // Fragment text
    prompt.append("\nFragmento narrado:\n");
    prompt.append("\"").append(text).append("\"\n");

    // Style preferences
    if (style != null && !style.trim().isEmpty()) {
      prompt.append("\nEstilo preferido: ").append(style).append("\n");
    }

    prompt.append(
        "\nDevuélveme SOLO el prompt final para generar la imagen 360°, sin encabezados ni comillas.");

    return prompt.toString();
  }

  private String truncateText(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength) + "...";
  }
}
