package com.uade.bookybe.router.dto.user;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
public class UserUpdateDto {

  private String id;
  private String name;
  private String lastname;
  private String description;
  private AddressDto address;
}
