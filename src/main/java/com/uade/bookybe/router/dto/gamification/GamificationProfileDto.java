package com.uade.bookybe.router.dto.gamification;

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
public class GamificationProfileDto {
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
  
  // Enhanced data
  private UserLevelDto userLevel;
  private List<UserAchievementDto> achievements;
  private int pointsToNextLevel;
}
