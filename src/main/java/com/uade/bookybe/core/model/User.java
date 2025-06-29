package com.uade.bookybe.core.model;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
public class User {
  private String id;
  private String username;
  private String name;
  private String lastname;
  private String description;
  private String image;
  private Address address;
  private LocalDateTime dateCreated;
  private String email;
  private String password;
}
