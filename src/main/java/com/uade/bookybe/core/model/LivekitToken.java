package com.uade.bookybe.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LivekitToken {
    private String token;
    private String roomName;
    private String participantName;
    private String participantId;
    private boolean isModerator;
    private TokenPermissions permissions;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenPermissions {
        @Builder.Default
        private boolean canPublish = true;
        
        @Builder.Default
        private boolean canSubscribe = true;
        
        @Builder.Default
        private boolean canPublishData = true;
        
        @Builder.Default
        private boolean isModerator = false;
        
        @Builder.Default
        private boolean canRecord = false;

        public TokenPermissions(boolean isModerator) {
            this.isModerator = isModerator;
            this.canRecord = isModerator;
            this.canPublish = true;
            this.canSubscribe = true;
            this.canPublishData = true;
        }
    }
}
