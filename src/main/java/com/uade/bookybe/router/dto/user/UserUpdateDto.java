package com.uade.bookybe.router.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
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

  @Schema(description = "Profile image encoded in base64", example = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD...")
  private String image;
}
