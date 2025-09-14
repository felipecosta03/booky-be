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
public class Community {
  private String id;
  private LocalDateTime dateCreated;
  private String description;
  private String name;
  private String adminId;
  private User admin;
  private long memberCount; // Campo calculado dinámicamente por el service
  private boolean isJoinAvailable; // Campo calculado dinámicamente por el service
}
