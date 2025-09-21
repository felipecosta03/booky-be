package com.uade.bookybe.infraestructure.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "messages")
public class MessageEntity {
  @Id private String id;

  @Column(name = "chat_id", nullable = false)
  private String chatId;

  @Column(name = "sender_id", nullable = false)
  private String senderId;

  @Column(name = "content", nullable = false, length = 1000)
  private String content;

  @Column(name = "image", length = 100000)
  private String image; // base64

  @Column(name = "date_sent", nullable = false)
  private LocalDateTime dateSent;

  @Column(name = "is_read", nullable = false)
  private boolean read;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_id", insertable = false, updatable = false)
  private ChatEntity chat;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id", insertable = false, updatable = false)
  private UserEntity sender;
}
