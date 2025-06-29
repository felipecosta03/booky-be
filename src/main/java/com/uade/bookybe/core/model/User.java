package com.uade.bookybe.core.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
