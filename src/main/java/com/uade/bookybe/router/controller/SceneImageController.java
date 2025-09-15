package com.uade.bookybe.router.controller;

import com.uade.bookybe.core.exception.BookNotFoundException;
import com.uade.bookybe.core.exception.InvalidImageSizeException;
import com.uade.bookybe.core.exception.OpenAIServiceException;
import com.uade.bookybe.core.model.dto.SceneImageRequest;
import com.uade.bookybe.core.model.dto.SceneImageResponse;
import com.uade.bookybe.core.service.SceneImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Scene Image Generation", description = "Generate 360° VR scene images from book text fragments")
public class SceneImageController {

  private final SceneImageService sceneImageService;

  // Simple in-memory rate limiting (in production, use Redis or similar)
  private final ConcurrentHashMap<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();

  // Rate limiting configuration
  private static final int MAX_REQUESTS_PER_MINUTE = 10;

  @PostMapping("/{bookId}/scene-image")
  @Operation(
      summary = "Generate 360° scene image from book text fragment",
      description = "Creates a 360° equirectangular VR image based on a text fragment from the specified book. " +
                   "The system uses the book's metadata (title, author, genre) combined with the text fragment " +
                   "to generate a detailed scene description prompt via GPT, then creates the image using DALL-E."
  )
  @ApiResponse(responseCode = "200", description = "Scene image generated successfully")
  @ApiResponse(responseCode = "400", description = "Invalid request parameters")
  @ApiResponse(responseCode = "404", description = "Book not found")
  @ApiResponse(responseCode = "422", description = "Invalid image size format")
  @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
  @ApiResponse(responseCode = "503", description = "OpenAI service unavailable")
  public ResponseEntity<SceneImageResponse> generateSceneImage(
      @Parameter(description = "Book ID", required = true)
      @PathVariable String bookId,

      @Parameter(description = "Scene generation request", required = true)
      @Valid @RequestBody SceneImageRequest request,

      HttpServletRequest httpRequest) {

    try {
      // Apply rate limiting
      if (!checkRateLimit(getClientIdentifier(httpRequest))) {
        log.warn("Rate limit exceeded for client: {}", getClientIdentifier(httpRequest));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
      }

      log.info("Generating scene image for book: {} with text length: {}",
          bookId, request.getText().length());

      SceneImageResponse response = sceneImageService.generateSceneImage(bookId, request);

      log.info("Successfully generated scene image for book: {}", bookId);
      return ResponseEntity.ok(response);

    } catch (IllegalArgumentException e) {
      log.warn("Invalid request for book {}: {}", bookId, e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (BookNotFoundException e) {
      log.warn("Book not found: {}", bookId);
      return ResponseEntity.notFound().build();
    } catch (InvalidImageSizeException e) {
      log.warn("Invalid size format for book {}: {}", bookId, e.getMessage());
      return ResponseEntity.unprocessableEntity().build();
    } catch (OpenAIServiceException e) {
      log.error("OpenAI service error for book {}: {}", bookId, e.getMessage());
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    } catch (Exception e) {
      log.error("Unexpected error generating scene image for book: " + bookId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/{bookId}/scene-generations")
  @Operation(
      summary = "Get all scene generations for a book",
      description = "Returns all previous scene image generations for the specified book"
  )
  public ResponseEntity<List<Object>> getBookSceneGenerations(@PathVariable String bookId) {
    try {
      var generations = sceneImageService.getBookSceneGenerations(bookId);
      return ResponseEntity.ok(List.of(generations.toArray()));
    } catch (Exception e) {
      log.error("Error retrieving scene generations for book: " + bookId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/{bookId}/scene-generations/count")
  @Operation(
      summary = "Get scene generation count for a book",
      description = "Returns the total number of scene generations created for the specified book"
  )
  public ResponseEntity<Long> getBookGenerationCount(@PathVariable String bookId) {
    try {
      long count = sceneImageService.getBookGenerationCount(bookId);
      return ResponseEntity.ok(count);
    } catch (Exception e) {
      log.error("Error retrieving generation count for book: " + bookId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  private boolean checkRateLimit(String clientId) {
    LocalDateTime now = LocalDateTime.now();
    RateLimitInfo rateLimitInfo = rateLimitMap.computeIfAbsent(clientId, k -> new RateLimitInfo());

    // Clean old requests (older than 1 minute)
    rateLimitInfo.cleanOldRequests(now);

    // Check if under limit
    if (rateLimitInfo.getRequestCount() >= MAX_REQUESTS_PER_MINUTE) {
      return false;
    }

    // Add current request
    rateLimitInfo.addRequest(now);
    return true;
  }

  private String getClientIdentifier(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  // Simple rate limit tracking class
  private static class RateLimitInfo {
    private final ConcurrentHashMap<LocalDateTime, Integer> requests = new ConcurrentHashMap<>();

    public void addRequest(LocalDateTime timestamp) {
      requests.merge(timestamp.truncatedTo(ChronoUnit.SECONDS), 1, Integer::sum);
    }

    public int getRequestCount() {
      return requests.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void cleanOldRequests(LocalDateTime now) {
      LocalDateTime oneMinuteAgo = now.minusMinutes(1);
      requests.entrySet().removeIf(entry -> entry.getKey().isBefore(oneMinuteAgo));
    }
  }
}
