package com.uade.bookybe.router.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
@Schema(description = "User login credentials")
public class UserSignInDto {

  @NotBlank(message = "Email is required")
  @Email(message = "Email should be valid")
  @Schema(description = "User's email address", example = "john.doe@example.com", required = true)
  private String email;

  @NotBlank(message = "Password is required")
  @Schema(description = "User's password", example = "SecurePassword123!", required = true)
  private String password;
}
