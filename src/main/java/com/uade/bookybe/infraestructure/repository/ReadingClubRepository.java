package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.infraestructure.entity.ReadingClubEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadingClubRepository extends JpaRepository<ReadingClubEntity, String> {

  List<ReadingClubEntity> findByCommunityIdOrderByDateCreatedDesc(String communityId);
  
  List<ReadingClubEntity> findByModeratorIdOrderByDateCreatedDesc(String moderatorId);
  
  List<ReadingClubEntity> findByBookIdOrderByDateCreatedDesc(String bookId);
  

  
  @Query("SELECT rc FROM ReadingClubEntity rc WHERE LOWER(rc.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
         "OR LOWER(rc.description) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY rc.dateCreated DESC")
  List<ReadingClubEntity> searchReadingClubs(@Param("query") String query);
  
  boolean existsByName(String name);
} 