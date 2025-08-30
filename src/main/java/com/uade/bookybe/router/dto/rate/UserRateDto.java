package com.uade.bookybe.router.dto.rate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserRateDto {
    private String id;
    private String userId;
    private String exchangeId;
    private Integer rating;
    private String comment;
    private LocalDateTime dateCreated;
}
