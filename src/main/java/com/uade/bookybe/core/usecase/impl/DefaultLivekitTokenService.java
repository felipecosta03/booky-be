package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.config.LivekitProps;
import com.uade.bookybe.core.model.LivekitToken;
import com.uade.bookybe.core.usecase.LivekitTokenService;
import io.livekit.server.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultLivekitTokenService implements LivekitTokenService {

  private final LivekitProps livekitProps;

  @PostConstruct
  public void init() {
    if (livekitProps.getApiKey() == null || livekitProps.getApiSecret() == null) {
      log.error("Error: LIVEKIT_API_KEY o SECRET no configurados en el entorno!");
    } else {
      log.info("LiveKit configurado correctamente");
    }
  }

  @Override
  public LivekitToken createToken(
      String roomName,
      String participantName,
      String participantId,
      LivekitToken.TokenPermissions permissions) {
    try {
      log.info(
          "🎯 Generando token LiveKit para: {} (ID: {}) en sala: {}",
          participantName,
          participantId,
          roomName);

      String tokenString = generateToken(roomName, participantId, participantName, permissions);

      log.info("✅ Token LiveKit generado exitosamente para: {}", participantName);

      return LivekitToken.builder()
          .token(tokenString)
          .roomName(roomName)
          .participantName(participantName)
          .participantId(participantId)
          .isModerator(permissions.isModerator())
          .permissions(permissions)
          .build();

    } catch (Exception e) {
      throw new RuntimeException("Error al generar token de video", e);
    }
  }

  @Override
  public String createJoinToken(
      String roomName, String participantId, boolean canPublish, boolean canSubscribe) {
    LivekitToken.TokenPermissions permissions =
        LivekitToken.TokenPermissions.builder()
            .canPublish(canPublish)
            .canSubscribe(canSubscribe)
            .canPublishData(true)
            .isModerator(false)
            .canRecord(false)
            .build();

    return generateToken(roomName, participantId, participantId, permissions);
  }

  /**
   * Genera un token JWT para LiveKit con los permisos especificados. Esta implementación usa la
   * librería oficial de LiveKit para Java.
   *
   * <p>Equivalente a la implementación en React Native/TypeScript: const token = new
   * AccessToken(apiKey, apiSecret, { identity: userId, name: userName }); token.addGrant({
   * roomJoin: true, room: roomName, canPublish: true, canSubscribe: true, canPublishData: true });
   */
  private String generateToken(
      String roomName, String userId, String userName, LivekitToken.TokenPermissions permissions) {
    try {
      // 1. Crear el AccessToken con las credenciales
      AccessToken token = new AccessToken(livekitProps.getApiKey(), livekitProps.getApiSecret());

      // 2. Configurar identidad y nombre del participante
      token.setIdentity(userId);
      token.setName(userName);

      // 3. Configurar los permisos (Grants)
      // La API de Java usa addGrants() con diferentes tipos de Grant
      token.addGrants(new RoomJoin(true)); // Permitir unirse a la sala
      token.addGrants(new RoomName(roomName)); // Nombre de la sala
      token.addGrants(new CanPublish(permissions.isCanPublish())); // Audio/Video
      token.addGrants(new CanSubscribe(permissions.isCanSubscribe())); // Ver a otros
      token.addGrants(new CanPublishData(permissions.isCanPublishData())); // Chat y datos

      // Permisos adicionales para moderadores
      if (permissions.isModerator()) {
        token.addGrants(new RoomAdmin(true)); // Administrador de sala
      }

      // 4. Generar el JWT firmado
      return token.toJwt();

    } catch (Exception e) {
      throw new RuntimeException("Error al generar token de video", e);
    }
  }
}
