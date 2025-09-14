package com.uade.bookybe.router.dto.meeting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenResponseDto {
    private String token;
    private String roomName;
    private String participantName;
    private String participantId;
    private boolean isModerator;
}
