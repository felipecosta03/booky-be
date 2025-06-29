package com.uade.bookybe.core.model;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
public class UserSignUp {
  private String username;
  private String name;
  private String lastname;
  private String email;
  private String password;
}
