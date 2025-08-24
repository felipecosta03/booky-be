package com.uade.bookybe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JwtService {

    @Value("${app.security.jwt.secret:booky-secret-key-for-jwt-tokens-2024}")
    private String SECRET_KEY;
    
    @Value("${app.security.jwt.expiration:86400000}")
    private long EXPIRATION_TIME;

    public String generateToken(String userId, String email) {
        try {
            // Create header
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            // Create payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", userId);
            payload.put("email", email);
            payload.put("iat", new Date().getTime());
            payload.put("exp", new Date().getTime() + EXPIRATION_TIME);

            ObjectMapper mapper = new ObjectMapper();
            String headerJson = mapper.writeValueAsString(header);
            String payloadJson = mapper.writeValueAsString(payload);

            // Encode header and payload
            String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            // Create signature
            String data = encodedHeader + "." + encodedPayload;
            String signature = createSignature(data);

            return encodedHeader + "." + encodedPayload + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Error generating JWT token", e);
        }
    }

    private String createSignature(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception e) {
            throw new RuntimeException("Error creating signature", e);
        }
    }

    /**
     * Validates a JWT token and returns true if valid
     */
    public boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }

            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            // Verify signature
            String data = parts[0] + "." + parts[1];
            String expectedSignature = createSignature(data);
            
            if (!expectedSignature.equals(parts[2])) {
                log.warn("JWT signature validation failed");
                return false;
            }

            // Check expiration
            Map<String, Object> payload = getPayloadFromToken(token);
            if (payload == null) {
                return false;
            }

            Object expObj = payload.get("exp");
            if (expObj == null) {
                return false;
            }

            long exp = ((Number) expObj).longValue();
            long currentTime = new Date().getTime();
            
            if (currentTime > exp) {
                log.warn("JWT token has expired");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating JWT token", e);
            return false;
        }
    }

    /**
     * Extracts user ID from JWT token
     */
    public String getUserIdFromToken(String token) {
        try {
            Map<String, Object> payload = getPayloadFromToken(token);
            if (payload != null) {
                return (String) payload.get("sub");
            }
        } catch (Exception e) {
            log.error("Error extracting user ID from token", e);
        }
        return null;
    }

    /**
     * Extracts email from JWT token
     */
    public String getEmailFromToken(String token) {
        try {
            Map<String, Object> payload = getPayloadFromToken(token);
            if (payload != null) {
                return (String) payload.get("email");
            }
        } catch (Exception e) {
            log.error("Error extracting email from token", e);
        }
        return null;
    }

    /**
     * Extracts the payload from a JWT token
     */
    private Map<String, Object> getPayloadFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String encodedPayload = parts[1];
            byte[] decodedPayload = Base64.getUrlDecoder().decode(encodedPayload);
            String payloadJson = new String(decodedPayload, StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> result = mapper.readValue(payloadJson, Map.class);
            return result;
        } catch (Exception e) {
            log.error("Error extracting payload from token", e);
            return null;
        }
    }

    /**
     * Extracts token from Authorization header
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
} 