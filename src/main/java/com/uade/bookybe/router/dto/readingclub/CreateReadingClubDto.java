package com.uade.bookybe.router.dto.readingclub;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateReadingClubDto {
  @NotBlank(message = "Name is required")
  private String name;
  
  private String description;
  
  @NotBlank(message = "Community ID is required")
  private String communityId; // OBLIGATORIO - el club debe pertenecer a una comunidad
  
  @NotBlank(message = "Book ID is required")
  private String bookId; // OBLIGATORIO - el libro sobre el que será el club
} 