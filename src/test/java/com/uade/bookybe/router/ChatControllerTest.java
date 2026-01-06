package com.uade.bookybe.router;

import com.uade.bookybe.core.model.Chat;
import com.uade.bookybe.core.model.Message;
import com.uade.bookybe.core.usecase.ChatService;
import com.uade.bookybe.infraestructure.repository.MessageRepository;
import com.uade.bookybe.router.dto.chat.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private Principal principal;

    @InjectMocks
    private ChatController chatController;

    private Chat testChat;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        testChat = Chat.builder()
                .id("chat123")
                .user1Id("user123")
                .user2Id("user456")
                .build();

        testMessage = Message.builder()
                .id("msg123")
                .chatId("chat123")
                .senderId("user123")
                .content("Test message")
                .read(false)
                .build();

        when(principal.getName()).thenReturn("user123");
    }

    @Test
    void createOrGetChat_Success() {
        // Arrange
        CreateChatRequestDto dto = new CreateChatRequestDto();
        dto.setOtherUserId("user456");

        when(chatService.createOrGetChat("user123", "user456")).thenReturn(Optional.of(testChat));
        when(messageRepository.countUnreadMessagesByChatIdAndUserId("chat123", "user123")).thenReturn(0L);

        // Act
        ResponseEntity<ChatDto> response = chatController.createOrGetChat(dto, principal);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(chatService).createOrGetChat("user123", "user456");
    }

    @Test
    void createOrGetChat_BadRequest() {
        // Arrange
        CreateChatRequestDto dto = new CreateChatRequestDto();
        dto.setOtherUserId("user456");

        when(chatService.createOrGetChat("user123", "user456")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<ChatDto> response = chatController.createOrGetChat(dto, principal);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getUserChats_Success() {
        // Arrange
        List<Chat> chats = Arrays.asList(testChat);
        when(chatService.getUserChats("user123")).thenReturn(chats);
        when(messageRepository.countUnreadMessagesByChatIdAndUserId(anyString(), anyString())).thenReturn(2L);

        // Act
        ResponseEntity<List<ChatDto>> response = chatController.getUserChats(principal);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(chatService).getUserChats("user123");
    }

    @Test
    void getChatById_Success() {
        // Arrange
        when(chatService.getChatById("chat123", "user123")).thenReturn(Optional.of(testChat));
        when(messageRepository.countUnreadMessagesByChatIdAndUserId("chat123", "user123")).thenReturn(1L);

        // Act
        ResponseEntity<ChatDto> response = chatController.getChatById("chat123", principal);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(chatService).getChatById("chat123", "user123");
    }

    @Test
    void getChatById_NotFound() {
        // Arrange
        when(chatService.getChatById("chat123", "user123")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<ChatDto> response = chatController.getChatById("chat123", principal);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getChatMessages_Success() {
        // Arrange
        List<Message> messages = Arrays.asList(testMessage);
        when(chatService.getChatMessages("chat123", "user123")).thenReturn(messages);

        // Act
        ResponseEntity<List<MessageDto>> response = chatController.getChatMessages("chat123", principal);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(chatService).getChatMessages("chat123", "user123");
    }

    @Test
    void sendMessage_Success() {
        // Arrange
        SendMessageRequestDto dto = new SendMessageRequestDto();
        dto.setContent("Hello world");

        when(chatService.sendMessage(anyString(), anyString(), anyString(), any()))
                .thenReturn(Optional.of(testMessage));

        // Act
        ResponseEntity<MessageDto> response = chatController.sendMessage("chat123", dto, principal);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(chatService).sendMessage("chat123", "user123", "Hello world", null);
    }

    @Test
    void sendMessage_Failed() {
        // Arrange
        SendMessageRequestDto dto = new SendMessageRequestDto();
        dto.setContent("Hello world");

        when(chatService.sendMessage(anyString(), anyString(), anyString(), any()))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<MessageDto> response = chatController.sendMessage("chat123", dto, principal);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void markMessagesAsRead_Success() {
        // Arrange
        doNothing().when(chatService).markMessagesAsRead(anyString(), anyString());

        // Act
        ResponseEntity<Void> response = chatController.markMessagesAsRead("chat123", principal);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(chatService).markMessagesAsRead("chat123", "user123");
    }
}

