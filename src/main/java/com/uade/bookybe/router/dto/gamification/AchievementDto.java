package com.uade.bookybe.router.dto.gamification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AchievementDto {
  private String id;
  private String name;
  private String description;
  private String category;
  private String icon;
  private int requiredValue;
  private String condition;
  private int pointsReward;
  private boolean isActive;
}
