package com.uade.bookybe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "scene-image")
@Data
public class SceneImageConfig {

  private String defaultSize = "1024x1024";
  private int maxTextLength = 2000;
  private int minTextLength = 15;
  private RateLimit rateLimit = new RateLimit();

  @Data
  public static class RateLimit {
    private int requestsPerMinute = 10;
  }
}
