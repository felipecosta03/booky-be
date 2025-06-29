package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.infraestructure.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
  @Query(
      value =
          "SELECT COUNT(*) > 0 FROM user_follows WHERE follower_id = :followerId AND followed_id = :followedId",
      nativeQuery = true)
  boolean isFollowing(@Param("followerId") Long followerId, @Param("followedId") Long followedId);

  @Modifying
  @Transactional
  @Query(
      value =
          "INSERT INTO user_follows (follower_id, followed_id) VALUES (:followerId, :followedId)",
      nativeQuery = true)
  void follow(@Param("followerId") Long followerId, @Param("followedId") Long followedId);
}
