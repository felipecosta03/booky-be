package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.infraestructure.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
  @Query(
      value =
          "SELECT COUNT(*) > 0 FROM user_follows WHERE follower_id = :followerId AND followed_id = :followedId",
      nativeQuery = true)
  boolean isFollowing(
      @Param("followerId") String followerId, @Param("followedId") String followedId);

  @Modifying
  @Transactional
  @Query(
      value =
          "INSERT INTO user_follows (follower_id, followed_id) VALUES (:followerId, :followedId)",
      nativeQuery = true)
  void follow(@Param("followerId") String followerId, @Param("followedId") String followedId);

  @Modifying
  @Transactional
  @Query(
      value =
          "DELETE FROM user_follows WHERE follower_id = :followerId AND followed_id = :followedId",
      nativeQuery = true)
  void unfollow(@Param("followerId") String followerId, @Param("followedId") String followedId);

  @Query(
      value =
          "SELECT u.* FROM users u INNER JOIN user_follows uf ON u.id = uf.follower_id WHERE uf.followed_id = :userId",
      nativeQuery = true)
  List<UserEntity> findFollowers(@Param("userId") String userId);

  @Query(
      value =
          "SELECT u.* FROM users u INNER JOIN user_follows uf ON u.id = uf.followed_id WHERE uf.follower_id = :userId",
      nativeQuery = true)
  List<UserEntity> findFollowing(@Param("userId") String userId);

  Optional<UserEntity> findByEmail(String email);
}
