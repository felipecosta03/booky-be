package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.config.LivekitProps;
import com.uade.bookybe.core.model.LivekitToken;
import com.uade.bookybe.core.usecase.LivekitTokenService;
import io.livekit.server.AccessToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultLivekitTokenService implements LivekitTokenService {

    private final LivekitProps livekitProps;

    @Override
    public LivekitToken createToken(String roomName, String participantName, String participantId, 
                                  LivekitToken.TokenPermissions permissions) {
        try {
            String tokenString = createJoinToken(roomName, participantId, 
                permissions.isCanPublish(), permissions.isCanSubscribe());

            return LivekitToken.builder()
                .token(tokenString)
                .roomName(roomName)
                .participantName(participantName)
                .participantId(participantId)
                .isModerator(permissions.isModerator())
                .permissions(permissions)
                .build();

        } catch (Exception e) {
            log.error("Error creating LiveKit token", e);
            throw new RuntimeException("Failed to create access token", e);
        }
    }

    @Override
    public String createJoinToken(String roomName, String participantId, boolean canPublish, boolean canSubscribe) {
        try {
            AccessToken token = new AccessToken(
                livekitProps.getApiKey(),
                livekitProps.getApiSecret()
            );

            // Set participant identity
            token.setIdentity(participantId);

            // Add room join grant and video grants for publish/subscribe
            token.addGrants(
                new io.livekit.server.RoomJoin(true),
                new io.livekit.server.RoomName(roomName),
                new io.livekit.server.CanPublish(canPublish),
                new io.livekit.server.CanSubscribe(canSubscribe)
            );

            // Set token expiration using configured TTL
            token.setExpiration(Date.from(Instant.now().plusSeconds(livekitProps.getTokenTtlSeconds())));

            return token.toJwt();

        } catch (Exception e) {
            log.error("Error creating LiveKit join token", e);
            throw new RuntimeException("Failed to create join token", e);
        }
    }
}