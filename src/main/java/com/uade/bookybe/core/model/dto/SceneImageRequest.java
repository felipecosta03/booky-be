package com.uade.bookybe.core.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SceneImageRequest {

  @NotBlank(message = "Text cannot be blank")
  @Size(min = 15, max = 2000, message = "Text must be between 15 and 2000 characters")
  private String text;

  private String style;

  private Integer seed;

  @JsonProperty("return_base64")
  private Boolean returnBase64;

  private String size;
}
