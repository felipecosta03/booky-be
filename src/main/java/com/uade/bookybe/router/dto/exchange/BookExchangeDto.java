package com.uade.bookybe.router.dto.exchange;

import com.uade.bookybe.core.model.constant.ExchangeStatus;
import com.uade.bookybe.router.dto.user.UserPreviewDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BookExchangeDto {
  private String id;
  private String requesterId;
  private String ownerId;
  private ExchangeStatus status;
  private LocalDateTime dateCreated;
  private LocalDateTime dateUpdated;
  private List<String> ownerBookIds;
  private List<String> requesterBookIds;
  private UserPreviewDto requester;
  private UserPreviewDto owner;
} 