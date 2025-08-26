package com.uade.bookybe.router.dto.community;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateCommunityDto {
  @NotEmpty private String name;
  @NotEmpty private String description;
}
