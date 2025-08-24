package com.uade.bookybe.router.dto.book;

import com.uade.bookybe.core.model.constant.BookStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddBookToLibraryDto {

  @NotBlank(message = "ISBN is required")
  private String isbn;

  @NotNull(message = "Status is required")
  private BookStatus status;
}
