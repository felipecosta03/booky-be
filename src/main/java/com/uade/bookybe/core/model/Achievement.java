package com.uade.bookybe.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Achievement {
  private String id;
  private String name;
  private String description;
  private String category;
  private String icon;
  private int requiredValue;
  private String condition; // Tipo de condición (e.g., "BOOKS_READ", "EXCHANGES_COMPLETED")
  private int pointsReward;
  private boolean isActive;
}
