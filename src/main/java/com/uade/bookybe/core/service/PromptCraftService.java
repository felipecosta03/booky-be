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

  private static final String SYSTEM_PROMPT = """
      Eres un experto en dirección de arte para generación de imágenes 360 para VR. 
      Escribe un único párrafo descriptivo detallado del escenario, 360° equirectangular (2:1), 
      apto para DALL·E/Images. Mantén coherencia con época, género y tono del libro. 
      Describe ambiente, materiales, iluminación, clima, perspectiva, distancia focal, 
      nivel de detalle y atmósfera. Evita mencionar texto sobreimpreso, evita nombres 
      protegidos y evita instrucciones de cámara meta. Cierra con un breve bloque de 
      "rendering hints": "equirectangular 2:1, seamless edges, high detail, 8k if possible".
      """;

  /**
   * Build a comprehensive prompt for 360° image generation
   */
  public String buildPrompt(Book book, String text, String style) {
    log.debug("Building prompt for book: {} with style: {}", book.getTitle(), style);

    String userPrompt = buildUserPrompt(book, text, style);

    try {
      String craftedPrompt = openAIClient.craftPromptWithGPT(SYSTEM_PROMPT, userPrompt);
      log.debug("Successfully crafted prompt of {} characters", craftedPrompt.length());
      return craftedPrompt;
    } catch (Exception e) {
      log.error("Failed to craft prompt with GPT, falling back to template", e);
      return buildFallbackPrompt(book, text, style);
    }
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

    prompt.append("\nDevuélveme SOLO el prompt final para generar la imagen 360°, sin encabezados ni comillas.");

    return prompt.toString();
  }

  private String buildFallbackPrompt(Book book, String text, String style) {
    log.info("Using fallback prompt generation for book: {}", book.getTitle());

    StringBuilder prompt = new StringBuilder();

    // Extract key elements from the text
    String environment = extractEnvironmentFromText(text);
    String timeOfDay = extractTimeOfDayFromText(text);
    String mood = extractMoodFromText(text);

    // Build base prompt
    prompt.append("360° equirectangular panorama, 2:1 aspect ratio, ");

    // Add environment description
    if (!environment.isEmpty()) {
      prompt.append(environment).append(", ");
    }

    // Add time and lighting
    if (!timeOfDay.isEmpty()) {
      prompt.append(timeOfDay).append(" lighting, ");
    }

    // Add mood and atmosphere
    if (!mood.isEmpty()) {
      prompt.append(mood).append(" atmosphere, ");
    }

    // Add style
    String finalStyle = (style != null && !style.trim().isEmpty()) ? style : "photorealistic";
    prompt.append(finalStyle).append(" style, ");

    // Add genre-specific elements based on book categories
    if (book.getCategories() != null) {
      for (String category : book.getCategories()) {
        switch (category.toLowerCase()) {
          case "fantasy", "fantasía" -> prompt.append("magical elements, mystical ambiance, ");
          case "mystery", "misterio" -> prompt.append("shadowy corners, mysterious atmosphere, ");
          case "romance", "romántica" -> prompt.append("warm lighting, intimate setting, ");
          case "horror", "terror" -> prompt.append("ominous shadows, eerie atmosphere, ");
          case "science fiction", "ciencia ficción" -> prompt.append("futuristic elements, technological details, ");
        }
      }
    }

    // Add technical specifications
    prompt.append("seamless edges, high detail, 8k if possible, ");
    prompt.append("camera at human eye level 1.6m, consistent lighting and shadows, ");
    prompt.append("suitable for VR viewers, no text overlays, no copyrighted characters");

    return prompt.toString();
  }

  private String extractEnvironmentFromText(String text) {
    String lowerText = text.toLowerCase();

    if (lowerText.contains("biblioteca") || lowerText.contains("library")) {
      return "ancient library interior with wooden shelves";
    } else if (lowerText.contains("bosque") || lowerText.contains("forest")) {
      return "dense forest environment";
    } else if (lowerText.contains("castillo") || lowerText.contains("castle")) {
      return "medieval castle interior";
    } else if (lowerText.contains("ciudad") || lowerText.contains("city")) {
      return "urban cityscape";
    } else if (lowerText.contains("playa") || lowerText.contains("beach")) {
      return "coastal beach environment";
    } else if (lowerText.contains("montaña") || lowerText.contains("mountain")) {
      return "mountainous landscape";
    } else if (lowerText.contains("interior") || lowerText.contains("habitación") || lowerText.contains("room")) {
      return "indoor interior space";
    } else {
      return "atmospheric environment";
    }
  }

  private String extractTimeOfDayFromText(String text) {
    String lowerText = text.toLowerCase();

    if (lowerText.contains("amanecer") || lowerText.contains("dawn") || lowerText.contains("sunrise")) {
      return "golden dawn";
    } else if (lowerText.contains("atardecer") || lowerText.contains("sunset") || lowerText.contains("dusk")) {
      return "warm sunset";
    } else if (lowerText.contains("noche") || lowerText.contains("night") || lowerText.contains("nocturno")) {
      return "nighttime";
    } else if (lowerText.contains("mediodía") || lowerText.contains("noon")) {
      return "bright midday";
    } else if (lowerText.contains("mañana") || lowerText.contains("morning")) {
      return "soft morning";
    } else {
      return "natural";
    }
  }

  private String extractMoodFromText(String text) {
    String lowerText = text.toLowerCase();

    if (lowerText.contains("misterio") || lowerText.contains("mystery") || lowerText.contains("sombra")) {
      return "mysterious";
    } else if (lowerText.contains("paz") || lowerText.contains("tranquil") || lowerText.contains("sereno")) {
      return "peaceful";
    } else if (lowerText.contains("peligro") || lowerText.contains("danger") || lowerText.contains("amenaza")) {
      return "ominous";
    } else if (lowerText.contains("mágico") || lowerText.contains("magic") || lowerText.contains("encanto")) {
      return "enchanted";
    } else if (lowerText.contains("romántico") || lowerText.contains("romantic") || lowerText.contains("amor")) {
      return "romantic";
    } else {
      return "atmospheric";
    }
  }

  private String truncateText(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength) + "...";
  }
}
