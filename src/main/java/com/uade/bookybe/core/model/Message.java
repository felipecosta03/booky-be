package com.uade.bookybe.core.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message {
  private String id;
  private String chatId;
  private String senderId;
  private String content;
  private LocalDateTime dateSent;
  private boolean read;
  private User sender;
}
