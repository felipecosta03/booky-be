package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.infraestructure.entity.UserAchievementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievementEntity, String> {

  List<UserAchievementEntity> findByUserId(String userId);

  List<UserAchievementEntity> findByUserIdOrderByDateEarnedDesc(String userId);

  Optional<UserAchievementEntity> findByUserIdAndAchievementId(String userId, String achievementId);

  boolean existsByUserIdAndAchievementId(String userId, String achievementId);

  @Query("SELECT ua FROM UserAchievementEntity ua WHERE ua.userId = :userId AND ua.notified = false")
  List<UserAchievementEntity> findUnnotifiedByUserId(@Param("userId") String userId);

  @Query("SELECT COUNT(ua) FROM UserAchievementEntity ua WHERE ua.userId = :userId")
  long countByUserId(@Param("userId") String userId);
}
