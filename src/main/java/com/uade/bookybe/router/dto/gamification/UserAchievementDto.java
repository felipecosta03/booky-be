package com.uade.bookybe.router.dto.gamification;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAchievementDto {
  private String id;
  private String userId;
  private String achievementId;
  private LocalDateTime dateEarned;
  private boolean notified;
  private AchievementDto achievement;
}
