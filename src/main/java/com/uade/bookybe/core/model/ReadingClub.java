package com.uade.bookybe.core.model;

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
  private String name;
  private Long bookId;
  private String communityId;
  private String moderatorId;
  private Book book;
  private Community community;
  private User moderator;
} 