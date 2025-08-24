package com.uade.bookybe.infraestructure.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "community")
public class CommunityEntity {
  @Id private String id;

  @Column(name = "date_created")
  private LocalDateTime dateCreated;

  @Column(length = 1000)
  private String description;

  private String name;

  @Column(name = "admin_id", nullable = false)
  private String adminId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "admin_id", insertable = false, updatable = false)
  private UserEntity admin;
}
