package com.uade.bookybe.router.dto.readingclub;

import com.uade.bookybe.router.dto.book.BookDto;
import com.uade.bookybe.router.dto.community.CommunityDto;
import com.uade.bookybe.router.dto.user.UserPreviewDto;
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
  private String name;
  private Long bookId;
  private String communityId;
  private String moderatorId;
  private BookDto book;
  private CommunityDto community;
  private UserPreviewDto moderator;
  private long memberCount;
} 