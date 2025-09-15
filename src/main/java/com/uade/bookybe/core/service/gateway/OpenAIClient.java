package com.uade.bookybe.core.service.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uade.bookybe.config.OpenAIConfig;
import com.uade.bookybe.core.model.dto.ImageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIClient {

  @Qualifier("openaiWebClient")
  private final WebClient webClient;
  private final OpenAIConfig openAIConfig;
  private final ObjectMapper objectMapper;

  /**
   * Generate a crafted prompt using GPT for scene description
   */
  public String craftPromptWithGPT(String systemPrompt, String userPrompt) {
    log.debug("Crafting prompt with GPT using model: {}", openAIConfig.getChatModel());

    ChatRequest request = ChatRequest.builder()
        .model(openAIConfig.getChatModel())
        .messages(List.of(
            new ChatMessage("system", systemPrompt),
            new ChatMessage("user", userPrompt)
        ))
        .maxTokens(500)
        .temperature(0.7)
        .build();

    try {
      long startTime = System.currentTimeMillis();

      ChatResponse response = webClient.post()
          .uri("/chat/completions")
          .bodyValue(request)
          .retrieve()
          .bodyToMono(ChatResponse.class)
          .retryWhen(Retry.backoff(openAIConfig.getMaxRetries(), Duration.ofSeconds(1))
              .filter(this::isRetryableException))
          .timeout(openAIConfig.getTimeout())
          .block();

      long responseTime = System.currentTimeMillis() - startTime;
      log.debug("GPT prompt crafting completed in {}ms", responseTime);

      if (response != null && !response.getChoices().isEmpty()) {
        return response.getChoices().get(0).getMessage().getContent().trim();
      }

      throw new RuntimeException("Empty response from OpenAI GPT");

    } catch (WebClientResponseException e) {
      log.error("OpenAI GPT API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new RuntimeException("Failed to craft prompt with GPT: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error calling OpenAI GPT API", e);
      throw new RuntimeException("Failed to craft prompt with GPT", e);
    }
  }

  /**
   * Generate image using DALL-E
   */
  public ImageResult generateImage(String prompt, String size, Integer seed, boolean returnBase64) {
    log.debug("Generating image with DALL-E using model: {}", openAIConfig.getImageModel());

    ImageRequest.ImageRequestBuilder requestBuilder = ImageRequest.builder()
        .model(openAIConfig.getImageModel())
        .prompt(prompt)
        .n(1)
        .size(size)
        .quality("standard") // Use standard quality to reduce response size
        .style("vivid");

    // For large sizes, prefer URL over base64 to avoid buffer issues
    if (returnBase64 && isLargeSize(size)) {
      log.warn("Requested base64 for large size {}, switching to URL to avoid buffer overflow", size);
      requestBuilder.responseFormat("url");
      returnBase64 = false;
    } else if (returnBase64) {
      requestBuilder.responseFormat("b64_json");
    } else {
      requestBuilder.responseFormat("url");
    }

    ImageRequest request = requestBuilder.build();

    try {
      long startTime = System.currentTimeMillis();

      ImageResponse response = webClient.post()
          .uri("/images/generations")
          .bodyValue(request)
          .retrieve()
          .bodyToMono(ImageResponse.class)
          .retryWhen(Retry.backoff(openAIConfig.getMaxRetries(), Duration.ofSeconds(2))
              .filter(this::isRetryableException))
          .timeout(Duration.ofSeconds(90)) // Increase timeout for image generation
          .block();

      long responseTime = System.currentTimeMillis() - startTime;
      log.debug("Image generation completed in {}ms", responseTime);

      if (response != null && !response.getData().isEmpty()) {
        ImageData imageData = response.getData().get(0);
        return ImageResult.builder()
            .url(imageData.getUrl())
            .base64(returnBase64 ? imageData.getB64Json() : null)
            .revisedPrompt(imageData.getRevisedPrompt())
            .responseTimeMs(responseTime)
            .build();
      }

      throw new RuntimeException("Empty response from OpenAI Images API");

    } catch (WebClientResponseException e) {
      log.error("OpenAI Images API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new RuntimeException("Failed to generate image: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error calling OpenAI Images API", e);
      throw new RuntimeException("Failed to generate image", e);
    }
  }

  private boolean isLargeSize(String size) {
    // Consider sizes larger than 2048x1024 as large
    if (size == null) return false;
    return size.equals("4096x2048") || size.contains("4096");
  }

  private boolean isRetryableException(Throwable throwable) {
    if (throwable instanceof WebClientResponseException wcre) {
      HttpStatus status = HttpStatus.resolve(wcre.getStatusCode().value());
      return status == HttpStatus.TOO_MANY_REQUESTS ||
             status == HttpStatus.INTERNAL_SERVER_ERROR ||
             status == HttpStatus.BAD_GATEWAY ||
             status == HttpStatus.SERVICE_UNAVAILABLE ||
             status == HttpStatus.GATEWAY_TIMEOUT;
    }
    return false;
  }

  // DTOs for OpenAI API

  @lombok.Data
  @lombok.Builder
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  private static class ChatRequest {
    private String model;
    private List<ChatMessage> messages;
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    private Double temperature;
  }

  @lombok.Data
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  private static class ChatMessage {
    private String role;
    private String content;
  }

  @lombok.Data
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  private static class ChatResponse {
    private List<Choice> choices;
    private Usage usage;

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class Choice {
      private ChatMessage message;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class Usage {
      @JsonProperty("prompt_tokens")
      private Integer promptTokens;
      @JsonProperty("completion_tokens")
      private Integer completionTokens;
      @JsonProperty("total_tokens")
      private Integer totalTokens;
    }
  }

  @lombok.Data
  @lombok.Builder
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  private static class ImageRequest {
    private String model;
    private String prompt;
    private Integer n;
    private String size;
    private String quality;
    private String style;
    @JsonProperty("response_format")
    private String responseFormat;
  }

  @lombok.Data
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  private static class ImageResponse {
    private List<ImageData> data;
  }

  @lombok.Data
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  private static class ImageData {
    private String url;
    @JsonProperty("b64_json")
    private String b64Json;
    @JsonProperty("revised_prompt")
    private String revisedPrompt;
  }
}
