package com.uade.bookybe.infraestructure.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
@Entity
@Table(name = "addresses")
public class AddressEntity {
  @Id private String id;

  private String state;
  private String country;
  private Double longitude;
  private Double latitude;
}
