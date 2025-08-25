package com.uade.bookybe.core.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GamificationProfile {
  private String id;
  private String userId;
  private int totalPoints;
  private int currentLevel;
  private int booksRead;
  private int exchangesCompleted;
  private int postsCreated;
  private int commentsCreated;
  private int communitiesJoined;
  private int communitiesCreated;
  private int readingClubsJoined;
  private int readingClubsCreated;
  private LocalDateTime lastActivity;
  private LocalDateTime dateCreated;
  
  // Propiedades calculadas
  private UserLevel userLevel;
  private List<UserAchievement> achievements;
  private int pointsToNextLevel;
}
