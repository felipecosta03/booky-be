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
public class MeetingStatus {
    private boolean exists;
    private boolean isActive;
    private int participantCount;
    private LocalDateTime startedAt;
    private String roomName;
    private long creationTime;

    public MeetingStatus(boolean exists, boolean isActive, int participantCount, Long creationTime) {
        this.exists = exists;
        this.isActive = isActive;
        this.participantCount = participantCount;
        this.creationTime = creationTime != null ? creationTime : 0;
    }
}
