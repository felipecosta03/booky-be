package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.infraestructure.entity.BookEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<BookEntity, String> {

  Optional<BookEntity> findByIsbn(String isbn);

  @Query(
      """
      SELECT DISTINCT b FROM BookEntity b LEFT JOIN b.categories c
      WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))
         OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%'))
         OR LOWER(c) LIKE LOWER(CONCAT('%', :query, '%'))
      """)
  List<BookEntity> searchBooks(@Param("query") String query);
}
