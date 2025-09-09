package com.uade.bookybe.core.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Chat {
  private String id;
  private String user1Id;
  private String user2Id;
  private LocalDateTime dateCreated;
  private LocalDateTime dateUpdated;
  private User user1;
  private User user2;
  private List<Message> messages;
  private Message lastMessage;
}
