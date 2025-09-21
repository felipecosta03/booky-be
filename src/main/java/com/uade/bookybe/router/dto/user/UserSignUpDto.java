package com.uade.bookybe.router.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
@Schema(description = "User registration data")
public class UserSignUpDto {

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
  @Schema(description = "Unique username", example = "johndoe123", required = true)
  private String username;

  @NotBlank(message = "Name is required")
  @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
  @Schema(description = "User's first name", example = "John", required = true)
  private String name;

  @NotBlank(message = "Last name is required")
  @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
  @Schema(description = "User's last name", example = "Doe", required = true)
  private String lastname;

  @NotBlank(message = "Email is required")
  @Email(message = "Email should be valid")
  @Schema(description = "User's email address", example = "john.doe@example.com", required = true)
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
  @Schema(description = "User's password", example = "SecurePassword123!", required = true)
  private String password;

  @Schema(description = "Profile image encoded in base64", example = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD...")
  private String image;
}
