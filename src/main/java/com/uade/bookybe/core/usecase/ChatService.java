package com.uade.bookybe.core.usecase;

import com.uade.bookybe.core.model.Chat;
import com.uade.bookybe.core.model.Message;
import java.util.List;
import java.util.Optional;

public interface ChatService {
  Optional<Chat> createOrGetChat(String user1Id, String user2Id);
  Optional<Message> sendMessage(String chatId, String senderId, String content, String image);
  List<Message> getChatMessages(String chatId, String userId);
  List<Chat> getUserChats(String userId);
  Optional<Chat> getChatById(String chatId, String userId);
  void markMessagesAsRead(String chatId, String userId);
}
