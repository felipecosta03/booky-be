package com.uade.bookybe.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSignUp {
    private Long id;
    private String username;
    private String name;
    private String lastname;
    private String email;
    private String password;
}
