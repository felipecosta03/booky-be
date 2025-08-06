package com.uade.bookybe.router.dto.exchange;

import com.uade.bookybe.core.model.constant.ExchangeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateExchangeStatusDto {
  
  @NotNull(message = "Status is required")
  private ExchangeStatus status;
} 