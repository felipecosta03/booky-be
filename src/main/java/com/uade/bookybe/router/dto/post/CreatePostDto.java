package com.uade.bookybe.router.dto.post;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreatePostDto {
  private String body;
  private String communityId; // Opcional - si no se proporciona, es un post general
} 