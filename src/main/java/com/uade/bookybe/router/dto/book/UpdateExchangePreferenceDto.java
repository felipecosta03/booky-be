package com.uade.bookybe.router.dto.book;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateExchangePreferenceDto {
  
  @NotNull(message = "Exchange preference is required")
  private Boolean wantsToExchange;
} 