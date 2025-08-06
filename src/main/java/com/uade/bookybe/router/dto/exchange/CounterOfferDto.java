package com.uade.bookybe.router.dto.exchange;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CounterOfferDto {
  
  @NotEmpty(message = "At least one owner book is required")
  private List<String> ownerBookIds;
  
  @NotEmpty(message = "At least one requester book is required")
  private List<String> requesterBookIds;
} 