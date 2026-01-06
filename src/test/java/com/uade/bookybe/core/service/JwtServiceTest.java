package com.uade.bookybe.core.service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

  private JwtService sut;

  @BeforeEach
  void setUp() throws Exception {
    sut = new JwtService();

    // Inyectar valores de @Value via reflection (no hay constructor y no estamos usando Spring en
    // unit tests)
    setField(sut, "SECRET_KEY", "test-secret-key");
    setField(sut, "EXPIRATION_TIME", 60_000L); // 1 minuto
  }

  // ---------------- generateToken ----------------

  @Test
  void generateToken_deberiaGenerarJwt_conTresPartes_yBearerCompatible() {
    // when
    String token = sut.generateToken("u1", "u1@mail.com");

    // then
    assertNotNull(token);
    String[] parts = token.split("\\.");
    assertEquals(3, parts.length);

    // header/payload base64url sin padding => decode OK
    assertDoesNotThrow(() -> Base64.getUrlDecoder().decode(parts[0]));
    assertDoesNotThrow(() -> Base64.getUrlDecoder().decode(parts[1]));
    assertNotNull(parts[2]);
    assertFalse(parts[2].isBlank());
  }

  @Test
  void validateToken_deberiaRetornarTrue_paraTokenGenerado() {
    // given
    String token = sut.generateToken("u1", "u1@mail.com");

    // when
    boolean valid = sut.validateToken(token);

    // then
    assertTrue(valid);
  }

  @Test
  void getUserIdFromToken_deberiaExtraerSub() {
    // given
    String token = sut.generateToken("u123", "a@b.com");

    // when
    String userId = sut.getUserIdFromToken(token);

    // then
    assertEquals("u123", userId);
  }

  @Test
  void getEmailFromToken_deberiaExtraerEmail() {
    // given
    String token = sut.generateToken("u123", "a@b.com");

    // when
    String email = sut.getEmailFromToken(token);

    // then
    assertEquals("a@b.com", email);
  }

  // ---------------- validateToken ----------------

  @Test
  void validateToken_deberiaRetornarFalse_siTokenNullOVacio() {
    assertFalse(sut.validateToken(null));
    assertFalse(sut.validateToken(""));
    assertFalse(sut.validateToken("   "));
  }

  @Test
  void validateToken_deberiaRetornarFalse_siFormatoIncorrecto() {
    assertFalse(sut.validateToken("a.b")); // 2 partes
    assertFalse(sut.validateToken("a.b.c.d")); // 4 partes
    assertFalse(sut.validateToken("solo-una-parte"));
  }

  @Test
  void validateToken_deberiaRetornarFalse_siFirmaAlterada() {
    // given
    String token = sut.generateToken("u1", "u1@mail.com");
    String[] parts = token.split("\\.");
    assertEquals(3, parts.length);

    // Altero la firma (3ra parte)
    String tampered = parts[0] + "." + parts[1] + "." + parts[2] + "x";

    // when
    boolean valid = sut.validateToken(tampered);

    // then
    assertFalse(valid);
  }

  @Test
  void validateToken_deberiaRetornarFalse_siSecretKeyDiferente() throws Exception {
    // given
    String token = sut.generateToken("u1", "u1@mail.com");

    // cambio el secret => la firma esperada no coincide
    setField(sut, "SECRET_KEY", "other-secret");

    // when
    boolean valid = sut.validateToken(token);

    // then
    assertFalse(valid);
  }

  @Test
  void validateToken_deberiaRetornarFalse_siTokenExpirado() throws Exception {
    // given
    setField(sut, "EXPIRATION_TIME", -1L); // exp en el pasado
    String expiredToken = sut.generateToken("u1", "u1@mail.com");

    // when
    boolean valid = sut.validateToken(expiredToken);

    // then
    assertFalse(valid);
  }

  @Test
  void validateToken_deberiaRetornarFalse_siPayloadNoEsJsonValido() {
    // given: armo token con payload inválido pero con firma correcta (como createSignature es
    // interno,
    // lo más simple es generar uno válido y corromper el payload; eso invalida firma => false
    // igual,
    // pero cubre el camino de error/false)
    String token = sut.generateToken("u1", "u1@mail.com");
    String[] parts = token.split("\\.");
    String badPayload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("not-json".getBytes(StandardCharsets.UTF_8));

    String tampered = parts[0] + "." + badPayload + "." + parts[2];

    // when
    boolean valid = sut.validateToken(tampered);

    // then
    assertFalse(valid);
  }

  @Test
  void getUserIdFromToken_deberiaRetornarNull_siTokenInvalido() {
    assertNull(sut.getUserIdFromToken("a.b.c"));
  }

  @Test
  void getEmailFromToken_deberiaRetornarNull_siTokenInvalido() {
    assertNull(sut.getEmailFromToken("a.b.c"));
  }

  // ---------------- extractTokenFromHeader ----------------

  @Test
  void extractTokenFromHeader_deberiaExtraerToken_siBearer() {
    assertEquals("abc", sut.extractTokenFromHeader("Bearer abc"));
    assertEquals("a.b.c", sut.extractTokenFromHeader("Bearer a.b.c"));
  }

  // ---------------- helpers ----------------

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(target, value);
  }
}
