package com.uade.bookybe.core.service.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uade.bookybe.config.OpenAIConfig;
import com.uade.bookybe.core.model.dto.ImageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

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
   * Generate image using OpenAI Images API
   */
  public ImageResult generateImage(String prompt, String size, Integer seed, boolean returnBase64) {
    log.debug("Generating image with OpenAI using model: {}", openAIConfig.getImageModel());

    ImageRequest request = ImageRequest.builder()
        .model(openAIConfig.getImageModel())
        .prompt(prompt)
        .n(1)
        .size(size)
        .build();
    Duration imageTimeout = imageTimeout();
    int promptLength = promptLength(prompt);

    try {
      long startTime = System.currentTimeMillis();
      log.info(
          "Calling OpenAI Images API: model={}, size={}, promptLength={}, timeout={}, maxRetries={}, seedPresent={}, returnBase64Requested={}",
          request.getModel(), request.getSize(), promptLength, imageTimeout, openAIConfig.getMaxRetries(),
          seed != null, returnBase64);

      ImageResponse response = webClient.post()
          .uri("/images/generations")
          .bodyValue(request)
          .retrieve()
          .onStatus(HttpStatusCode::isError, clientResponse -> logImageErrorResponse(clientResponse, request))
          .bodyToMono(ImageResponse.class)
          .retryWhen(Retry.backoff(openAIConfig.getMaxRetries(), Duration.ofSeconds(2))
              .filter(this::isRetryableException))
          .timeout(imageTimeout)
          .block();

      long responseTime = System.currentTimeMillis() - startTime;
      log.info("OpenAI Images API completed in {}ms: model={}, size={}", responseTime, request.getModel(), request.getSize());

      if (response != null && !response.getData().isEmpty()) {
        ImageData imageData = response.getData().get(0);
        log.debug(
            "OpenAI Images API response parsed: model={}, size={}, hasUrl={}, hasBase64={}, hasRevisedPrompt={}",
            request.getModel(), request.getSize(), imageData.getUrl() != null, imageData.getB64Json() != null,
            imageData.getRevisedPrompt() != null);
        return ImageResult.builder()
            .url(imageData.getUrl())
            .base64(imageData.getB64Json())
            .revisedPrompt(imageData.getRevisedPrompt())
            .responseTimeMs(responseTime)
            .build();
      }

      log.error("OpenAI Images API returned empty response: model={}, size={}, promptLength={}",
          request.getModel(), request.getSize(), promptLength);
      throw new RuntimeException("Empty response from OpenAI Images API");

    } catch (WebClientResponseException e) {
      log.error(
          "OpenAI Images API failed after retries: status={}, model={}, size={}, promptLength={}, responseBody={}",
          e.getStatusCode(), request.getModel(), request.getSize(), promptLength,
          truncateForLog(e.getResponseBodyAsString()), e);
      throw new RuntimeException("Failed to generate image: " + e.getMessage(), e);
    } catch (Exception e) {
      Throwable rootCause = rootCause(e);
      if (hasTimeoutCause(e)) {
        log.error(
            "OpenAI Images API timed out: model={}, size={}, promptLength={}, configuredTimeout={}, rootCause={}: {}",
            request.getModel(), request.getSize(), promptLength, imageTimeout, rootCause.getClass().getName(),
            rootCause.getMessage(), e);
      } else {
        log.error(
            "Unexpected error calling OpenAI Images API: model={}, size={}, promptLength={}, rootCause={}: {}",
            request.getModel(), request.getSize(), promptLength, rootCause.getClass().getName(),
            rootCause.getMessage(), e);
      }
      throw new RuntimeException("Failed to generate image", e);
    }
  }

  private Mono<? extends Throwable> logImageErrorResponse(ClientResponse response, ImageRequest request) {
    return response.bodyToMono(String.class)
        .defaultIfEmpty("")
        .map(responseBody -> {
          HttpStatusCode status = response.statusCode();
          log.error(
              "OpenAI Images API returned non-2xx response: status={}, model={}, size={}, promptLength={}, responseBody={}",
              status, request.getModel(), request.getSize(), promptLength(request.getPrompt()),
              truncateForLog(responseBody));
          return WebClientResponseException.create(
              status.value(),
              status.toString(),
              response.headers().asHttpHeaders(),
              responseBody.getBytes(StandardCharsets.UTF_8),
              StandardCharsets.UTF_8);
        });
  }

  private Duration imageTimeout() {
    Duration configuredTimeout = openAIConfig.getImageTimeout();
    return configuredTimeout != null ? configuredTimeout : Duration.ofSeconds(180);
  }

  private int promptLength(String prompt) {
    return prompt == null ? 0 : prompt.length();
  }

  private String truncateForLog(String value) {
    if (value == null || value.isBlank()) {
      return "<empty>";
    }

    int maxLength = 4000;
    if (value.length() <= maxLength) {
      return value;
    }

    return value.substring(0, maxLength) + "... [truncated]";
  }

  private boolean hasTimeoutCause(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof TimeoutException || current.getClass().getName().contains("ReadTimeoutException")) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private Throwable rootCause(Throwable throwable) {
    Throwable root = throwable;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    return root;
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
