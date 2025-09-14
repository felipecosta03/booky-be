package com.uade.bookybe.infraestructure.entity;

import jakarta.persistence.*;
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
@Table(name = "books")
public class BookEntity {
  @Id private String id;

  private String isbn;
  private String title;
  @Column(length = 3000)
  private String overview;

  @Column(columnDefinition = "TEXT", length = 3000)
  private String synopsis;

  private Integer pages;
  private String edition;
  private String publisher;
  private String author;
  private String image;
  private Integer rate;

  @ElementCollection
  @CollectionTable(name = "book_categories", joinColumns = @JoinColumn(name = "book_id"))
  @Column(name = "category")
  private List<String> categories;
}
