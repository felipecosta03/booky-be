package com.uade.bookybe.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageResult {

  private String url;
  private String base64;
  private String revisedPrompt;
  private Long responseTimeMs;
  private Integer promptTokens;
  private Double costUsd;
}
