package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.infraestructure.entity.UserLevelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLevelRepository extends JpaRepository<UserLevelEntity, Integer> {

  @Query("SELECT ul FROM UserLevelEntity ul WHERE ul.minPoints <= :points AND ul.maxPoints >= :points")
  Optional<UserLevelEntity> findByPointsRange(@Param("points") int points);

  @Query("SELECT ul FROM UserLevelEntity ul WHERE ul.level = (SELECT MAX(ul2.level) FROM UserLevelEntity ul2 WHERE ul2.minPoints <= :points)")
  Optional<UserLevelEntity> findHighestLevelForPoints(@Param("points") int points);
}
