package com.uade.bookybe.core.model;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReadingClub {
  private String id;
  private LocalDateTime dateCreated;
  private String description;
  private LocalDateTime lastUpdated;

  @NotBlank(message = "Name is required")
  private String name;

  @NotBlank(message = "Book ID is required")
  private String bookId; // OBLIGATORIO - todo reading club debe tener un libro específico

  @NotBlank(message = "Community ID is required")
  private String communityId; // OBLIGATORIO - todo reading club debe pertenecer a una comunidad

  @NotBlank(message = "Moderator ID is required")
  private String moderatorId;

  private LocalDateTime nextMeeting; // Fecha de la próxima reunión

  private Integer currentChapter; // Capítulo actual desde el cual se arrancará la reunión

  private long memberCount;
}
