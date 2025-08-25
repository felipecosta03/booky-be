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
@Table(name = "gamification_profiles")
public class GamificationProfileEntity {
  @Id 
  private String id;

  @Column(name = "user_id", nullable = false, unique = true)
  private String userId;

  @Column(name = "total_points", nullable = false)
  private int totalPoints = 0;

  @Column(name = "current_level", nullable = false)
  private int currentLevel = 1;

  @Column(name = "books_read", nullable = false)
  private int booksRead = 0;

  @Column(name = "exchanges_completed", nullable = false)
  private int exchangesCompleted = 0;

  @Column(name = "posts_created", nullable = false)
  private int postsCreated = 0;

  @Column(name = "comments_created", nullable = false)
  private int commentsCreated = 0;

  @Column(name = "communities_joined", nullable = false)
  private int communitiesJoined = 0;

  @Column(name = "communities_created", nullable = false)
  private int communitiesCreated = 0;

  @Column(name = "reading_clubs_joined", nullable = false)
  private int readingClubsJoined = 0;

  @Column(name = "reading_clubs_created", nullable = false)
  private int readingClubsCreated = 0;

  @Column(name = "last_activity")
  private LocalDateTime lastActivity;

  @Column(name = "date_created", nullable = false)
  private LocalDateTime dateCreated;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
  private UserEntity user;
}
