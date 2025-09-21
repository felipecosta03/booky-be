package com.uade.bookybe.router.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreatePostDto {
  @NotEmpty private String body;
  private String communityId; // Opcional - si no se proporciona, es un post general

  @Schema(description = "Post image encoded in base64", example = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD...")
  private String image;
}
