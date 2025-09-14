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
public class MeetingStatusResponseDto {
    private boolean isActive;
    private int participantCount;
    private LocalDateTime startedAt;
    private String roomName;
    private boolean exists;
}
