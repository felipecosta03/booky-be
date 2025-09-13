package com.uade.bookybe.router.dto.readingclub;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateReadingClubMeetingDto {
  @NotNull(message = "Next meeting date is required")
  private LocalDateTime nextMeeting; // OBLIGATORIO - nueva fecha de la reunión

  @NotNull(message = "Current chapter is required")
  @Positive(message = "Current chapter must be a positive number")
  private Integer currentChapter; // OBLIGATORIO - capítulo desde el cual se arrancará
}
