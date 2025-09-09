package com.uade.bookybe.router;

import com.uade.bookybe.core.model.Chat;
import com.uade.bookybe.core.model.Message;
import com.uade.bookybe.core.usecase.ChatService;
import com.uade.bookybe.infraestructure.repository.MessageRepository;
import com.uade.bookybe.router.dto.chat.ChatDto;
import com.uade.bookybe.router.dto.chat.CreateChatRequestDto;
import com.uade.bookybe.router.dto.chat.MessageDto;
import com.uade.bookybe.router.dto.chat.SendMessageRequestDto;
import com.uade.bookybe.router.mapper.ChatDtoMapper;
import com.uade.bookybe.router.mapper.MessageDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat management endpoints")
public class ChatController {

  private final ChatService chatService;
  private final MessageRepository messageRepository;

  @PostMapping
  @Operation(summary = "Create or get existing chat with another user")
  public ResponseEntity<ChatDto> createOrGetChat(
      @RequestBody CreateChatRequestDto request, Principal principal) {
    
    String currentUserId = principal.getName();
    Optional<Chat> chatOpt = chatService.createOrGetChat(currentUserId, request.getOtherUserId());
    
    if (chatOpt.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    Chat chat = chatOpt.get();
    ChatDto chatDto = ChatDtoMapper.INSTANCE.toDto(chat);
    
    // Agregar contador de mensajes no leídos
    long unreadCount = messageRepository.countUnreadMessagesByChatIdAndUserId(chat.getId(), currentUserId);
    chatDto.setUnreadCount(unreadCount);
    
    return ResponseEntity.ok(chatDto);
  }

  @GetMapping
  @Operation(summary = "Get all chats for current user")
  public ResponseEntity<List<ChatDto>> getUserChats(Principal principal) {
    String currentUserId = principal.getName();
    List<Chat> chats = chatService.getUserChats(currentUserId);
    
    List<ChatDto> chatDtos = chats.stream()
        .map(chat -> {
          ChatDto chatDto = ChatDtoMapper.INSTANCE.toDto(chat);
          // Agregar contador de mensajes no leídos
          long unreadCount = messageRepository.countUnreadMessagesByChatIdAndUserId(chat.getId(), currentUserId);
          chatDto.setUnreadCount(unreadCount);
          return chatDto;
        })
        .toList();
    
    return ResponseEntity.ok(chatDtos);
  }

  @GetMapping("/{chatId}")
  @Operation(summary = "Get chat by ID")
  public ResponseEntity<ChatDto> getChatById(@PathVariable String chatId, Principal principal) {
    String currentUserId = principal.getName();
    Optional<Chat> chatOpt = chatService.getChatById(chatId, currentUserId);
    
    if (chatOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Chat chat = chatOpt.get();
    ChatDto chatDto = ChatDtoMapper.INSTANCE.toDto(chat);
    
    // Agregar contador de mensajes no leídos
    long unreadCount = messageRepository.countUnreadMessagesByChatIdAndUserId(chat.getId(), currentUserId);
    chatDto.setUnreadCount(unreadCount);
    
    return ResponseEntity.ok(chatDto);
  }

  @GetMapping("/{chatId}/messages")
  @Operation(summary = "Get all messages from a chat")
  public ResponseEntity<List<MessageDto>> getChatMessages(
      @PathVariable String chatId, Principal principal) {
    
    String currentUserId = principal.getName();
    List<Message> messages = chatService.getChatMessages(chatId, currentUserId);
    
    List<MessageDto> messageDtos = messages.stream()
        .map(MessageDtoMapper.INSTANCE::toDto)
        .toList();
    
    return ResponseEntity.ok(messageDtos);
  }

  @PostMapping("/{chatId}/messages")
  @Operation(summary = "Send a message to a chat")
  public ResponseEntity<MessageDto> sendMessage(
      @PathVariable String chatId,
      @RequestBody SendMessageRequestDto request,
      Principal principal) {
    
    String currentUserId = principal.getName();
    Optional<Message> messageOpt = chatService.sendMessage(chatId, currentUserId, request.getContent());
    
    if (messageOpt.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    MessageDto messageDto = MessageDtoMapper.INSTANCE.toDto(messageOpt.get());
    return ResponseEntity.ok(messageDto);
  }

  @PutMapping("/{chatId}/mark-read")
  @Operation(summary = "Mark all messages in chat as read")
  public ResponseEntity<Void> markMessagesAsRead(@PathVariable String chatId, Principal principal) {
    String currentUserId = principal.getName();
    chatService.markMessagesAsRead(chatId, currentUserId);
    return ResponseEntity.ok().build();
  }
}
