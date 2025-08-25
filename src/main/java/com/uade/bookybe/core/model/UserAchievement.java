package com.uade.bookybe.core.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAchievement {
  private String id;
  private String userId;
  private String achievementId;
  private LocalDateTime dateEarned;
  private boolean notified;
  
  // Propiedades del achievement
  private Achievement achievement;
}
