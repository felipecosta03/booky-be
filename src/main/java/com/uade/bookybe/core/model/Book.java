package com.uade.bookybe.core.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Book {
  private String id;
  private String isbn;
  private String title;
  private String overview;
  private String synopsis;
  private Integer pages;
  private String edition;
  private String publisher;
  private String author;
  private String image;
  private Integer rate;
  private List<String> categories;
} 