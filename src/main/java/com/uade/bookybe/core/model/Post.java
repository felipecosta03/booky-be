package com.uade.bookybe.core.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Post {
  private String id;
  private String body;
  private LocalDateTime dateCreated;
  private String image;
  private String userId;
  private String communityId;
  private User user;
  private Community community;
  @Builder.Default
  private List<String> likes = new ArrayList<>();
}
