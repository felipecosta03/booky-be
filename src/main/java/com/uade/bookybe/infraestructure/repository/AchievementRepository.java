package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.infraestructure.entity.AchievementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchievementRepository extends JpaRepository<AchievementEntity, String> {

  List<AchievementEntity> findByIsActiveTrue();

  List<AchievementEntity> findByCategory(String category);

  List<AchievementEntity> findByCategoryAndIsActiveTrue(String category);

  @Query("SELECT a FROM AchievementEntity a WHERE a.condition = :condition AND a.isActive = true")
  List<AchievementEntity> findByConditionAndIsActiveTrue(@Param("condition") String condition);
}
