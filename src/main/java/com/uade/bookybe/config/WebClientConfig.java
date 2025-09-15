package com.uade.bookybe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

  @Bean("openaiWebClient")
  public WebClient openaiWebClient(OpenAIConfig openAIConfig) {
    // Increase buffer size for large image responses (10MB)
    ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
        .build();

    // Configure HTTP client with connection pooling and timeouts
    HttpClient httpClient = HttpClient.create()
        .responseTimeout(openAIConfig.getTimeout())
        .compress(true);

    return WebClient.builder()
        .baseUrl(openAIConfig.getBaseUrl())
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .exchangeStrategies(strategies)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAIConfig.getApiKey())
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.USER_AGENT, "BookyBE/1.0")
        .build();
  }
}
