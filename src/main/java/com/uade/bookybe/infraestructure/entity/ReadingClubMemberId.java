package com.uade.bookybe.infraestructure.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingClubMemberId implements Serializable {
  private String readingClubId;
  private String userId;
} 