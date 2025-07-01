package com.uade.bookybe.router.dto.community;

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
public class CommunityDto {
  private String id;
  private LocalDateTime dateCreated;
  private String description;
  private String name;
  private String adminId;
  private UserPreviewDto admin;
  private long memberCount;
} 