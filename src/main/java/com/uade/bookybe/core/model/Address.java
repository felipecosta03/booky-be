package com.uade.bookybe.core.model;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
public class Address {
  private String id;
  private String state;
  private String country;
  private Double longitude;
  private Double latitude;
}
