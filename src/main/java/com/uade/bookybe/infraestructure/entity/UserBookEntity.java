package com.uade.bookybe.infraestructure.entity;

import com.uade.bookybe.core.model.constant.BookStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_books")
public class UserBookEntity {
  @Id private String id;

  @Column(name = "user_id", nullable = false)
  private String userId;

  @Column(name = "book_id", nullable = false)
  private String bookId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private BookStatus status;

  @Column(name = "is_favorite", nullable = false)
  private boolean favorite;

  @Column(name = "wants_to_exchange", nullable = false)
  private boolean wantsToExchange;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id", insertable = false, updatable = false)
  private BookEntity book;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private UserEntity user;
}
