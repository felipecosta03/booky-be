package com.uade.bookybe.infraestructure.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "chats")
@ToString
public class ChatEntity {
  @Id private String id;

  @Column(name = "user1_id", nullable = false)
  private String user1Id;

  @Column(name = "user2_id", nullable = false)
  private String user2Id;

  @Column(name = "date_created", nullable = false)
  private LocalDateTime dateCreated;

  @Column(name = "date_updated", nullable = false)
  private LocalDateTime dateUpdated;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user1_id", insertable = false, updatable = false)
  private UserEntity user1;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user2_id", insertable = false, updatable = false)
  private UserEntity user2;

  @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<MessageEntity> messages;
}
