package com.uade.bookybe.router.dto.meeting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeetingActionResponseDto {
    private boolean success;
    private String roomName;
    private LocalDateTime timestamp;
    private Long duration; // Only for end meeting
    private String message;
}
