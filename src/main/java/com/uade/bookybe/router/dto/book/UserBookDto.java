package com.uade.bookybe.router.dto.book;

import com.uade.bookybe.core.model.constant.BookStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserBookDto {
  private Long id;
  private String userId;
  private BookStatus status;
  private boolean favorite;
  private boolean wantsToExchange;
  private BookDto book;
} 