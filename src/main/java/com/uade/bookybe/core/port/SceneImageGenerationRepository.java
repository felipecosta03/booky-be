package com.uade.bookybe.core.port;

import com.uade.bookybe.core.model.SceneImageGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SceneImageGenerationRepository extends JpaRepository<SceneImageGeneration, Long> {

  /**
   * Find existing generation by book ID and fragment hash to avoid duplicates
   */
  Optional<SceneImageGeneration> findByBookIdAndFragmentHash(String bookId, String fragmentHash);

  /**
   * Find all generations for a specific book
   */
  List<SceneImageGeneration> findByBookIdOrderByCreatedAtDesc(String bookId);

  /**
   * Count generations for a book (for analytics)
   */
  long countByBookId(String bookId);

  /**
   * Find recent generations (for monitoring and analytics)
   */
  @Query("SELECT sig FROM SceneImageGeneration sig ORDER BY sig.createdAt DESC")
  List<SceneImageGeneration> findRecentGenerations(@Param("limit") int limit);
}
