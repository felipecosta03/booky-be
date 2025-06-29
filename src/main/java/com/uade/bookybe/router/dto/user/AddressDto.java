package com.uade.bookybe.router.dto.user;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
public class AddressDto {

  private String id;
  private String state;
  private String country;
  private Double longitude;
  private Double latitude;
}
