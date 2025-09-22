package com.uade.bookybe.router.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserPreviewDto {
  private String id;
  private String username;
  private String name;
  private String lastname;
  private String image;
  private AddressDto address;
  private RateUserDto userRate;
}
