package com.uade.bookybe.infraestructure.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_achievements")
public class UserAchievementEntity {
  @Id 
  private String id;

  @Column(name = "user_id", nullable = false)
  private String userId;

  @Column(name = "achievement_id", nullable = false)
  private String achievementId;

  @Column(name = "date_earned", nullable = false)
  private LocalDateTime dateEarned;

  @Column(name = "notified", nullable = false)
  private boolean notified = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
  private UserEntity user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "achievement_id", referencedColumnName = "id", insertable = false, updatable = false)
  private AchievementEntity achievement;
}
