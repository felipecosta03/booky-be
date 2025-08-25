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
@Table(name = "user_levels")
public class UserLevelEntity {
  @Id 
  private int level;

  @Column(nullable = false)
  private String name;

  @Column(length = 1000)
  private String description;

  @Column(name = "min_points", nullable = false)
  private int minPoints;

  @Column(name = "max_points", nullable = false)
  private int maxPoints;

  private String badge;
  private String color;
}
