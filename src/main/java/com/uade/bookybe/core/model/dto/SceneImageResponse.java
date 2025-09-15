package com.uade.bookybe.core.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SceneImageResponse {

  @JsonProperty("book_id")
  private String bookId;

  @JsonProperty("crafted_prompt")
  private String craftedPrompt;

  @JsonProperty("image_url")
  private String imageUrl;

  @JsonProperty("image_base64")
  private String imageBase64;

  private String size;

  @JsonProperty("created_at")
  private LocalDateTime createdAt;

  private String style;

  private Integer seed;
}
