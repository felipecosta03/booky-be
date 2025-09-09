package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.infraestructure.entity.MessageEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, String> {

  List<MessageEntity> findByChatIdOrderByDateSentAsc(String chatId);

  @Query(
      "SELECT m FROM MessageEntity m JOIN FETCH m.sender WHERE m.chatId = :chatId ORDER BY m.dateSent ASC")
  List<MessageEntity> findByChatIdWithSenderOrderByDateSentAsc(@Param("chatId") String chatId);

  @Query(
      "SELECT m FROM MessageEntity m WHERE m.chatId = :chatId ORDER BY m.dateSent DESC LIMIT 1")
  Optional<MessageEntity> findLastMessageByChatId(@Param("chatId") String chatId);

  @Query(
      "SELECT COUNT(m) FROM MessageEntity m WHERE m.chatId = :chatId AND m.senderId != :userId AND m.read = false")
  long countUnreadMessagesByChatIdAndUserId(@Param("chatId") String chatId, @Param("userId") String userId);
}
