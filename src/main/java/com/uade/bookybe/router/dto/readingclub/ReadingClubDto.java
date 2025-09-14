package com.uade.bookybe.router.dto.readingclub;

import com.uade.bookybe.router.dto.book.BookDto;
import com.uade.bookybe.router.dto.community.CommunityDto;
import com.uade.bookybe.router.dto.user.UserPreviewDto;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReadingClubDto {
  private String id;
  private LocalDateTime dateCreated;
  private String description;
  private LocalDateTime lastUpdated;

  @NotBlank(message = "Name is required")
  private String name;

  @NotBlank(message = "Book ID is required")
  private String bookId; // OBLIGATORIO

  @NotBlank(message = "Community ID is required")
  private String communityId; // OBLIGATORIO

  @NotBlank(message = "Moderator ID is required")
  private String moderatorId;

  private LocalDateTime nextMeeting; // Fecha de la próxima reunión

  private Integer currentChapter; // Capítulo actual desde el cual se arrancará la reunión

  private BookDto book; // OBLIGATORIO (enriquecido)
  private CommunityDto community; // OBLIGATORIO (enriquecido)
  private UserPreviewDto moderator; // OBLIGATORIO (enriquecido)
  private long memberCount;
  private boolean isJoinAvailable; // Campo calculado dinámicamente por el service

}
