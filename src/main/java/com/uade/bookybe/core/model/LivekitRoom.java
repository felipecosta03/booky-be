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
public class LivekitRoom {
    private String name;
    private String readingClubId;
    private String moderatorId;
    private boolean isActive;
    private int participantCount;
    private LocalDateTime createdAt;
    private RoomOptions options;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoomOptions {
        @Builder.Default
        private int emptyTimeout = 600; // 10 minutes
        
        @Builder.Default
        private int maxParticipants = 50;
        
        private String metadata;

    }
}
