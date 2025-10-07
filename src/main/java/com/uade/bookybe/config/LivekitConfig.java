package com.uade.bookybe.config;

import io.livekit.server.RoomServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
public class LivekitConfig {

    private final LivekitProps livekitProps;

    @Bean
    public RoomServiceClient roomServiceClient() {
        return RoomServiceClient.create(
            livekitProps.getWsUrl(), 
            livekitProps.getApiKey(), 
            livekitProps.getApiSecret()
        );
    }
}
