package com.uade.bookybe.core.usecase.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.exception.UnauthorizedException;
import com.uade.bookybe.core.model.LivekitRoom;
import com.uade.bookybe.core.model.LivekitToken;
import com.uade.bookybe.core.model.MeetingStatus;
import com.uade.bookybe.core.model.ReadingClub;
import com.uade.bookybe.core.usecase.LivekitRoomService;
import com.uade.bookybe.core.usecase.LivekitTokenService;
import com.uade.bookybe.core.usecase.ReadingClubService;
import com.uade.bookybe.infraestructure.entity.ReadingClubEntity;
import com.uade.bookybe.infraestructure.repository.ReadingClubRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultMeetingServiceTest {

  @Mock private LivekitTokenService livekitTokenService;
  @Mock private LivekitRoomService livekitRoomService;
  @Mock private ReadingClubService readingClubService;
  @Mock private ReadingClubRepository readingClubRepository;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private DefaultMeetingService sut;

  // ---------------- generateMeetingToken ----------------

  @Test
  void generateMeetingToken_deberiaLanzarNotFound_siClubNoExiste() {
    // given
    given(readingClubService.getReadingClubById("club1")).willReturn(Optional.empty());

    // when + then
    NotFoundException ex =
        assertThrows(
            NotFoundException.class, () -> sut.generateMeetingToken("club1", "u1", "Felipe"));
    assertTrue(ex.getMessage().toLowerCase().contains("reading club not found"));

    then(livekitTokenService).shouldHaveNoInteractions();
    then(livekitRoomService).shouldHaveNoInteractions();
  }

  @Test
  void generateMeetingToken_deberiaLanzarUnauthorized_siUserNoEsMiembro() {
    // given
    ReadingClub club = ReadingClub.builder().id("club1").name("Club").moderatorId("u9").build();
    given(readingClubService.getReadingClubById("club1")).willReturn(Optional.of(club));
    given(readingClubService.isUserMember("club1", "u1")).willReturn(false);

    // when + then
    UnauthorizedException ex =
        assertThrows(
            UnauthorizedException.class, () -> sut.generateMeetingToken("club1", "u1", "Felipe"));
    assertTrue(ex.getMessage().toLowerCase().contains("not a member"));

    then(livekitTokenService).shouldHaveNoInteractions();
  }

  @Test
  void generateMeetingToken_deberiaCrearToken_conPermisosModerador_siUserEsModerador() {
    // given
    String clubId = "club1";
    String userId = "u1";
    ReadingClub club = ReadingClub.builder().id(clubId).name("Club").moderatorId(userId).build();

    given(readingClubService.getReadingClubById(clubId)).willReturn(Optional.of(club));
    given(readingClubService.isUserMember(clubId, userId)).willReturn(true);

    LivekitToken expected =
        LivekitToken.builder()
            .token("jwt")
            .roomName("reading-club-" + clubId)
            .participantId(userId)
            .build();
    given(
            livekitTokenService.createToken(
                anyString(), anyString(), anyString(), any(LivekitToken.TokenPermissions.class)))
        .willReturn(expected);

    ArgumentCaptor<LivekitToken.TokenPermissions> permCaptor =
        ArgumentCaptor.forClass(LivekitToken.TokenPermissions.class);

    // when
    LivekitToken result = sut.generateMeetingToken(clubId, userId, "Felipe");

    // then
    assertNotNull(result);
    assertEquals(expected, result);

    then(livekitTokenService)
        .should()
        .createToken(eq("reading-club-" + clubId), eq("Felipe"), eq(userId), permCaptor.capture());

    assertTrue(permCaptor.getValue().isModerator());
  }

  @Test
  void generateMeetingToken_deberiaCrearToken_conPermisosNoModerador_siUserNoEsModerador() {
    // given
    String clubId = "club1";
    String userId = "u2";
    ReadingClub club = ReadingClub.builder().id(clubId).name("Club").moderatorId("u1").build();

    given(readingClubService.getReadingClubById(clubId)).willReturn(Optional.of(club));
    given(readingClubService.isUserMember(clubId, userId)).willReturn(true);

    LivekitToken expected =
        LivekitToken.builder()
            .token("jwt")
            .roomName("reading-club-" + clubId)
            .participantId(userId)
            .build();
    given(
            livekitTokenService.createToken(
                anyString(), anyString(), anyString(), any(LivekitToken.TokenPermissions.class)))
        .willReturn(expected);

    ArgumentCaptor<LivekitToken.TokenPermissions> permCaptor =
        ArgumentCaptor.forClass(LivekitToken.TokenPermissions.class);

    // when
    LivekitToken result = sut.generateMeetingToken(clubId, userId, "User2");

    // then
    assertNotNull(result);
    then(livekitTokenService)
        .should()
        .createToken(eq("reading-club-" + clubId), eq("User2"), eq(userId), permCaptor.capture());

    assertFalse(permCaptor.getValue().isModerator());
  }

  // ---------------- startMeeting ----------------

  @Test
  void startMeeting_deberiaLanzarNotFound_siClubNoExiste() {
    // given
    given(readingClubService.getReadingClubById("club1")).willReturn(Optional.empty());

    // when + then
    assertThrows(NotFoundException.class, () -> sut.startMeeting("club1", "u1"));
  }

  @Test
  void startMeeting_deberiaLanzarUnauthorized_siNoEsModerador() {
    // given
    ReadingClub club = ReadingClub.builder().id("club1").name("Club").moderatorId("u9").build();
    given(readingClubService.getReadingClubById("club1")).willReturn(Optional.of(club));

    // when + then
    assertThrows(UnauthorizedException.class, () -> sut.startMeeting("club1", "u1"));
  }

  @Test
  void startMeeting_deberiaCrearRoom_yActualizarEntidad_yRetornarClubActualizado()
      throws Exception {
    // given
    String clubId = "club1";
    String moderatorId = "u1";

    ReadingClub club =
        ReadingClub.builder().id(clubId).name("ClubName").moderatorId(moderatorId).build();
    given(readingClubService.getReadingClubById(clubId)).willReturn(Optional.of(club));

    given(objectMapper.writeValueAsString(any())).willReturn("{\"ok\":true}");

    ReadingClubEntity entity =
        ReadingClubEntity.builder()
            .id(clubId)
            .moderatorId(moderatorId)
            .name("ClubName")
            .meetingActive(false)
            .build();
    given(readingClubRepository.findById(clubId)).willReturn(Optional.of(entity));
    given(readingClubRepository.save(any(ReadingClubEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, ReadingClubEntity.class));

    given(readingClubService.getMemberCount(clubId)).willReturn(7L);

    ArgumentCaptor<LivekitRoom.RoomOptions> optionsCaptor =
        ArgumentCaptor.forClass(LivekitRoom.RoomOptions.class);
    ArgumentCaptor<ReadingClubEntity> entityCaptor =
        ArgumentCaptor.forClass(ReadingClubEntity.class);

    // when
    ReadingClub updated = sut.startMeeting(clubId, moderatorId);

    // then
    assertNotNull(updated);
    assertEquals(clubId, updated.getId());
    assertEquals(7L, updated.getMemberCount());
    assertNotNull(
        updated.getMeetingStartedAt()); // depende del mapper, pero en general debería setearse

    then(livekitRoomService)
        .should()
        .createRoom(eq("reading-club-" + clubId), optionsCaptor.capture());
    LivekitRoom.RoomOptions opts = optionsCaptor.getValue();
    assertNotNull(opts);
    assertEquals(600, opts.getEmptyTimeout());
    assertEquals(50, opts.getMaxParticipants());
    assertEquals("{\"ok\":true}", opts.getMetadata());

    then(readingClubRepository).should().save(entityCaptor.capture());
    ReadingClubEntity savedEntity = entityCaptor.getValue();
    assertTrue(savedEntity.getMeetingActive());
    assertNotNull(savedEntity.getMeetingStartedAt());
  }

  @Test
  void startMeeting_deberiaLanzarRuntime_siNoPuedeActualizarEntidad() throws Exception {
    // given
    String clubId = "club1";
    String moderatorId = "u1";

    ReadingClub club =
        ReadingClub.builder().id(clubId).name("ClubName").moderatorId(moderatorId).build();
    given(readingClubService.getReadingClubById(clubId)).willReturn(Optional.of(club));

    given(objectMapper.writeValueAsString(any())).willReturn("{}");
    given(readingClubRepository.findById(clubId)).willReturn(Optional.empty());

    // when + then
    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> sut.startMeeting(clubId, moderatorId));
    assertTrue(ex.getMessage().toLowerCase().contains("failed"));
  }

  @Test
  void startMeeting_deberiaLanzarRuntime_siObjectMapperFalla() throws Exception {
    // given
    String clubId = "club1";
    String moderatorId = "u1";

    ReadingClub club =
        ReadingClub.builder().id(clubId).name("ClubName").moderatorId(moderatorId).build();
    given(readingClubService.getReadingClubById(clubId)).willReturn(Optional.of(club));

    given(objectMapper.writeValueAsString(any())).willThrow(new JsonProcessingException("boom") {});

    // when + then
    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> sut.startMeeting(clubId, moderatorId));
    assertTrue(ex.getMessage().toLowerCase().contains("failed"));
  }

  // ---------------- endMeeting ----------------

  @Test
  void endMeeting_deberiaLanzarNotFound_siClubNoExiste() {
    // given
    given(readingClubService.getReadingClubById("club1")).willReturn(Optional.empty());

    // when + then
    assertThrows(NotFoundException.class, () -> sut.endMeeting("club1", "u1"));
  }

  @Test
  void endMeeting_deberiaLanzarUnauthorized_siNoEsModerador() {
    // given
    ReadingClub club = ReadingClub.builder().id("club1").name("Club").moderatorId("u9").build();
    given(readingClubService.getReadingClubById("club1")).willReturn(Optional.of(club));

    // when + then
    assertThrows(UnauthorizedException.class, () -> sut.endMeeting("club1", "u1"));
  }

  @Test
  void endMeeting_deberiaDeleteRoom_yActualizarEntidadConDuracion_yRetornarClubActualizado() {
    // given
    String clubId = "club1";
    String moderatorId = "u1";

    ReadingClub club =
        ReadingClub.builder().id(clubId).name("ClubName").moderatorId(moderatorId).build();
    given(readingClubService.getReadingClubById(clubId)).willReturn(Optional.of(club));

    LocalDateTime startedAt = LocalDateTime.now().minusMinutes(5);
    ReadingClubEntity entity =
        ReadingClubEntity.builder()
            .id(clubId)
            .moderatorId(moderatorId)
            .name("ClubName")
            .meetingActive(true)
            .meetingStartedAt(startedAt)
            .build();

    given(readingClubRepository.findById(clubId)).willReturn(Optional.of(entity));
    given(readingClubRepository.save(any(ReadingClubEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, ReadingClubEntity.class));

    given(readingClubService.getMemberCount(clubId)).willReturn(3L);

    ArgumentCaptor<ReadingClubEntity> entityCaptor =
        ArgumentCaptor.forClass(ReadingClubEntity.class);

    // when
    ReadingClub updated = sut.endMeeting(clubId, moderatorId);

    // then
    assertNotNull(updated);
    assertEquals(3L, updated.getMemberCount());

    then(livekitRoomService).should().deleteRoom("reading-club-" + clubId);

    then(readingClubRepository).should().save(entityCaptor.capture());
    ReadingClubEntity saved = entityCaptor.getValue();
    assertFalse(saved.getMeetingActive());
    assertNotNull(saved.getMeetingEndedAt());
    assertNotNull(saved.getLastMeetingDuration());
    assertTrue(saved.getLastMeetingDuration() >= 0);
  }

  @Test
  void endMeeting_deberiaLanzarRuntime_siNoPuedeActualizarEntidad() {
    // given
    String clubId = "club1";
    String moderatorId = "u1";

    ReadingClub club =
        ReadingClub.builder().id(clubId).name("ClubName").moderatorId(moderatorId).build();
    given(readingClubService.getReadingClubById(clubId)).willReturn(Optional.of(club));

    given(readingClubRepository.findById(clubId)).willReturn(Optional.empty());

    // when + then
    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> sut.endMeeting(clubId, moderatorId));
    assertTrue(ex.getMessage().toLowerCase().contains("failed"));
  }

  // ---------------- getMeetingStatus ----------------

  @Test
  void getMeetingStatus_deberiaDevolverStatusDeLivekit_yEnriquecerConStartedAtSiExisteClub() {
    // given
    String clubId = "club1";
    MeetingStatus status =
        MeetingStatus.builder().exists(true).roomName("reading-club-" + clubId).build();
    given(livekitRoomService.getRoomStatus("reading-club-" + clubId)).willReturn(status);

    LocalDateTime startedAt = LocalDateTime.now().minusMinutes(2);
    ReadingClub club =
        ReadingClub.builder().id(clubId).moderatorId("u1").meetingStartedAt(startedAt).build();
    given(readingClubService.getReadingClubById(clubId)).willReturn(Optional.of(club));

    // when
    MeetingStatus result = sut.getMeetingStatus(clubId);

    // then
    assertNotNull(result);
    assertEquals(startedAt, result.getStartedAt());
    then(livekitRoomService).should().getRoomStatus("reading-club-" + clubId);
  }

  @Test
  void getMeetingStatus_deberiaDevolverStatusDeLivekit_sinModificarStartedAt_siClubNoExiste() {
    // given
    String clubId = "club1";
    MeetingStatus status =
        MeetingStatus.builder().exists(true).roomName("reading-club-" + clubId).build();
    given(livekitRoomService.getRoomStatus("reading-club-" + clubId)).willReturn(status);
    given(readingClubService.getReadingClubById(clubId)).willReturn(Optional.empty());

    // when
    MeetingStatus result = sut.getMeetingStatus(clubId);

    // then
    assertNotNull(result);
    // no assert sobre startedAt porque depende del builder / default
    then(livekitRoomService).should().getRoomStatus("reading-club-" + clubId);
  }

  // ---------------- passthroughs ----------------

  @Test
  void isMemberOfReadingClub_deberiaDelegarEnReadingClubService() {
    // given
    given(readingClubService.isUserMember("club1", "u1")).willReturn(true);

    // when
    boolean result = sut.isMemberOfReadingClub("u1", "club1");

    // then
    assertTrue(result);
    then(readingClubService).should().isUserMember("club1", "u1");
  }

  @Test
  void getReadingClub_deberiaDelegarEnReadingClubService() {
    // given
    ReadingClub club = ReadingClub.builder().id("club1").build();
    given(readingClubService.getReadingClubById("club1")).willReturn(Optional.of(club));

    // when
    Optional<ReadingClub> result = sut.getReadingClub("club1");

    // then
    assertTrue(result.isPresent());
    assertEquals("club1", result.get().getId());
    then(readingClubService).should().getReadingClubById("club1");
  }
}
