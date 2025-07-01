package com.uade.bookybe.router.dto.book;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class BookPreviewDto {

  private String id;
  private String title;
  private String author;
  private Integer rate;
  private String image;
}
