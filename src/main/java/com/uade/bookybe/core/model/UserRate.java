package com.uade.bookybe.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRate {
    private String id;
    private String userId;
    private String exchangeId;
    private Integer rating; // 1-5
    private String comment;
    private LocalDateTime dateCreated;
}
