package com.uade.bookybe.router.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class UserDto {

  private String id;
  private String username;
  private String name;
  private String lastname;
  private String description;
  private String image;
  private Integer coins;
  private AddressDto address;
  private LocalDateTime dateCreated;
}
