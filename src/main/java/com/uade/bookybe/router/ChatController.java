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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
  @Operation(
      summary = "Create or get existing chat with another user",
      description = "Creates a new chat or returns existing chat between current user and specified user"
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Chat created or retrieved successfully",
          content = @Content(schema = @Schema(implementation = ChatDto.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid request data"),
      @ApiResponse(responseCode = "401", description = "Not authenticated")
  })
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
  @Operation(
      summary = "Get all chats for current user",
      description = "Returns all chats where the current user is a participant"
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Chats retrieved successfully",
          content = @Content(schema = @Schema(implementation = ChatDto.class))
      ),
      @ApiResponse(responseCode = "401", description = "Not authenticated")
  })
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
  @Operation(
      summary = "Get chat by ID",
      description = "Returns a specific chat by its ID if user is a participant"
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Chat found",
          content = @Content(schema = @Schema(implementation = ChatDto.class))
      ),
      @ApiResponse(responseCode = "404", description = "Chat not found or user not authorized"),
      @ApiResponse(responseCode = "401", description = "Not authenticated")
  })
  public ResponseEntity<ChatDto> getChatById(
      @Parameter(description = "Chat ID", required = true) @PathVariable String chatId, 
      Principal principal) {
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
  @Operation(
      summary = "Get all messages from a chat",
      description = "Returns all messages from a specific chat if user is a participant"
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Messages retrieved successfully",
          content = @Content(schema = @Schema(implementation = MessageDto.class))
      ),
      @ApiResponse(responseCode = "404", description = "Chat not found or user not authorized"),
      @ApiResponse(responseCode = "401", description = "Not authenticated")
  })
  public ResponseEntity<List<MessageDto>> getChatMessages(
      @Parameter(description = "Chat ID", required = true) @PathVariable String chatId, 
      Principal principal) {
    
    String currentUserId = principal.getName();
    List<Message> messages = chatService.getChatMessages(chatId, currentUserId);
    
    List<MessageDto> messageDtos = messages.stream()
        .map(MessageDtoMapper.INSTANCE::toDto)
        .toList();
    
    return ResponseEntity.ok(messageDtos);
  }

  @PostMapping("/{chatId}/messages")
  @Operation(
      summary = "Send a message to a chat",
      description = "Sends a new message to the specified chat if user is a participant"
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Message sent successfully",
          content = @Content(schema = @Schema(implementation = MessageDto.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid message data or user not authorized"),
      @ApiResponse(responseCode = "404", description = "Chat not found"),
      @ApiResponse(responseCode = "401", description = "Not authenticated")
  })
  public ResponseEntity<MessageDto> sendMessage(
      @Parameter(description = "Chat ID", required = true) @PathVariable String chatId,
      @RequestBody SendMessageRequestDto request,
      Principal principal) {
    
    String currentUserId = principal.getName();
    Optional<Message> messageOpt =
        chatService.sendMessage(chatId, currentUserId, request.getContent(), request.getImage());

    if (messageOpt.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    MessageDto messageDto = MessageDtoMapper.INSTANCE.toDto(messageOpt.get());
    return ResponseEntity.ok(messageDto);
  }

  @PutMapping("/{chatId}/mark-read")
  @Operation(
      summary = "Mark all messages in chat as read",
      description = "Marks all messages in the specified chat as read for the current user"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Messages marked as read successfully"),
      @ApiResponse(responseCode = "404", description = "Chat not found or user not authorized"),
      @ApiResponse(responseCode = "401", description = "Not authenticated")
  })
  public ResponseEntity<Void> markMessagesAsRead(
      @Parameter(description = "Chat ID", required = true) @PathVariable String chatId, 
      Principal principal) {
    String currentUserId = principal.getName();
    chatService.markMessagesAsRead(chatId, currentUserId);
    return ResponseEntity.ok().build();
  }
}
