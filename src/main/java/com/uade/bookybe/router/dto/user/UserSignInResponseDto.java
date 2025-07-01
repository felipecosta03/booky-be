package com.uade.bookybe.router.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
@Schema(description = "User sign-in response with user data and JWT token")
public class UserSignInResponseDto {

  @Schema(description = "JWT authentication token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
  private String token;

  @Schema(description = "User information")
  private UserDto user;
} 