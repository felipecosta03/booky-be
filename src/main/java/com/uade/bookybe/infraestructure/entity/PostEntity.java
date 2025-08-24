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
@Table(name = "post")
public class PostEntity {
  @Id private String id;

  @Column(length = 2000)
  private String body;

  @Column(name = "date_created")
  private LocalDateTime dateCreated;

  private String image;

  @Column(name = "user_id", nullable = false)
  private String userId;

  @Column(name = "community_id")
  private String communityId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private UserEntity user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "community_id", insertable = false, updatable = false)
  private CommunityEntity community;
}
