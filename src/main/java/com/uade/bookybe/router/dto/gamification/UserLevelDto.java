package com.uade.bookybe.router.dto.gamification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserLevelDto {
  private int level;
  private String name;
  private String description;
  private int minPoints;
  private int maxPoints;
  private String badge;
  private String color;
}
