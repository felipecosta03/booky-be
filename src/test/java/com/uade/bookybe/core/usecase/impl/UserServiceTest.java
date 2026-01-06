package com.uade.bookybe.core.usecase.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

import com.uade.bookybe.core.exception.ConflictException;
import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.model.User;
import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.core.port.ImageStoragePort;
import com.uade.bookybe.core.usecase.GamificationService;
import com.uade.bookybe.core.usecase.UserRateService;
import com.uade.bookybe.infraestructure.entity.AddressEntity;
import com.uade.bookybe.infraestructure.entity.UserEntity;
import com.uade.bookybe.infraestructure.repository.UserBookRepository;
import com.uade.bookybe.infraestructure.repository.UserRepository;
import com.uade.bookybe.router.dto.user.UserPreviewDto;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private UserBookRepository userBookRepository;
  @Mock private BCryptPasswordEncoder passwordEncoder;
  @Mock private ImageStoragePort imageStoragePort;
  @Mock private GamificationService gamificationService;
  @Mock private UserRateService userRateService;

  @InjectMocks private UserServiceImpl sut;

  // ---------------- getUserById ----------------

  @Test
  void getUserById_deberiaMapearSiExiste() {
    // given
    UserEntity entity = UserEntity.builder().id("u1").email("a@a.com").build();
    given(userRepository.findById("u1")).willReturn(Optional.of(entity));

    // when
    Optional<User> result = sut.getUserById("u1");

    // then
    assertTrue(result.isPresent());
    assertEquals("u1", result.get().getId());
  }

  @Test
  void getUserById_deberiaRetornarEmptySiNoExiste() {
    // given
    given(userRepository.findById("u1")).willReturn(Optional.empty());

    // when
    Optional<User> result = sut.getUserById("u1");

    // then
    assertTrue(result.isEmpty());
  }

  // ---------------- updateUser ----------------

  @Test
  void updateUser_deberiaLanzarNotFoundSiNoExiste() {
    // given
    given(userRepository.findById("u1")).willReturn(Optional.empty());

    // when + then
    assertThrows(
        NotFoundException.class,
        () -> sut.updateUser("u1", User.builder().name("n").build(), null));
  }

  @Test
  void updateUser_deberiaActualizarCamposBasicos_yGuardar() {
    // given
    UserEntity existing =
        UserEntity.builder()
            .id("u1")
            .name("old")
            .lastname("oldL")
            .description("oldD")
            .image(null)
            .password("hash")
            .build();

    given(userRepository.findById("u1")).willReturn(Optional.of(existing));
    given(userRepository.save(any(UserEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, UserEntity.class));

    User input = User.builder().name("new").lastname("newL").description("newD").build();

    // when
    Optional<User> result = sut.updateUser("u1", input, null);

    // then
    assertTrue(result.isPresent());
    then(userRepository)
        .should()
        .save(
            argThat(
                e ->
                    "new".equals(e.getName())
                        && "newL".equals(e.getLastname())
                        && "newD".equals(e.getDescription())));
    then(imageStoragePort).shouldHaveNoInteractions();
    then(passwordEncoder).should(never()).encode(anyString());
  }

  @Test
  void updateUser_deberiaActualizarAddress_siSeProvee() {
    // given
    UserEntity existing = UserEntity.builder().id("u1").build();
    given(userRepository.findById("u1")).willReturn(Optional.of(existing));
    given(userRepository.save(any(UserEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, UserEntity.class));

    User input =
        User.builder()
            .address(
                com.uade.bookybe.core.model.Address.builder()
                    .city("CABA")
                    .state("BA")
                    .country("AR")
                    .latitude(-34.6)
                    .longitude(-58.4)
                    .build())
            .build();

    // when
    Optional<User> result = sut.updateUser("u1", input, null);

    // then
    assertTrue(result.isPresent());
    then(userRepository)
        .should()
        .save(
            argThat(
                e ->
                    e.getAddress() != null
                        && "CABA".equals(e.getAddress().getCity())
                        && "BA".equals(e.getAddress().getState())
                        && "AR".equals(e.getAddress().getCountry())
                        && Double.valueOf(-34.6).equals(e.getAddress().getLatitude())
                        && Double.valueOf(-58.4).equals(e.getAddress().getLongitude())));
  }

  @Test
  void updateUser_deberiaBorrarImagenAnterior_ySubirNueva_siImageBase64Presente() {
    // given
    UserEntity existing = UserEntity.builder().id("u1").image("old-url").build();

    given(userRepository.findById("u1")).willReturn(Optional.of(existing));

    given(imageStoragePort.uploadImage("b64", "booky/users")).willReturn(Optional.of("new-url"));

    given(userRepository.save(any(UserEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, UserEntity.class));

    // when
    Optional<User> result = sut.updateUser("u1", User.builder().build(), "b64");

    // then
    assertTrue(result.isPresent());
    then(imageStoragePort).should().deleteImage("old-url");
    then(imageStoragePort).should().uploadImage("b64", "booky/users");
    then(userRepository).should().save(argThat(e -> "new-url".equals(e.getImage())));
  }

  @Test
  void updateUser_noDeberiaBorrarImagenAnterior_siNoHabiaImagen() {
    // given
    UserEntity existing = UserEntity.builder().id("u1").image(null).build();
    given(userRepository.findById("u1")).willReturn(Optional.of(existing));

    given(imageStoragePort.uploadImage("b64", "booky/users")).willReturn(Optional.of("new-url"));
    given(userRepository.save(any(UserEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, UserEntity.class));

    // when
    sut.updateUser("u1", User.builder().build(), "b64");

    // then
    then(imageStoragePort).should(never()).deleteImage(anyString());
    then(userRepository).should().save(argThat(e -> "new-url".equals(e.getImage())));
  }

  @Test
  void updateUser_deberiaMantenerImagenAnterior_siUploadFalla() {
    // given
    UserEntity existing = UserEntity.builder().id("u1").image("old-url").build();
    given(userRepository.findById("u1")).willReturn(Optional.of(existing));

    given(imageStoragePort.uploadImage("b64", "booky/users")).willReturn(Optional.empty());

    given(userRepository.save(any(UserEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, UserEntity.class));

    // when
    sut.updateUser("u1", User.builder().build(), "b64");

    // then
    then(imageStoragePort).should().deleteImage("old-url");
    then(userRepository).should().save(argThat(e -> "old-url".equals(e.getImage())));
  }

  @Test
  void updateUser_deberiaSetearImagenDirecta_siImageBase64Vacio_yUserTraeImage() {
    // given
    UserEntity existing = UserEntity.builder().id("u1").image("old-url").build();
    given(userRepository.findById("u1")).willReturn(Optional.of(existing));
    given(userRepository.save(any(UserEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, UserEntity.class));

    User input = User.builder().image("direct-url").build();

    // when
    sut.updateUser("u1", input, "   "); // blank

    // then
    then(imageStoragePort).shouldHaveNoInteractions();
    then(userRepository).should().save(argThat(e -> "direct-url".equals(e.getImage())));
  }

  @Test
  void updateUser_deberiaCodificarPassword_siSeProvee() {
    // given
    UserEntity existing = UserEntity.builder().id("u1").password("hash").build();
    given(userRepository.findById("u1")).willReturn(Optional.of(existing));

    given(passwordEncoder.encode("newpass")).willReturn("encoded");
    given(userRepository.save(any(UserEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, UserEntity.class));

    User input = User.builder().password("newpass").build();

    // when
    sut.updateUser("u1", input, null);

    // then
    then(passwordEncoder).should().encode("newpass");
    then(userRepository).should().save(argThat(e -> "encoded".equals(e.getPassword())));
  }

  // ---------------- deleteUser ----------------

  @Test
  void deleteUser_deberiaRetornarFalse_siNoExiste() {
    // given
    given(userRepository.existsById("u1")).willReturn(false);

    // when
    boolean result = sut.deleteUser("u1");

    // then
    assertFalse(result);
    then(userRepository).should(never()).deleteById(anyString());
  }

  @Test
  void deleteUser_deberiaBorrarUsuario_yRetornarTrue_aunSiGamificationFalla() {
    // given
    given(userRepository.existsById("u1")).willReturn(true);

    willThrow(new RuntimeException("boom"))
        .given(gamificationService)
        .deleteUserGamificationData("u1");

    willDoNothing().given(userRepository).deleteById("u1");

    // when
    boolean result = sut.deleteUser("u1");

    // then
    assertTrue(result);
    then(userRepository).should().deleteById("u1");
  }

  @Test
  void
      deleteUser_deberiaLanzarRuntimeConMensajeEspecifico_siDataIntegrityPorGamificationProfiles() {
    // given
    given(userRepository.existsById("u1")).willReturn(true);

    DataIntegrityViolationException dive =
        new DataIntegrityViolationException("fk violation on gamification_profiles");
    willThrow(dive).given(userRepository).deleteById("u1");

    // when + then
    RuntimeException ex = assertThrows(RuntimeException.class, () -> sut.deleteUser("u1"));
    assertTrue(ex.getMessage().toLowerCase().contains("gamification profile"));
  }

  @Test
  void deleteUser_deberiaLanzarRuntimeConMensajeEspecifico_siDataIntegrityPorUserAchievements() {
    // given
    given(userRepository.existsById("u1")).willReturn(true);

    DataIntegrityViolationException dive =
        new DataIntegrityViolationException("fk violation on user_achievements");
    willThrow(dive).given(userRepository).deleteById("u1");

    // when + then
    RuntimeException ex = assertThrows(RuntimeException.class, () -> sut.deleteUser("u1"));
    assertTrue(ex.getMessage().toLowerCase().contains("user achievements"));
  }

  @Test
  void deleteUser_deberiaLanzarRuntimeGenerico_siDataIntegrityOtroCaso() {
    // given
    given(userRepository.existsById("u1")).willReturn(true);

    DataIntegrityViolationException dive =
        new DataIntegrityViolationException("fk violation on something_else");
    willThrow(dive).given(userRepository).deleteById("u1");

    // when + then
    RuntimeException ex = assertThrows(RuntimeException.class, () -> sut.deleteUser("u1"));
    assertTrue(ex.getMessage().toLowerCase().contains("foreign key"));
  }

  @Test
  void deleteUser_deberiaLanzarRuntimeGenerico_siErrorInesperado() {
    // given
    given(userRepository.existsById("u1")).willReturn(true);

    willThrow(new RuntimeException("boom")).given(userRepository).deleteById("u1");

    // when + then
    RuntimeException ex = assertThrows(RuntimeException.class, () -> sut.deleteUser("u1"));
    assertTrue(ex.getMessage().contains("Failed to delete user"));
  }

  // ---------------- follow / unfollow ----------------

  @Test
  void followUser_deberiaRetornarFalse_siYaLoSigue() {
    // given
    given(userRepository.isFollowing("u1", "u2")).willReturn(true);

    // when
    boolean result = sut.followUser("u1", "u2");

    // then
    assertFalse(result);
    then(userRepository).should(never()).follow(anyString(), anyString());
  }

  @Test
  void followUser_deberiaSeguir_yRetornarTrue_siNoLoSigue() {
    // given
    given(userRepository.isFollowing("u1", "u2")).willReturn(false);

    // when
    boolean result = sut.followUser("u1", "u2");

    // then
    assertTrue(result);
    then(userRepository).should().follow("u1", "u2");
  }

  @Test
  void unfollowUser_deberiaRetornarFalse_siNoLoSigue() {
    // given
    given(userRepository.isFollowing("u1", "u2")).willReturn(false);

    // when
    boolean result = sut.unfollowUser("u1", "u2");

    // then
    assertFalse(result);
    then(userRepository).should(never()).unfollow(anyString(), anyString());
  }

  @Test
  void unfollowUser_deberiaDejarDeSeguir_yRetornarTrue_siLoSigue() {
    // given
    given(userRepository.isFollowing("u1", "u2")).willReturn(true);

    // when
    boolean result = sut.unfollowUser("u1", "u2");

    // then
    assertTrue(result);
    then(userRepository).should().unfollow("u1", "u2");
  }

  // ---------------- followers / following ----------------

  @Test
  void getFollowers_deberiaMapearLista() {
    // given
    given(userRepository.findFollowers("u1"))
        .willReturn(
            List.of(UserEntity.builder().id("u2").build(), UserEntity.builder().id("u3").build()));

    // when
    List<User> result = sut.getFollowers("u1");

    // then
    assertEquals(2, result.size());
    assertEquals("u2", result.get(0).getId());
    assertEquals("u3", result.get(1).getId());
  }

  @Test
  void getFollowing_deberiaMapearLista() {
    // given
    given(userRepository.findFollowing("u1"))
        .willReturn(List.of(UserEntity.builder().id("u2").build()));

    // when
    List<User> result = sut.getFollowing("u1");

    // then
    assertEquals(1, result.size());
    assertEquals("u2", result.get(0).getId());
  }

  // ---------------- signUp ----------------

  @Test
  void signUp_deberiaLanzarConflict_siEmailYaExiste() {
    // given
    UserSignUp dto =
        UserSignUp.builder()
            .email("a@a.com")
            .password("p")
            .username("u")
            .name("n")
            .lastname("l")
            .build();

    given(userRepository.findByEmail("a@a.com"))
        .willReturn(Optional.of(UserEntity.builder().id("u1").build()));

    // when + then
    assertThrows(ConflictException.class, () -> sut.signUp(dto));

    then(userRepository).should(never()).save(any());
    then(gamificationService).shouldHaveNoInteractions();
  }

  // ---------------- signIn ----------------

  @Test
  void signIn_deberiaRetornarEmpty_siNoExisteEmail() {
    // given
    given(userRepository.findByEmail("a@a.com")).willReturn(Optional.empty());

    // when
    Optional<User> result = sut.signIn("a@a.com", "p");

    // then
    assertTrue(result.isEmpty());
    then(passwordEncoder).should(never()).matches(anyString(), anyString());
  }

  @Test
  void signIn_deberiaRetornarUser_siPasswordMatch() {
    // given
    UserEntity entity = UserEntity.builder().id("u1").email("a@a.com").password("hash").build();
    given(userRepository.findByEmail("a@a.com")).willReturn(Optional.of(entity));
    given(passwordEncoder.matches("p", "hash")).willReturn(true);

    // when
    Optional<User> result = sut.signIn("a@a.com", "p");

    // then
    assertTrue(result.isPresent());
    assertEquals("u1", result.get().getId());
  }

  @Test
  void signIn_deberiaRetornarEmpty_siPasswordNoMatch() {
    // given
    UserEntity entity = UserEntity.builder().id("u1").email("a@a.com").password("hash").build();
    given(userRepository.findByEmail("a@a.com")).willReturn(Optional.of(entity));
    given(passwordEncoder.matches("p", "hash")).willReturn(false);

    // when
    Optional<User> result = sut.signIn("a@a.com", "p");

    // then
    assertTrue(result.isEmpty());
  }

  // ---------------- searchUsersByUsername ----------------

  @Test
  void searchUsersByUsername_deberiaMapearLista() {
    // given
    given(userRepository.findByUsernameContainingIgnoreCase("pe"))
        .willReturn(
            List.of(
                UserEntity.builder().id("u1").username("pepe").build(),
                UserEntity.builder().id("u2").username("peter").build()));

    // when
    List<User> result = sut.searchUsersByUsername("pe");

    // then
    assertEquals(2, result.size());
    assertEquals("u1", result.get(0).getId());
    assertEquals("u2", result.get(1).getId());
  }

  // ---------------- searchUsersByBooks ----------------

  @Test
  void searchUsersByBooks_deberiaMapearEnriquecerAddress_yRate() {
    // given
    List<Object[]> rows =
        List.of(
            new Object[] {"u2", "u2name", "N", "L", "img"},
            new Object[] {"u3", "u3name", "N3", "L3", null});

    given(userBookRepository.findUsersByBookIds(List.of("b1"), "u1", 1)).willReturn(rows);

    // requesting user SIN coords => no sort
    given(userRepository.findById("u1"))
        .willReturn(Optional.of(UserEntity.builder().id("u1").address(null).build()));

    // enrich address por cada user
    given(userRepository.findById("u2"))
        .willReturn(
            Optional.of(
                UserEntity.builder()
                    .id("u2")
                    .address(
                        AddressEntity.builder()
                            .city("CABA")
                            .state("BA")
                            .country("AR")
                            .latitude(-34.6)
                            .longitude(-58.4)
                            .build())
                    .build()));

    given(userRepository.findById("u3"))
        .willReturn(Optional.of(UserEntity.builder().id("u3").address(null).build()));

    // enrich rate por cada user
    given(userRateService.getUserAverageRating("u2")).willReturn(4.5);
    given(userRateService.getUserRatingCount("u2")).willReturn(10L);
    given(userRateService.getUserAverageRating("u3")).willReturn(null);
    given(userRateService.getUserRatingCount("u3")).willReturn(0L);

    // when
    List<UserPreviewDto> result = sut.searchUsersByBooks(List.of("b1"), "u1");

    // then
    assertEquals(2, result.size());
    UserPreviewDto u2 =
        result.stream().filter(u -> "u2".equals(u.getId())).findFirst().orElseThrow();
    assertNotNull(u2.getAddress());
    assertEquals("CABA", u2.getAddress().getCity());
    assertNotNull(u2.getUserRate());
    assertEquals(4.5, u2.getUserRate().getAverageRating());
    assertEquals(10L, u2.getUserRate().getTotalRatings());
  }

  @Test
  void searchUsersByBooks_deberiaOrdenarPorDistancia_siRequestingTieneCoords_yPonerNullsAlFinal() {
    // given
    List<Object[]> rows =
        List.of(
            new Object[] {"u2", "u2name", "N", "L", "img"},
            new Object[] {"u3", "u3name", "N3", "L3", null},
            new Object[] {"u4", "u4name", "N4", "L4", null});

    given(userBookRepository.findUsersByBookIds(List.of("b1"), "u1", 1)).willReturn(rows);

    // requesting user CON coords (0,0)
    given(userRepository.findById("u1"))
        .willReturn(
            Optional.of(
                UserEntity.builder()
                    .id("u1")
                    .address(AddressEntity.builder().latitude(0.0).longitude(0.0).build())
                    .build()));

    // u2 coords (1,1) dist sqrt2
    given(userRepository.findById("u2"))
        .willReturn(
            Optional.of(
                UserEntity.builder()
                    .id("u2")
                    .address(AddressEntity.builder().latitude(1.0).longitude(1.0).build())
                    .build()));

    // u3 coords (2,2) dist sqrt8 (más lejos)
    given(userRepository.findById("u3"))
        .willReturn(
            Optional.of(
                UserEntity.builder()
                    .id("u3")
                    .address(AddressEntity.builder().latitude(2.0).longitude(2.0).build())
                    .build()));

    // u4 sin coords => al final
    given(userRepository.findById("u4"))
        .willReturn(Optional.of(UserEntity.builder().id("u4").address(null).build()));

    // rate (no importa para orden, pero enrich lo llama)
    for (String id : List.of("u2", "u3", "u4")) {
      given(userRateService.getUserAverageRating(id)).willReturn(0.0);
      given(userRateService.getUserRatingCount(id)).willReturn(0L);
    }

    // when
    List<UserPreviewDto> result = sut.searchUsersByBooks(List.of("b1"), "u1");

    // then
    assertEquals(3, result.size());
    assertEquals("u2", result.get(0).getId()); // más cercano
    assertEquals("u3", result.get(1).getId()); // más lejos
    assertEquals("u4", result.get(2).getId()); // sin coords al final
  }

  // ---------------- searchUsersByLocation ----------------
  // Nota: este método usa UserDtoMapper.INSTANCE.toPreviewDto(), que es estático y no se mockea
  // fácil.
  // Testeamos que delega al repo y retorna tamaño esperado.

  @Test
  void searchUsersByLocation_deberiaDelegarRepo_yRetornarLista() {
    // given
    given(userRepository.findUsersByLocationBounds(1.0, 2.0, 3.0, 4.0))
        .willReturn(
            List.of(
                UserEntity.builder().id("u1").username("a").build(),
                UserEntity.builder().id("u2").username("b").build()));

    // when
    List<UserPreviewDto> result = sut.searchUsersByLocation(1.0, 2.0, 3.0, 4.0);

    // then
    assertEquals(2, result.size());
    then(userRepository).should().findUsersByLocationBounds(1.0, 2.0, 3.0, 4.0);
  }
}
