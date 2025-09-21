package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.model.Chat;
import com.uade.bookybe.core.model.Message;
import com.uade.bookybe.core.port.ImageStoragePort;
import com.uade.bookybe.core.usecase.ChatService;
import com.uade.bookybe.infraestructure.entity.ChatEntity;
import com.uade.bookybe.infraestructure.entity.MessageEntity;
import com.uade.bookybe.infraestructure.mapper.ChatEntityMapper;
import com.uade.bookybe.infraestructure.mapper.MessageEntityMapper;
import com.uade.bookybe.infraestructure.repository.ChatRepository;
import com.uade.bookybe.infraestructure.repository.MessageRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

  private final ChatRepository chatRepository;
  private final MessageRepository messageRepository;
  private final ImageStoragePort imageStoragePort;

  @Override
  @Transactional
  public Optional<Chat> createOrGetChat(String user1Id, String user2Id) {
    // Verificar si ya existe un chat entre estos usuarios
    Optional<ChatEntity> existingChat = chatRepository.findByUsers(user1Id, user2Id);
    
    if (existingChat.isPresent()) {
      return Optional.of(ChatEntityMapper.INSTANCE.toModel(existingChat.get()));
    }

    // Crear nuevo chat
    ChatEntity newChat = ChatEntity.builder()
        .id(UUID.randomUUID().toString())
        .user1Id(user1Id)
        .user2Id(user2Id)
        .dateCreated(LocalDateTime.now())
        .dateUpdated(LocalDateTime.now())
        .build();

    ChatEntity savedChat = chatRepository.save(newChat);
    return Optional.of(ChatEntityMapper.INSTANCE.toModel(savedChat));
  }

  @Override
  @Transactional
  public Optional<Message> sendMessage(
      String chatId, String senderId, String content, String imageBase64) {
    // Verificar que el chat existe y el usuario pertenece al chat
    Optional<ChatEntity> chatOpt = chatRepository.findById(chatId);
    if (chatOpt.isEmpty()) {
      return Optional.empty();
    }

    ChatEntity chat = chatOpt.get();
    if (!chat.getUser1Id().equals(senderId) && !chat.getUser2Id().equals(senderId)) {
      return Optional.empty(); // Usuario no pertenece al chat
    }

    // Crear el mensaje
    MessageEntity.MessageEntityBuilder message = MessageEntity.builder()
        .id(UUID.randomUUID().toString())
        .chatId(chatId)
        .senderId(senderId)
        .content(content)
        .dateSent(LocalDateTime.now())
        .read(false);

    if (imageBase64 != null) {
        Optional<String> uploadedImageUrl = imageStoragePort.uploadImage(imageBase64, "booky/messages");
        uploadedImageUrl.ifPresent(message::image);
    }

    MessageEntity savedMessage = messageRepository.save(message.build());

    // Actualizar fecha de última actualización del chat
    chat.setDateUpdated(LocalDateTime.now());
    chatRepository.save(chat);

    return Optional.of(MessageEntityMapper.INSTANCE.toModel(savedMessage));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Message> getChatMessages(String chatId, String userId) {
    // Verificar que el usuario pertenece al chat
    Optional<ChatEntity> chatOpt = chatRepository.findById(chatId);
    if (chatOpt.isEmpty()) {
      return List.of();
    }

    ChatEntity chat = chatOpt.get();
    if (!chat.getUser1Id().equals(userId) && !chat.getUser2Id().equals(userId)) {
      return List.of(); // Usuario no pertenece al chat
    }

    List<MessageEntity> messages = messageRepository.findByChatIdWithSenderOrderByDateSentAsc(chatId);
    return messages.stream()
        .map(MessageEntityMapper.INSTANCE::toModel)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Chat> getUserChats(String userId) {
    List<ChatEntity> chatEntities = chatRepository.findByUserIdWithUsersOrderByDateUpdatedDesc(userId);
    
    return chatEntities.stream()
        .map(chatEntity -> {
          Chat chat = ChatEntityMapper.INSTANCE.toModel(chatEntity);
          
          // Obtener el último mensaje
          Optional<MessageEntity> lastMessage = messageRepository.findLastMessageByChatId(chatEntity.getId());
          lastMessage.ifPresent(msg -> chat.setLastMessage(MessageEntityMapper.INSTANCE.toModel(msg)));
          
          return chat;
        })
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Chat> getChatById(String chatId, String userId) {
    Optional<ChatEntity> chatOpt = chatRepository.findByIdWithUsers(chatId);
    if (chatOpt.isEmpty()) {
      return Optional.empty();
    }

    ChatEntity chatEntity = chatOpt.get();
    if (!chatEntity.getUser1Id().equals(userId) && !chatEntity.getUser2Id().equals(userId)) {
      return Optional.empty(); // Usuario no pertenece al chat
    }

    return Optional.of(ChatEntityMapper.INSTANCE.toModel(chatEntity));
  }

  @Override
  @Transactional
  public void markMessagesAsRead(String chatId, String userId) {
    // Verificar que el usuario pertenece al chat
    Optional<ChatEntity> chatOpt = chatRepository.findById(chatId);
    if (chatOpt.isEmpty()) {
      return;
    }

    ChatEntity chat = chatOpt.get();
    if (!chat.getUser1Id().equals(userId) && !chat.getUser2Id().equals(userId)) {
      return; // Usuario no pertenece al chat
    }

    // Marcar como leídos todos los mensajes que no son del usuario actual
    List<MessageEntity> unreadMessages = messageRepository.findByChatIdOrderByDateSentAsc(chatId)
        .stream()
        .filter(msg -> !msg.getSenderId().equals(userId) && !msg.isRead())
        .toList();

    unreadMessages.forEach(msg -> msg.setRead(true));
    messageRepository.saveAll(unreadMessages);
  }
}
