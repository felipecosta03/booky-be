package com.uade.bookybe.router.dto.gamification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GamificationActivityDto {
  private String name;
  private int points;
  private String description;
}
