package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.config.LivekitProps;
import com.uade.bookybe.core.usecase.LivekitTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultLivekitTokenService implements LivekitTokenService {
  private final LivekitProps props;

  public String createJoinToken(
      String room, String identity, boolean canPublish, boolean canSubscribe) {
    long now = System.currentTimeMillis();
    Date iat = new Date(now);
    Date exp = new Date(now + props.getTokenTtlSeconds() * 1000);

    // Grant de LiveKit
    Map<String, Object> videoGrant = new HashMap<>();
    videoGrant.put("room", room);
    videoGrant.put("roomJoin", true);
    videoGrant.put("canPublish", canPublish);
    videoGrant.put("canSubscribe", canSubscribe);

    Map<String, Object> grants = new HashMap<>();
    grants.put("video", videoGrant);
    grants.put("identity", identity); // quién sos
    // Optional: name visible en la sala
    // grants.put("name", "Felipe");

    SecretKey key = Keys.hmacShaKeyFor(props.getApiSecret().getBytes(StandardCharsets.UTF_8));

    // "iss" = apiKey; "sub" opcional; "nbf" opcional
    return Jwts.builder()
        .header()
        .add("typ", "JWT")
        .and()
        .claim("iss", props.getApiKey())
        .claim("nbf", (now / 1000) - 10) // tolerancia reloj
        .claim("grants", grants)
        .issuedAt(iat)
        .expiration(exp)
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }
}
