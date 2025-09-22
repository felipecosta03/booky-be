package com.uade.bookybe.router.dto.user;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RateUserDto {
  private Double averageRating;
  private Long totalRatings;
}
