package com.uade.bookybe.router.dto.readingclub;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateReadingClubDto {
  private String name;
  private String description;
  private String bookId; // El libro sobre el que será el club
} 