package com.uade.bookybe.router.dto.book;

import com.uade.bookybe.core.model.constant.BookStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateStatusDto {

  @NotNull(message = "Status is required")
  private BookStatus status;
}
