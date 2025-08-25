package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.infraestructure.entity.GamificationProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GamificationProfileRepository extends JpaRepository<GamificationProfileEntity, String> {

  Optional<GamificationProfileEntity> findByUserId(String userId);


}
