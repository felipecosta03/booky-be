package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.infraestructure.entity.ChatEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends JpaRepository<ChatEntity, String> {

  @Query(
      "SELECT c FROM ChatEntity c WHERE (c.user1Id = :userId OR c.user2Id = :userId) ORDER BY c.dateUpdated DESC")
  List<ChatEntity> findByUserIdOrderByDateUpdatedDesc(@Param("userId") String userId);

  @Query(
      "SELECT c FROM ChatEntity c WHERE (c.user1Id = :user1Id AND c.user2Id = :user2Id) OR (c.user1Id = :user2Id AND c.user2Id = :user1Id)")
  Optional<ChatEntity> findByUsers(@Param("user1Id") String user1Id, @Param("user2Id") String user2Id);

  @Query(
      "SELECT c FROM ChatEntity c JOIN FETCH c.user1 JOIN FETCH c.user2 WHERE c.id = :chatId")
  Optional<ChatEntity> findByIdWithUsers(@Param("chatId") String chatId);

  @Query(
      "SELECT c FROM ChatEntity c JOIN FETCH c.user1 JOIN FETCH c.user2 WHERE (c.user1Id = :userId OR c.user2Id = :userId) ORDER BY c.dateUpdated DESC")
  List<ChatEntity> findByUserIdWithUsersOrderByDateUpdatedDesc(@Param("userId") String userId);
}
