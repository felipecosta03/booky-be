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
@Table(name = "reading_clubs")
public class ReadingClubEntity {
  @Id private String id;

  @Column(name = "date_created")
  private LocalDateTime dateCreated;

  @Column(length = 1000)
  private String description;

  @Column(name = "last_updated")
  private LocalDateTime lastUpdated;

  private String name;

  @Column(name = "book_id")
  private String bookId;

  @Column(name = "community_id", nullable = false)
  private String communityId;

  @Column(name = "moderator_id", nullable = false)
  private String moderatorId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id", insertable = false, updatable = false)
  private BookEntity book;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "community_id", insertable = false, updatable = false)
  private CommunityEntity community;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "moderator_id", insertable = false, updatable = false)
  private UserEntity moderator;
} 