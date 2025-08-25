package com.uade.bookybe.infraestructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "achievements")
public class AchievementEntity {
  @Id 
  private String id;

  @Column(nullable = false)
  private String name;

  @Column(length = 1000)
  private String description;

  @Column(nullable = false)
  private String category;

  private String icon;

  @Column(name = "required_value", nullable = false)
  private int requiredValue;

  @Column(name = "condition_type", nullable = false)
  private String condition;

  @Column(name = "points_reward", nullable = false)
  private int pointsReward;

  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;
}
