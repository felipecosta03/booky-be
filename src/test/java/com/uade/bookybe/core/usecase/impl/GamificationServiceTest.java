package com.uade.bookybe.core.usecase.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

import com.uade.bookybe.core.model.*;
import com.uade.bookybe.core.model.constant.GamificationActivity;
import com.uade.bookybe.infraestructure.entity.*;
import com.uade.bookybe.infraestructure.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GamificationServiceImplTest {

  @Mock private GamificationProfileRepository gamificationProfileRepository;
  @Mock private UserAchievementRepository userAchievementRepository;
  @Mock private AchievementRepository achievementRepository;
  @Mock private UserLevelRepository userLevelRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks private GamificationServiceImpl sut;

  // ---------------- initializeUserProfile ----------------

  @Test
  void initializeUserProfile_deberiaDevolverGetUserProfile_cuandoYaExistePerfil() {
    // given
    String userId = "u1";
    given(gamificationProfileRepository.findByUserId(userId))
        .willReturn(
            Optional.of(GamificationProfileEntity.builder().id("gp1").userId(userId).build()));

    // para que getUserProfile funcione sin volver a inicializar
    given(userLevelRepository.findAll()).willReturn(List.of());
    given(userAchievementRepository.findByUserIdOrderByDateEarnedDesc(userId))
        .willReturn(List.of());

    // when
    Optional<GamificationProfile> result = sut.initializeUserProfile(userId);

    // then
    assertTrue(result.isPresent());
    assertEquals(userId, result.get().getUserId());
    then(userRepository).shouldHaveNoInteractions();
    then(gamificationProfileRepository).should(never()).save(any());
  }

  @Test
  void initializeUserProfile_deberiaRetornarEmpty_cuandoUserNoExiste() {
    // given
    String userId = "u1";
    given(gamificationProfileRepository.findByUserId(userId)).willReturn(Optional.empty());
    given(userRepository.existsById(userId)).willReturn(false);

    // when
    Optional<GamificationProfile> result = sut.initializeUserProfile(userId);

    // then
    assertTrue(result.isEmpty());
    then(gamificationProfileRepository).should(never()).save(any());
  }

  @Test
  void initializeUserProfile_deberiaCrearPerfil_yEnriquecer() {
    // given
    String userId = "u1";
    given(gamificationProfileRepository.findByUserId(userId)).willReturn(Optional.empty());
    given(userRepository.existsById(userId)).willReturn(true);

    // save devuelve el mismo entity
    given(gamificationProfileRepository.save(any(GamificationProfileEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, GamificationProfileEntity.class));

    // enrichProfile -> getLevelForPoints -> findHighestLevelForPoints
    UserLevelEntity levelEntity = UserLevelEntity.builder().level(1).minPoints(0).build();
    given(userLevelRepository.findHighestLevelForPoints(anyInt()))
        .willReturn(Optional.of(levelEntity));

    // enrichProfile -> achievements del usuario
    given(userAchievementRepository.findByUserIdOrderByDateEarnedDesc(userId))
        .willReturn(List.of());

    // calculatePointsToNextLevel -> userLevelRepository.findAll()
    given(userLevelRepository.findAll())
        .willReturn(
            List.of(
                UserLevelEntity.builder().level(1).minPoints(0).build(),
                UserLevelEntity.builder().level(2).minPoints(100).build()));

    // when
    Optional<GamificationProfile> result = sut.initializeUserProfile(userId);

    // then
    assertTrue(result.isPresent());
    assertEquals(userId, result.get().getUserId());
    assertEquals(0, result.get().getTotalPoints());
    assertEquals(1, result.get().getCurrentLevel());
    assertNotNull(result.get().getLastActivity());
    assertNotNull(result.get().getDateCreated());
    assertNotNull(result.get().getAchievements());
    assertEquals(100, result.get().getPointsToNextLevel());

    ArgumentCaptor<GamificationProfileEntity> captor =
        ArgumentCaptor.forClass(GamificationProfileEntity.class);
    then(gamificationProfileRepository).should().save(captor.capture());
    assertNotNull(captor.getValue().getId());
    assertTrue(captor.getValue().getId().startsWith("gp-"));
  }

  // ---------------- getUserProfile ----------------

  @Test
  void getUserProfile_deberiaAutoInicializar_cuandoNoExistePerfil() {
    // given
    String userId = "u1";
    given(gamificationProfileRepository.findByUserId(userId)).willReturn(Optional.empty());

    // initializeUserProfile path
    given(userRepository.existsById(userId)).willReturn(true);
    given(gamificationProfileRepository.save(any(GamificationProfileEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, GamificationProfileEntity.class));

    // enrichProfile dependencies
    given(userAchievementRepository.findByUserIdOrderByDateEarnedDesc(userId))
        .willReturn(List.of());
    given(userLevelRepository.findHighestLevelForPoints(anyInt()))
        .willReturn(Optional.of(UserLevelEntity.builder().level(1).minPoints(0).build()));
    given(userLevelRepository.findAll())
        .willReturn(List.of(UserLevelEntity.builder().level(1).minPoints(0).build()));

    // when
    Optional<GamificationProfile> result = sut.getUserProfile(userId);

    // then
    assertTrue(result.isPresent());
    assertEquals(userId, result.get().getUserId());
    then(gamificationProfileRepository).should().save(any(GamificationProfileEntity.class));
  }

  @Test
  void getUserProfile_deberiaMapearYEnriquecer_cuandoExistePerfil() {
    // given
    String userId = "u1";
    GamificationProfileEntity entity =
        GamificationProfileEntity.builder()
            .id("gp1")
            .userId(userId)
            .totalPoints(50)
            .currentLevel(1)
            .build();

    given(gamificationProfileRepository.findByUserId(userId)).willReturn(Optional.of(entity));

    // enrichProfile
    given(userLevelRepository.findHighestLevelForPoints(50))
        .willReturn(Optional.of(UserLevelEntity.builder().level(1).minPoints(0).build()));
    given(userAchievementRepository.findByUserIdOrderByDateEarnedDesc(userId))
        .willReturn(List.of());
    given(userLevelRepository.findAll())
        .willReturn(
            List.of(
                UserLevelEntity.builder().level(1).minPoints(0).build(),
                UserLevelEntity.builder().level(2).minPoints(100).build()));

    // when
    Optional<GamificationProfile> result = sut.getUserProfile(userId);

    // then
    assertTrue(result.isPresent());
    assertEquals(50, result.get().getTotalPoints());
    assertEquals(50, result.get().getPointsToNextLevel()); // 100 - 50
  }

  // ---------------- awardPoints (String) ----------------

  @Test
  void awardPoints_deberiaSumarPuntos_actualizarNivel_guardar_yChequearAchievements_yEnriquecer() {
    // given
    String userId = "u1";
    GamificationProfileEntity profile =
        GamificationProfileEntity.builder()
            .id("gp1")
            .userId(userId)
            .totalPoints(90)
            .currentLevel(1)
            .booksRead(0)
            .exchangesCompleted(0)
            .postsCreated(0)
            .commentsCreated(0)
            .communitiesJoined(0)
            .communitiesCreated(0)
            .readingClubsJoined(0)
            .readingClubsCreated(0)
            .build();

    given(gamificationProfileRepository.findByUserId(userId)).willReturn(Optional.of(profile));
    given(gamificationProfileRepository.save(any(GamificationProfileEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, GamificationProfileEntity.class));

    // updateUserLevel: con 100 puntos pasa a nivel 2
    given(userLevelRepository.findHighestLevelForPoints(100))
        .willReturn(Optional.of(UserLevelEntity.builder().level(2).minPoints(100).build()));

    // checkAndAwardAchievements
    given(achievementRepository.findByIsActiveTrue()).willReturn(List.of());

    // enrichProfile: getLevelForPoints + achievements + pointsToNextLevel
    given(userAchievementRepository.findByUserIdOrderByDateEarnedDesc(userId))
        .willReturn(List.of());
    given(userLevelRepository.findAll())
        .willReturn(
            List.of(
                UserLevelEntity.builder().level(1).minPoints(0).build(),
                UserLevelEntity.builder().level(2).minPoints(100).build(),
                UserLevelEntity.builder().level(3).minPoints(250).build()));

    // when
    Optional<GamificationProfile> result = sut.awardPoints(userId, "ACTION", 10);

    // then
    assertTrue(result.isPresent());
    assertEquals(100, result.get().getTotalPoints());
    // pointsToNextLevel = 250 - 100
    assertEquals(150, result.get().getPointsToNextLevel());

    ArgumentCaptor<GamificationProfileEntity> captor =
        ArgumentCaptor.forClass(GamificationProfileEntity.class);
    then(gamificationProfileRepository).should(atLeastOnce()).save(captor.capture());
    assertTrue(captor.getAllValues().stream().anyMatch(e -> e.getTotalPoints() == 100));
  }

  // ---------------- awardPoints (GamificationActivity) ----------------

  @Test
  void awardPoints_porActivity_deberiaDelegarEnAwardPointsString() {
    // given
    String userId = "u1";
    GamificationProfileEntity profile =
        GamificationProfileEntity.builder()
            .id("gp1")
            .userId(userId)
            .totalPoints(0)
            .currentLevel(1)
            .booksRead(0)
            .exchangesCompleted(0)
            .postsCreated(0)
            .commentsCreated(0)
            .communitiesJoined(0)
            .communitiesCreated(0)
            .readingClubsJoined(0)
            .readingClubsCreated(0)
            .build();

    given(gamificationProfileRepository.findByUserId(userId)).willReturn(Optional.of(profile));
    given(gamificationProfileRepository.save(any(GamificationProfileEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, GamificationProfileEntity.class));

    given(userLevelRepository.findHighestLevelForPoints(anyInt()))
        .willReturn(Optional.of(UserLevelEntity.builder().level(1).minPoints(0).build()));

    given(achievementRepository.findByIsActiveTrue()).willReturn(List.of());
    given(userAchievementRepository.findByUserIdOrderByDateEarnedDesc(userId))
        .willReturn(List.of());
    given(userLevelRepository.findAll())
        .willReturn(List.of(UserLevelEntity.builder().level(1).minPoints(0).build()));

    // when
    Optional<GamificationProfile> result = sut.awardPoints(userId, GamificationActivity.BOOK_ADDED);

    // then
    assertTrue(result.isPresent());
    assertEquals(GamificationActivity.BOOK_ADDED.getPoints(), result.get().getTotalPoints());
  }

  // ---------------- process* (activity counters) ----------------
  // Nota: updateActivityCounter solo actúa si existe perfil. Verificamos que incrementa y guarda.

  @Test
  void processBookRead_deberiaIncrementarBooksRead_yOtorgarPuntos() {
    // given
    String userId = "u1";
    GamificationProfileEntity profile =
        GamificationProfileEntity.builder()
            .id("gp1")
            .userId(userId)
            .booksRead(1)
            .totalPoints(0)
            .currentLevel(1)
            .exchangesCompleted(0)
            .postsCreated(0)
            .commentsCreated(0)
            .communitiesJoined(0)
            .communitiesCreated(0)
            .readingClubsJoined(0)
            .readingClubsCreated(0)
            .build();

    // updateActivityCounter hace findByUserId y save
    // awardPoints vuelve a hacer findByUserId
    given(gamificationProfileRepository.findByUserId(userId)).willReturn(Optional.of(profile));
    given(gamificationProfileRepository.save(any(GamificationProfileEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, GamificationProfileEntity.class));

    given(userLevelRepository.findHighestLevelForPoints(anyInt()))
        .willReturn(Optional.of(UserLevelEntity.builder().level(1).minPoints(0).build()));
    given(achievementRepository.findByIsActiveTrue()).willReturn(List.of());
    given(userAchievementRepository.findByUserIdOrderByDateEarnedDesc(userId))
        .willReturn(List.of());
    given(userLevelRepository.findAll())
        .willReturn(List.of(UserLevelEntity.builder().level(1).minPoints(0).build()));

    // when
    Optional<GamificationProfile> result = sut.processBookRead(userId);

    // then
    assertTrue(result.isPresent());
    // booksRead debe haber aumentado a 2 en el entity guardado por updateActivityCounter
    then(gamificationProfileRepository)
        .should(atLeastOnce())
        .save(argThat(e -> e.getUserId().equals(userId) && e.getBooksRead() == 2));
  }

  @Test
  void processCommunityCreated_deberiaIncrementarCommunitiesCreated_yOtorgarPuntos() {
    // given
    String userId = "u1";
    GamificationProfileEntity profile =
        GamificationProfileEntity.builder()
            .id("gp1")
            .userId(userId)
            .communitiesCreated(0)
            .totalPoints(0)
            .currentLevel(1)
            .booksRead(0)
            .exchangesCompleted(0)
            .postsCreated(0)
            .commentsCreated(0)
            .communitiesJoined(0)
            .readingClubsJoined(0)
            .readingClubsCreated(0)
            .build();

    given(gamificationProfileRepository.findByUserId(userId)).willReturn(Optional.of(profile));
    given(gamificationProfileRepository.save(any(GamificationProfileEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, GamificationProfileEntity.class));

    given(userLevelRepository.findHighestLevelForPoints(anyInt()))
        .willReturn(Optional.of(UserLevelEntity.builder().level(1).minPoints(0).build()));
    given(achievementRepository.findByIsActiveTrue()).willReturn(List.of());
    given(userAchievementRepository.findByUserIdOrderByDateEarnedDesc(userId))
        .willReturn(List.of());
    given(userLevelRepository.findAll())
        .willReturn(List.of(UserLevelEntity.builder().level(1).minPoints(0).build()));

    // when
    Optional<GamificationProfile> result = sut.processCommunityCreated(userId);

    // then
    assertTrue(result.isPresent());
    then(gamificationProfileRepository)
        .should(atLeastOnce())
        .save(argThat(e -> e.getUserId().equals(userId) && e.getCommunitiesCreated() == 1));
  }

  // ---------------- checkAndAwardAchievements ----------------

  @Test
  void checkAndAwardAchievements_deberiaRetornarVacio_cuandoNoHayPerfil() {
    // given
    given(gamificationProfileRepository.findByUserId("u1")).willReturn(Optional.empty());

    // when
    List<UserAchievement> result = sut.checkAndAwardAchievements("u1");

    // then
    assertNotNull(result);
    assertTrue(result.isEmpty());
    then(achievementRepository).shouldHaveNoInteractions();
  }

  @Test
  void
      checkAndAwardAchievements_deberiaCrearUserAchievement_ySumarPuntosReward_cuandoCumpleCondicion() {
    // given
    String userId = "u1";

    GamificationProfileEntity profile =
        GamificationProfileEntity.builder()
            .id("gp1")
            .userId(userId)
            .totalPoints(10)
            .booksRead(5)
            .exchangesCompleted(0)
            .postsCreated(0)
            .commentsCreated(0)
            .communitiesJoined(0)
            .communitiesCreated(0)
            .readingClubsJoined(0)
            .readingClubsCreated(0)
            .build();

    given(gamificationProfileRepository.findByUserId(userId)).willReturn(Optional.of(profile));

    AchievementEntity achievement =
        AchievementEntity.builder()
            .id("a1")
            .name("Lector")
            .condition("BOOKS_READ")
            .requiredValue(5)
            .pointsReward(20)
            .isActive(true)
            .build();

    given(achievementRepository.findByIsActiveTrue()).willReturn(List.of(achievement));
    given(userAchievementRepository.existsByUserIdAndAchievementId(userId, "a1")).willReturn(false);

    // save del user achievement
    given(userAchievementRepository.save(any(UserAchievementEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, UserAchievementEntity.class));

    // save del profile (sumar pointsReward)
    given(gamificationProfileRepository.save(any(GamificationProfileEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, GamificationProfileEntity.class));

    // when
    List<UserAchievement> result = sut.checkAndAwardAchievements(userId);

    // then
    assertEquals(1, result.size());
    assertEquals("a1", result.get(0).getAchievementId());
    assertNotNull(result.get(0).getAchievement());
    assertEquals("Lector", result.get(0).getAchievement().getName());

    // profile totalPoints debe sumar reward
    then(gamificationProfileRepository)
        .should()
        .save(argThat(p -> p.getUserId().equals(userId) && p.getTotalPoints() == 30));

    then(userAchievementRepository)
        .should()
        .save(
            argThat(
                ua ->
                    ua.getUserId().equals(userId)
                        && "a1".equals(ua.getAchievementId())
                        && !ua.isNotified()));
  }

  @Test
  void checkAndAwardAchievements_noDeberiaCrearSiYaExiste() {
    // given
    String userId = "u1";
    GamificationProfileEntity profile =
        GamificationProfileEntity.builder().id("gp1").userId(userId).build();
    given(gamificationProfileRepository.findByUserId(userId)).willReturn(Optional.of(profile));

    AchievementEntity achievement =
        AchievementEntity.builder()
            .id("a1")
            .condition("TOTAL_POINTS")
            .requiredValue(1)
            .pointsReward(10)
            .isActive(true)
            .build();

    given(achievementRepository.findByIsActiveTrue()).willReturn(List.of(achievement));
    given(userAchievementRepository.existsByUserIdAndAchievementId(userId, "a1")).willReturn(true);

    // when
    List<UserAchievement> result = sut.checkAndAwardAchievements(userId);

    // then
    assertTrue(result.isEmpty());
    then(userAchievementRepository).should(never()).save(any(UserAchievementEntity.class));
  }

  // ---------------- markAchievementsAsNotified ----------------

  @Test
  void markAchievementsAsNotified_deberiaMarcarYGuardar_cuandoExisteRegistro() {
    // given
    String userId = "u1";
    String achievementId = "a1";

    UserAchievementEntity entity =
        UserAchievementEntity.builder()
            .id("ua1")
            .userId(userId)
            .achievementId(achievementId)
            .notified(false)
            .build();

    given(userAchievementRepository.findByUserIdAndAchievementId(userId, achievementId))
        .willReturn(Optional.of(entity));

    given(userAchievementRepository.save(any(UserAchievementEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, UserAchievementEntity.class));

    // when
    sut.markAchievementsAsNotified(userId, List.of(achievementId));

    // then
    then(userAchievementRepository)
        .should()
        .save(
            argThat(
                e ->
                    e.getUserId().equals(userId)
                        && e.getAchievementId().equals(achievementId)
                        && e.isNotified()));
  }

  @Test
  void markAchievementsAsNotified_noHaceNada_cuandoNoExisteRegistro() {
    // given
    given(userAchievementRepository.findByUserIdAndAchievementId("u1", "a1"))
        .willReturn(Optional.empty());

    // when
    sut.markAchievementsAsNotified("u1", List.of("a1"));

    // then
    then(userAchievementRepository).should(never()).save(any(UserAchievementEntity.class));
  }

  // ---------------- getAllAchievements / getUserAchievements / getUnnotifiedAchievements
  // ----------------

  @Test
  void getAllAchievements_deberiaMapearYRetornarLista() {
    // given
    given(achievementRepository.findByIsActiveTrue())
        .willReturn(List.of(AchievementEntity.builder().id("a1").name("X").isActive(true).build()));

    // when
    List<Achievement> result = sut.getAllAchievements();

    // then
    assertEquals(1, result.size());
    assertEquals("a1", result.get(0).getId());
  }

  @Test
  void getUserAchievements_deberiaMapearYRetornarLista() {
    // given
    given(userAchievementRepository.findByUserIdOrderByDateEarnedDesc("u1"))
        .willReturn(
            List.of(
                UserAchievementEntity.builder()
                    .id("ua1")
                    .userId("u1")
                    .achievementId("a1")
                    .build()));

    // when
    List<UserAchievement> result = sut.getUserAchievements("u1");

    // then
    assertEquals(1, result.size());
    assertEquals("ua1", result.get(0).getId());
  }

  @Test
  void getUnnotifiedAchievements_deberiaMapearYRetornarLista() {
    // given
    given(userAchievementRepository.findUnnotifiedByUserId("u1"))
        .willReturn(
            List.of(
                UserAchievementEntity.builder()
                    .id("ua1")
                    .userId("u1")
                    .achievementId("a1")
                    .notified(false)
                    .build()));

    // when
    List<UserAchievement> result = sut.getUnnotifiedAchievements("u1");

    // then
    assertEquals(1, result.size());
    assertFalse(result.get(0).isNotified());
  }

  // ---------------- getAllUserLevels / getLevelForPoints ----------------

  @Test
  void getAllUserLevels_deberiaMapearYRetornarLista() {
    // given
    given(userLevelRepository.findAll())
        .willReturn(List.of(UserLevelEntity.builder().level(1).minPoints(0).build()));

    // when
    List<UserLevel> result = sut.getAllUserLevels();

    // then
    assertEquals(1, result.size());
    assertEquals(1, result.get(0).getLevel());
  }

  @Test
  void getLevelForPoints_deberiaMapearOptional() {
    // given
    given(userLevelRepository.findHighestLevelForPoints(150))
        .willReturn(Optional.of(UserLevelEntity.builder().level(2).minPoints(100).build()));

    // when
    Optional<UserLevel> result = sut.getLevelForPoints(150);

    // then
    assertTrue(result.isPresent());
    assertEquals(2, result.get().getLevel());
  }

  // ---------------- deleteUserGamificationData ----------------

  @Test
  void deleteUserGamificationData_deberiaBorrarAchievements_yProfile_yRetornarTrue() {
    // given
    String userId = "u1";

    List<UserAchievementEntity> achievements =
        List.of(
            UserAchievementEntity.builder().id("ua1").userId(userId).achievementId("a1").build());
    given(userAchievementRepository.findByUserId(userId)).willReturn(achievements);

    GamificationProfileEntity profile =
        GamificationProfileEntity.builder().id("gp1").userId(userId).build();
    given(gamificationProfileRepository.findByUserId(userId)).willReturn(Optional.of(profile));

    willDoNothing().given(userAchievementRepository).deleteAll(achievements);
    willDoNothing().given(gamificationProfileRepository).delete(profile);

    // when
    boolean result = sut.deleteUserGamificationData(userId);

    // then
    assertTrue(result);
    then(userAchievementRepository).should().deleteAll(achievements);
    then(gamificationProfileRepository).should().delete(profile);
  }

  @Test
  void deleteUserGamificationData_deberiaBorrarSoloProfile_siNoHayAchievements() {
    // given
    String userId = "u1";
    given(userAchievementRepository.findByUserId(userId)).willReturn(List.of());

    GamificationProfileEntity profile =
        GamificationProfileEntity.builder().id("gp1").userId(userId).build();
    given(gamificationProfileRepository.findByUserId(userId)).willReturn(Optional.of(profile));

    willDoNothing().given(gamificationProfileRepository).delete(profile);

    // when
    boolean result = sut.deleteUserGamificationData(userId);

    // then
    assertTrue(result);
    then(userAchievementRepository).should(never()).deleteAll(anyList());
    then(gamificationProfileRepository).should().delete(profile);
  }

  @Test
  void deleteUserGamificationData_deberiaRetornarTrue_siNoHayProfile() {
    // given
    String userId = "u1";
    given(userAchievementRepository.findByUserId(userId)).willReturn(List.of());
    given(gamificationProfileRepository.findByUserId(userId)).willReturn(Optional.empty());

    // when
    boolean result = sut.deleteUserGamificationData(userId);

    // then
    assertTrue(result);
    then(gamificationProfileRepository).should(never()).delete(any());
  }

  @Test
  void deleteUserGamificationData_deberiaRetornarFalse_cuandoExplotaAlgo() {
    // given
    String userId = "u1";
    given(userAchievementRepository.findByUserId(userId)).willThrow(new RuntimeException("boom"));

    // when
    boolean result = sut.deleteUserGamificationData(userId);

    // then
    assertFalse(result);
  }
}
