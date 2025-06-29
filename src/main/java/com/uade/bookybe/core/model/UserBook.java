package com.uade.bookybe.core.model;

import com.uade.bookybe.core.model.constant.BookStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserBook {
  private Long id;
  private String userId;
  private Long bookId;
  private BookStatus status;
  private boolean favorite;
  private boolean wantsToExchange;
  private Book book;
}