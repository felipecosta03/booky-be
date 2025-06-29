package com.uade.bookybe.router.dto.user;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
public class UserPreviewDto {

  private String id;
  private String username;
  private String image;
}
