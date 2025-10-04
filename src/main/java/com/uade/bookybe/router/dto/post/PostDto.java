package com.uade.bookybe.router.dto.post;

import com.uade.bookybe.router.dto.community.CommunityDto;
import com.uade.bookybe.router.dto.user.UserPreviewDto;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PostDto {
  private String id;
  private String body;
  private LocalDateTime dateCreated;
  private String image;
  private String userId;
  private String communityId;
  private UserPreviewDto user;
  private CommunityDto community;
  private Integer commentsCount;
  private Integer likesCount;
  private Boolean isLikedByUser;
  private List<String> likes;
}
