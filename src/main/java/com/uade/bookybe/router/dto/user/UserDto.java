package com.uade.bookybe.router.dto.user;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
public class UserDto {

  private String id;
  private String username;
  private String email;
  private String name;
  private String lastname;
  private String description;
  private String image;
  private AddressDto address;
  private LocalDateTime dateCreated;
}
