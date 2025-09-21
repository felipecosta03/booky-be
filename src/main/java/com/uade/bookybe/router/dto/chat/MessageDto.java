package com.uade.bookybe.router.dto.chat;

import com.uade.bookybe.router.dto.user.UserPreviewDto;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessageDto {
  private String id;
  private String chatId;
  private String senderId;
  private String content;
  private String image;
  private LocalDateTime dateSent;
  private boolean read;
  private UserPreviewDto sender;
}
