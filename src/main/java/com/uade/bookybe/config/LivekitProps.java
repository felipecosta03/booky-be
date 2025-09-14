package com.uade.bookybe.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "livekit")
@Getter
@Setter
public class LivekitProps {
  private String apiKey;
  private String apiSecret;
  private String wsUrl;
  private long tokenTtlSeconds = 600; // 10min
}
