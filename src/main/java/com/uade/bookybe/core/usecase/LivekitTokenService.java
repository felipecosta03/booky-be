package com.uade.bookybe.core.usecase;

import com.uade.bookybe.core.model.LivekitToken;

public interface LivekitTokenService {
    LivekitToken createToken(String roomName, String participantName, String participantId, 
                           LivekitToken.TokenPermissions permissions);
    
    String createJoinToken(String roomName, String participantId, boolean canPublish, boolean canSubscribe);
}