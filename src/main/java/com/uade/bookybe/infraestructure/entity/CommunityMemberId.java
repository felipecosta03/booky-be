package com.uade.bookybe.infraestructure.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunityMemberId implements Serializable {
  private String communityId;
  private String userId;
} 