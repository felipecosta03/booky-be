package com.uade.bookybe.core.service;

import com.uade.bookybe.config.SceneImageConfig;
import com.uade.bookybe.core.exception.BookNotFoundException;
import com.uade.bookybe.core.exception.InvalidImageSizeException;
import com.uade.bookybe.core.exception.OpenAIServiceException;
import com.uade.bookybe.core.model.Book;
import com.uade.bookybe.core.model.SceneImageGeneration;
import com.uade.bookybe.core.model.dto.ImageResult;
import com.uade.bookybe.core.model.dto.SceneImageRequest;
import com.uade.bookybe.core.model.dto.SceneImageResponse;
import com.uade.bookybe.core.port.SceneImageGenerationRepository;
import com.uade.bookybe.core.service.gateway.OpenAIClient;
import com.uade.bookybe.core.usecase.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SceneImageService {

  private final BookService bookService;
  private final SceneImageGenerationRepository sceneImageGenerationRepository;
  private final PromptCraftService promptCraftService;
  private final OpenAIClient openAIClient;
  private final SceneImageConfig sceneImageConfig;

  private static final List<String> VALID_SIZES = Arrays.asList(
      "1024x512", "2048x1024", "4096x2048"
  );

  private static final Pattern SIZE_PATTERN = Pattern.compile("^(\\d+)x(\\d+)$");

  @Transactional
  public SceneImageResponse generateSceneImage(String bookId, SceneImageRequest request) {
    log.info("Generating scene image for book: {} with text length: {}", bookId, request.getText().length());

    // Validate request
    validateRequest(request);

    // Get book using existing BookService
    Book book = bookService.getBookById(bookId)
        .orElseThrow(() -> new BookNotFoundException(bookId));

    // Generate fragment hash for caching/deduplication
    String fragmentHash = generateFragmentHash(request.getText());

    // Check if we already have this generation (optional caching)
    Optional<SceneImageGeneration> existingGeneration =
        sceneImageGenerationRepository.findByBookIdAndFragmentHash(bookId, fragmentHash);

    if (existingGeneration.isPresent()) {
      log.info("Found existing generation for book: {} and fragment hash: {}", bookId, fragmentHash);
      return mapToResponse(existingGeneration.get());
    }

    try {
      // Generate the crafted prompt
      String style = request.getStyle() != null ? request.getStyle() : "photorealistic";
      String craftedPrompt = promptCraftService.buildPrompt(book, request.getText(), style);

      // Generate the image - prefer URL for large sizes to avoid buffer issues
      String size = request.getSize() != null ? request.getSize() : sceneImageConfig.getDefaultSize();
      boolean returnBase64 = Boolean.TRUE.equals(request.getReturnBase64());

      // For large images, force URL response to prevent buffer overflow
      if (returnBase64 && isLargeImageSize(size)) {
        log.info("Large image size {} requested with base64, switching to URL to prevent buffer overflow", size);
        returnBase64 = false;
      }

      ImageResult imageResult = openAIClient.generateImage(craftedPrompt, size, request.getSeed(), returnBase64);

      // Save the generation record
      SceneImageGeneration generation = SceneImageGeneration.builder()
          .bookId(bookId)
          .fragmentHash(fragmentHash)
          .craftedPrompt(craftedPrompt)
          .imageUrl(imageResult.getUrl())
          .imageBase64(imageResult.getBase64())
          .size(size)
          .style(style)
          .seed(request.getSeed())
          .createdAt(LocalDateTime.now())
          .openaiResponseTimeMs(imageResult.getResponseTimeMs())
          .promptTokens(imageResult.getPromptTokens())
          .totalCostUsd(imageResult.getCostUsd())
          .build();

      generation = sceneImageGenerationRepository.save(generation);

      log.info("Successfully generated scene image for book: {} in {}ms",
          bookId, imageResult.getResponseTimeMs());

      return mapToResponse(generation);

    } catch (Exception e) {
      log.error("Error generating scene image for book: " + bookId, e);
      throw new OpenAIServiceException("Failed to generate scene image: " + e.getMessage(), e);
    }
  }

  private boolean isLargeImageSize(String size) {
    return size != null && (size.equals("4096x2048") || size.contains("4096"));
  }

  private void validateRequest(SceneImageRequest request) {
    // Text validation
    if (request.getText() == null || request.getText().trim().isEmpty()) {
      throw new IllegalArgumentException("Text cannot be blank");
    }

    String text = request.getText().trim();
    if (text.length() < sceneImageConfig.getMinTextLength()) {
      throw new IllegalArgumentException(
          String.format("Text must be at least %d characters long", sceneImageConfig.getMinTextLength()));
    }

    if (text.length() > sceneImageConfig.getMaxTextLength()) {
      throw new IllegalArgumentException(
          String.format("Text must not exceed %d characters", sceneImageConfig.getMaxTextLength()));
    }

    // Size validation
    if (request.getSize() != null) {
      validateImageSize(request.getSize());
    }
  }

  private void validateImageSize(String size) {
    if (!VALID_SIZES.contains(size)) {
      throw new InvalidImageSizeException(
          "Invalid size. Supported sizes for 360° images: " + String.join(", ", VALID_SIZES));
    }

    // Validate 2:1 aspect ratio for 360° images
    if (!isValidAspectRatio(size)) {
      throw new InvalidImageSizeException("Size must have 2:1 aspect ratio for 360° equirectangular images");
    }
  }

  private boolean isValidAspectRatio(String size) {
    var matcher = SIZE_PATTERN.matcher(size);
    if (matcher.matches()) {
      int width = Integer.parseInt(matcher.group(1));
      int height = Integer.parseInt(matcher.group(2));
      double ratio = (double) width / height;
      return Math.abs(ratio - 2.0) < 0.01; // Allow small floating point errors
    }
    return false;
  }

  private String generateFragmentHash(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(text.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString().substring(0, 16); // First 16 characters
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  private SceneImageResponse mapToResponse(SceneImageGeneration generation) {
    return SceneImageResponse.builder()
        .bookId(generation.getBookId())
        .craftedPrompt(generation.getCraftedPrompt())
        .imageUrl(generation.getImageUrl())
        .imageBase64(generation.getImageBase64())
        .size(generation.getSize())
        .style(generation.getStyle())
        .seed(generation.getSeed())
        .createdAt(generation.getCreatedAt())
        .build();
  }

  /**
   * Get all scene generations for a specific book
   */
  public List<SceneImageGeneration> getBookSceneGenerations(String bookId) {
    return sceneImageGenerationRepository.findByBookIdOrderByCreatedAtDesc(bookId);
  }

  /**
   * Get generation statistics for a book
   */
  public long getBookGenerationCount(String bookId) {
    return sceneImageGenerationRepository.countByBookId(bookId);
  }
}
