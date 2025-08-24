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
@Table(name = "reading_club_members")
@IdClass(ReadingClubMemberId.class)
public class ReadingClubMemberEntity {

  @Id
  @Column(name = "reading_club_id")
  private String readingClubId;

  @Id
  @Column(name = "user_id")
  private String userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reading_club_id", insertable = false, updatable = false)
  private ReadingClubEntity readingClub;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private UserEntity user;
}
