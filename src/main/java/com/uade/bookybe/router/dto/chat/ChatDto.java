package com.uade.bookybe.router.dto.chat;

import com.uade.bookybe.router.dto.user.UserPreviewDto;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatDto {
  private String id;
  private String user1Id;
  private String user2Id;
  private LocalDateTime dateCreated;
  private LocalDateTime dateUpdated;
  private UserPreviewDto user1;
  private UserPreviewDto user2;
  private List<MessageDto> messages;
  private MessageDto lastMessage;
  private long unreadCount;
}
