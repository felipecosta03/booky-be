package com.uade.bookybe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "openai")
@Data
public class OpenAIConfig {

  private String apiKey;
  private String chatModel = "gpt-4o";
  private String imageModel = "gpt-image-1"; // dall-e-3, gpt-image-1
  private String baseUrl = "https://api.openai.com/v1";
  private Duration timeout = Duration.ofSeconds(30);
  private int maxRetries = 3;
}
