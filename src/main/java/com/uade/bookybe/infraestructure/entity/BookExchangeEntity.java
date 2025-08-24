package com.uade.bookybe.infraestructure.entity;

import com.uade.bookybe.core.model.constant.ExchangeStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "book_exchanges")
public class BookExchangeEntity {
  @Id private String id;

  @Column(name = "requester_id", nullable = false)
  private String requesterId;

  @Column(name = "owner_id", nullable = false)
  private String ownerId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ExchangeStatus status;

  @Column(name = "date_created", nullable = false)
  private LocalDateTime dateCreated;

  @Column(name = "date_updated", nullable = false)
  private LocalDateTime dateUpdated;

  @ElementCollection
  @CollectionTable(name = "exchange_owner_books", joinColumns = @JoinColumn(name = "exchange_id"))
  @Column(name = "user_book_id")
  private List<String> ownerBookIds;

  @ElementCollection
  @CollectionTable(
      name = "exchange_requester_books",
      joinColumns = @JoinColumn(name = "exchange_id"))
  @Column(name = "user_book_id")
  private List<String> requesterBookIds;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requester_id", insertable = false, updatable = false)
  private UserEntity requester;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", insertable = false, updatable = false)
  private UserEntity owner;
}
