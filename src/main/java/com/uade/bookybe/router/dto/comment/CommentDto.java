package com.uade.bookybe.router.dto.comment;

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
public class CommentDto {
  private String id;
  private String body;
  private LocalDateTime dateCreated;
  private String userId;
  private String postId;
  private UserPreviewDto user;
} 