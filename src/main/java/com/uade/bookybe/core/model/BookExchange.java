package com.uade.bookybe.core.model;

import com.uade.bookybe.core.model.constant.ExchangeStatus;
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
public class BookExchange {
  private String id;
  private String requesterId;
  private String ownerId;
  private ExchangeStatus status;
  private LocalDateTime dateCreated;
  private LocalDateTime dateUpdated;
  private List<String> ownerBookIds;
  private List<String> requesterBookIds;
  private String chatId;
  private List<UserBook> ownerBooks;
  private List<UserBook> requesterBooks;
  private UserRate requesterRate;
  private UserRate ownerRate;
}
