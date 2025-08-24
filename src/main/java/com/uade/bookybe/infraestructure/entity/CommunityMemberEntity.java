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
@Table(name = "community_members")
@IdClass(CommunityMemberId.class)
public class CommunityMemberEntity {

  @Id
  @Column(name = "community_id")
  private String communityId;

  @Id
  @Column(name = "user_id")
  private String userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "community_id", insertable = false, updatable = false)
  private CommunityEntity community;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private UserEntity user;
}
