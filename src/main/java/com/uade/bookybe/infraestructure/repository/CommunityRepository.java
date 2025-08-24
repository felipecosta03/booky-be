package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.infraestructure.entity.CommunityEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityRepository extends JpaRepository<CommunityEntity, String> {

  List<CommunityEntity> findByAdminIdOrderByDateCreatedDesc(String adminId);

  @Query(
      "SELECT c FROM CommunityEntity c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) "
          + "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY c.dateCreated DESC")
  List<CommunityEntity> searchCommunities(@Param("query") String query);

  @Query("SELECT c FROM CommunityEntity c JOIN FETCH c.admin ORDER BY c.dateCreated DESC")
  List<CommunityEntity> findAllWithAdminOrderByDateCreatedDesc();

  boolean existsByName(String name);
}
