package com.uade.bookybe.router.dto.readingclub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
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

  @NotNull(message = "Next meeting date is required")
  private LocalDateTime nextMeeting; // OBLIGATORIO - fecha de la próxima reunión
}
