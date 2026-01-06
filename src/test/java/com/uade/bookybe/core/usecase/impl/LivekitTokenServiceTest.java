package com.uade.bookybe.core.usecase.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.uade.bookybe.config.LivekitProps;
import com.uade.bookybe.core.model.LivekitToken;
import io.livekit.server.AccessToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultLivekitTokenServiceTest {

  @Mock private LivekitProps livekitProps;

  @Spy @InjectMocks private DefaultLivekitTokenService sut;

  // ---------------- createToken ----------------

  @Test
  void createToken_deberiaConstruirLivekitToken_conCamposCorrectos() {
    // given
    String roomName = "room-1";
    String participantName = "Felipe";
    String participantId = "u1";
    LivekitToken.TokenPermissions permissions =
        LivekitToken.TokenPermissions.builder()
            .canPublish(true)
            .canSubscribe(false)
            .isModerator(true)
            .build();

    doReturn("jwt-123")
        .when(sut)
        .createJoinToken(eq(roomName), eq(participantId), eq(true), eq(false));

    // when
    LivekitToken token = sut.createToken(roomName, participantName, participantId, permissions);

    // then
    assertNotNull(token);
    assertEquals("jwt-123", token.getToken());
    assertEquals(roomName, token.getRoomName());
    assertEquals(participantName, token.getParticipantName());
    assertEquals(participantId, token.getParticipantId());
    assertTrue(token.isModerator());
    assertEquals(permissions, token.getPermissions());

    then(sut).should().createJoinToken(eq(roomName), eq(participantId), eq(true), eq(false));
  }

  @Test
  void createToken_deberiaLanzarRuntimeException_siCreateJoinTokenFalla() {
    // given
    LivekitToken.TokenPermissions permissions =
        LivekitToken.TokenPermissions.builder()
            .canPublish(true)
            .canSubscribe(true)
            .isModerator(false)
            .build();

    doThrow(new RuntimeException("boom"))
        .when(sut)
        .createJoinToken(anyString(), anyString(), anyBoolean(), anyBoolean());

    // when + then
    RuntimeException ex =
        assertThrows(
            RuntimeException.class, () -> sut.createToken("room-1", "name", "u1", permissions));
    assertTrue(ex.getMessage().toLowerCase().contains("failed"));
  }

  @Test
  void createJoinToken_deberiaLanzarRuntimeException_siSdkFalla() {
    // given
    given(livekitProps.getApiKey()).willReturn("api-key");
    given(livekitProps.getApiSecret()).willReturn("api-secret");
    given(livekitProps.getTokenTtlSeconds()).willReturn(60L);

    try (MockedConstruction<AccessToken> mocked =
        Mockito.mockConstruction(
            AccessToken.class,
            (mock, ctx) -> given(mock.toJwt()).willThrow(new RuntimeException("sdk error")))) {

      // when + then
      RuntimeException ex =
          assertThrows(
              RuntimeException.class, () -> sut.createJoinToken("room-1", "u1", true, true));
      assertTrue(ex.getMessage().toLowerCase().contains("failed"));
      assertNotNull(ex.getCause());
    }
  }
}
