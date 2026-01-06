package com.uade.bookybe.core.usecase.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

import com.uade.bookybe.core.exception.ConflictException;
import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.model.ReadingClub;
import com.uade.bookybe.core.usecase.GamificationService;
import com.uade.bookybe.infraestructure.entity.ReadingClubEntity;
import com.uade.bookybe.infraestructure.entity.ReadingClubMemberEntity;
import com.uade.bookybe.infraestructure.repository.BookRepository;
import com.uade.bookybe.infraestructure.repository.CommunityRepository;
import com.uade.bookybe.infraestructure.repository.ReadingClubMemberRepository;
import com.uade.bookybe.infraestructure.repository.ReadingClubRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReadingClubServiceImplTest {

  @Mock private ReadingClubRepository readingClubRepository;
  @Mock private ReadingClubMemberRepository readingClubMemberRepository;
  @Mock private CommunityRepository communityRepository;
  @Mock private BookRepository bookRepository;
  @Mock private GamificationService gamificationService;

  @InjectMocks private ReadingClubServiceImpl sut;

  // ---------------- getAllReadingClubs ----------------

  @Test
  void getAllReadingClubs_deberiaMapear_ySetearMemberCount() {
    // given
    ReadingClubEntity e1 = ReadingClubEntity.builder().id("c1").name("Club 1").build();
    ReadingClubEntity e2 = ReadingClubEntity.builder().id("c2").name("Club 2").build();
    given(readingClubRepository.findAll()).willReturn(List.of(e1, e2));

    given(readingClubMemberRepository.countByReadingClubId("c1")).willReturn(3L);
    given(readingClubMemberRepository.countByReadingClubId("c2")).willReturn(0L);

    // when
    List<ReadingClub> result = sut.getAllReadingClubs();

    // then
    assertEquals(2, result.size());
    assertEquals("c1", result.get(0).getId());
    assertEquals(3L, result.get(0).getMemberCount());
    assertEquals("c2", result.get(1).getId());
    assertEquals(0L, result.get(1).getMemberCount());
  }

  // ---------------- getReadingClubById ----------------

  @Test
  void getReadingClubById_deberiaRetornarEmpty_siNoExiste() {
    // given
    given(readingClubRepository.findById("c1")).willReturn(Optional.empty());

    // when
    Optional<ReadingClub> result = sut.getReadingClubById("c1");

    // then
    assertTrue(result.isEmpty());
    then(readingClubMemberRepository).should(never()).countByReadingClubId(anyString());
  }

  @Test
  void getReadingClubById_deberiaMapear_ySetearMemberCount_siExiste() {
    // given
    ReadingClubEntity e = ReadingClubEntity.builder().id("c1").name("Club").build();
    given(readingClubRepository.findById("c1")).willReturn(Optional.of(e));
    given(readingClubMemberRepository.countByReadingClubId("c1")).willReturn(5L);

    // when
    Optional<ReadingClub> result = sut.getReadingClubById("c1");

    // then
    assertTrue(result.isPresent());
    assertEquals("c1", result.get().getId());
    assertEquals(5L, result.get().getMemberCount());
  }

  // ---------------- getReadingClubsByUserId ----------------

  @Test
  void getReadingClubsByUserId_deberiaMapearDesdeMemberEntity_ySetearMemberCount() {
    // given
    ReadingClubEntity clubEntity = ReadingClubEntity.builder().id("c1").name("Club").build();
    ReadingClubMemberEntity memberEntity =
        ReadingClubMemberEntity.builder()
            .readingClubId("c1")
            .userId("u1")
            .readingClub(clubEntity)
            .build();

    given(readingClubMemberRepository.findByUserIdWithReadingClub("u1"))
        .willReturn(List.of(memberEntity));

    given(readingClubMemberRepository.countByReadingClubId("c1")).willReturn(2L);

    // when
    List<ReadingClub> result = sut.getReadingClubsByUserId("u1");

    // then
    assertEquals(1, result.size());
    assertEquals("c1", result.get(0).getId());
    assertEquals(2L, result.get(0).getMemberCount());
  }

  // ---------------- getReadingClubsByCommunityId ----------------

  @Test
  void getReadingClubsByCommunityId_deberiaMapear_ySetearMemberCount() {
    // given
    ReadingClubEntity e = ReadingClubEntity.builder().id("c1").communityId("com1").build();
    given(readingClubRepository.findByCommunityIdOrderByDateCreatedDesc("com1"))
        .willReturn(List.of(e));
    given(readingClubMemberRepository.countByReadingClubId("c1")).willReturn(7L);

    // when
    List<ReadingClub> result = sut.getReadingClubsByCommunityId("com1");

    // then
    assertEquals(1, result.size());
    assertEquals(7L, result.get(0).getMemberCount());
  }

  // ---------------- createReadingClub ----------------

  @Test
  void createReadingClub_deberiaRetornarEmpty_siNombreYaExiste() {
    // given
    given(readingClubRepository.existsByName("Club")).willReturn(true);

    // when
    Optional<ReadingClub> result =
        sut.createReadingClub("u1", "Club", "d", "com1", "b1", LocalDateTime.now());

    // then
    assertTrue(result.isEmpty());
    then(communityRepository).shouldHaveNoInteractions();
    then(bookRepository).shouldHaveNoInteractions();
    then(readingClubRepository).should(never()).save(any());
  }

  @Test
  void createReadingClub_deberiaRetornarEmpty_siCommunityNoExiste() {
    // given
    given(readingClubRepository.existsByName("Club")).willReturn(false);
    given(communityRepository.existsById("com1")).willReturn(false);

    // when
    Optional<ReadingClub> result =
        sut.createReadingClub("u1", "Club", "d", "com1", "b1", LocalDateTime.now());

    // then
    assertTrue(result.isEmpty());
    then(bookRepository).shouldHaveNoInteractions();
    then(readingClubRepository).should(never()).save(any());
  }

  @Test
  void createReadingClub_deberiaRetornarEmpty_siBookNoExiste() {
    // given
    given(readingClubRepository.existsByName("Club")).willReturn(false);
    given(communityRepository.existsById("com1")).willReturn(true);
    given(bookRepository.existsById("b1")).willReturn(false);

    // when
    Optional<ReadingClub> result =
        sut.createReadingClub("u1", "Club", "d", "com1", "b1", LocalDateTime.now());

    // then
    assertTrue(result.isEmpty());
    then(readingClubRepository).should(never()).save(any());
    then(readingClubMemberRepository).should(never()).save(any());
    then(gamificationService).shouldHaveNoInteractions();
  }

  @Test
  void createReadingClub_deberiaCrearClub_guardarMember_yOtorgarGamification() {
    // given
    LocalDateTime next = LocalDateTime.now().plusDays(1);

    given(readingClubRepository.existsByName("Club")).willReturn(false);
    given(communityRepository.existsById("com1")).willReturn(true);
    given(bookRepository.existsById("b1")).willReturn(true);

    given(readingClubRepository.save(any(ReadingClubEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, ReadingClubEntity.class));

    given(readingClubMemberRepository.save(any(ReadingClubMemberEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, ReadingClubMemberEntity.class));

    ArgumentCaptor<ReadingClubEntity> clubCaptor = ArgumentCaptor.forClass(ReadingClubEntity.class);
    ArgumentCaptor<ReadingClubMemberEntity> memberCaptor =
        ArgumentCaptor.forClass(ReadingClubMemberEntity.class);

    // when
    Optional<ReadingClub> result = sut.createReadingClub("u1", "Club", "desc", "com1", "b1", next);

    // then
    assertTrue(result.isPresent());
    assertEquals("Club", result.get().getName());
    assertEquals(1L, result.get().getMemberCount());

    then(readingClubRepository).should().save(clubCaptor.capture());
    ReadingClubEntity savedClub = clubCaptor.getValue();
    assertNotNull(savedClub.getId());
    assertTrue(savedClub.getId().startsWith("club-"));
    assertEquals("com1", savedClub.getCommunityId());
    assertEquals("b1", savedClub.getBookId());
    assertEquals("u1", savedClub.getModeratorId());
    assertEquals(next, savedClub.getNextMeeting());
    assertNotNull(savedClub.getDateCreated());
    assertNotNull(savedClub.getLastUpdated());

    then(readingClubMemberRepository).should().save(memberCaptor.capture());
    ReadingClubMemberEntity savedMember = memberCaptor.getValue();
    assertEquals("u1", savedMember.getUserId());
    assertEquals(savedClub.getId(), savedMember.getReadingClubId());

    then(gamificationService).should().processReadingClubCreated("u1");
  }

  @Test
  void createReadingClub_deberiaRetornarEmpty_siExplotaAlgunaExcepcion() {
    // given
    given(readingClubRepository.existsByName("Club")).willReturn(false);
    given(communityRepository.existsById("com1")).willReturn(true);
    given(bookRepository.existsById("b1")).willReturn(true);

    willThrow(new RuntimeException("boom"))
        .given(readingClubRepository)
        .save(any(ReadingClubEntity.class));

    // when
    Optional<ReadingClub> result =
        sut.createReadingClub("u1", "Club", "d", "com1", "b1", LocalDateTime.now());

    // then
    assertTrue(result.isEmpty());
    then(readingClubMemberRepository).should(never()).save(any());
    then(gamificationService).shouldHaveNoInteractions();
  }

  // ---------------- joinReadingClub ----------------

  @Test
  void joinReadingClub_deberiaLanzarConflict_siYaEsMiembro() {
    // given
    given(readingClubMemberRepository.existsByReadingClubIdAndUserId("c1", "u1")).willReturn(true);

    // when + then
    assertThrows(ConflictException.class, () -> sut.joinReadingClub("c1", "u1"));

    then(readingClubRepository).shouldHaveNoInteractions();
    then(readingClubMemberRepository).should(never()).save(any());
  }

  @Test
  void joinReadingClub_deberiaLanzarNotFound_siClubNoExiste() {
    // given
    given(readingClubMemberRepository.existsByReadingClubIdAndUserId("c1", "u1")).willReturn(false);
    given(readingClubRepository.existsById("c1")).willReturn(false);

    // when + then
    assertThrows(NotFoundException.class, () -> sut.joinReadingClub("c1", "u1"));

    then(readingClubMemberRepository).should(never()).save(any());
  }

  @Test
  void joinReadingClub_deberiaGuardarMember_yOtorgarGamification_siNoEsModerador() {
    // given
    given(readingClubMemberRepository.existsByReadingClubIdAndUserId("c1", "u2")).willReturn(false);
    given(readingClubRepository.existsById("c1")).willReturn(true);

    given(readingClubMemberRepository.save(any(ReadingClubMemberEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, ReadingClubMemberEntity.class));

    ReadingClubEntity club = ReadingClubEntity.builder().id("c1").moderatorId("u1").build();
    given(readingClubRepository.findById("c1")).willReturn(Optional.of(club));

    // when
    boolean result = sut.joinReadingClub("c1", "u2");

    // then
    assertTrue(result);
    then(readingClubMemberRepository)
        .should()
        .save(argThat(m -> "c1".equals(m.getReadingClubId()) && "u2".equals(m.getUserId())));
    then(gamificationService).should().processReadingClubJoined("u2");
  }

  @Test
  void joinReadingClub_noDeberiaOtorgarGamification_siSeUneElModerador() {
    // given
    given(readingClubMemberRepository.existsByReadingClubIdAndUserId("c1", "u1")).willReturn(false);
    given(readingClubRepository.existsById("c1")).willReturn(true);

    given(readingClubMemberRepository.save(any(ReadingClubMemberEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, ReadingClubMemberEntity.class));

    ReadingClubEntity club = ReadingClubEntity.builder().id("c1").moderatorId("u1").build();
    given(readingClubRepository.findById("c1")).willReturn(Optional.of(club));

    // when
    boolean result = sut.joinReadingClub("c1", "u1");

    // then
    assertTrue(result);
    then(gamificationService).should(never()).processReadingClubJoined(anyString());
  }

  @Test
  void joinReadingClub_deberiaNoFallarSiNoPuedeOtorgarGamification_porExcepcion() {
    // given
    given(readingClubMemberRepository.existsByReadingClubIdAndUserId("c1", "u2")).willReturn(false);
    given(readingClubRepository.existsById("c1")).willReturn(true);

    given(readingClubMemberRepository.save(any(ReadingClubMemberEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, ReadingClubMemberEntity.class));

    willThrow(new RuntimeException("boom")).given(readingClubRepository).findById("c1");

    // when
    boolean result = sut.joinReadingClub("c1", "u2");

    // then
    assertTrue(result);
    // gamificationService no se verifica porque queda dentro del try/catch
  }

  // ---------------- leaveReadingClub ----------------

  @Test
  void leaveReadingClub_deberiaLanzarNotFound_siClubNoExiste() {
    // given
    given(readingClubRepository.existsById("c1")).willReturn(false);

    // when + then
    assertThrows(NotFoundException.class, () -> sut.leaveReadingClub("c1", "u1"));
  }

  @Test
  void leaveReadingClub_deberiaRetornarFalse_siNoEsMiembro() {
    // given
    given(readingClubRepository.existsById("c1")).willReturn(true);
    given(readingClubMemberRepository.existsByReadingClubIdAndUserId("c1", "u1")).willReturn(false);

    // when
    boolean result = sut.leaveReadingClub("c1", "u1");

    // then
    assertFalse(result);
    then(readingClubMemberRepository)
        .should(never())
        .leaveFromReadingClub(anyString(), anyString());
  }

  @Test
  void leaveReadingClub_deberiaRetornarTrue_siLeaveOk() {
    // given
    given(readingClubRepository.existsById("c1")).willReturn(true);
    given(readingClubMemberRepository.existsByReadingClubIdAndUserId("c1", "u1")).willReturn(true);

    willDoNothing().given(readingClubMemberRepository).leaveFromReadingClub("c1", "u1");

    // when
    boolean result = sut.leaveReadingClub("c1", "u1");

    // then
    assertTrue(result);
    then(readingClubMemberRepository).should().leaveFromReadingClub("c1", "u1");
  }

  @Test
  void leaveReadingClub_deberiaRetornarFalse_siLeaveTiraExcepcion() {
    // given
    given(readingClubRepository.existsById("c1")).willReturn(true);
    given(readingClubMemberRepository.existsByReadingClubIdAndUserId("c1", "u1")).willReturn(true);

    willThrow(new RuntimeException("boom"))
        .given(readingClubMemberRepository)
        .leaveFromReadingClub("c1", "u1");

    // when
    boolean result = sut.leaveReadingClub("c1", "u1");

    // then
    assertFalse(result);
  }

  // ---------------- deleteReadingClub ----------------

  @Test
  void deleteReadingClub_deberiaLanzarNotFound_siNoExiste() {
    // given
    given(readingClubRepository.existsById("c1")).willReturn(false);

    // when + then
    assertThrows(NotFoundException.class, () -> sut.deleteReadingClub("c1", "u1"));
  }

  @Test
  void deleteReadingClub_deberiaLanzarNotFound_siExistsPeroFindByIdEmpty() {
    // given
    given(readingClubRepository.existsById("c1")).willReturn(true);
    given(readingClubRepository.findById("c1")).willReturn(Optional.empty());

    // when + then
    assertThrows(NotFoundException.class, () -> sut.deleteReadingClub("c1", "u1"));
  }

  @Test
  void deleteReadingClub_deberiaRetornarFalse_siNoEsModerador() {
    // given
    given(readingClubRepository.existsById("c1")).willReturn(true);
    given(readingClubRepository.findById("c1"))
        .willReturn(Optional.of(ReadingClubEntity.builder().id("c1").moderatorId("u9").build()));

    // when
    boolean result = sut.deleteReadingClub("c1", "u1");

    // then
    assertFalse(result);
    then(readingClubMemberRepository).should(never()).deleteAllByReadingClubId(anyString());
    then(readingClubRepository).should(never()).deleteById(anyString());
  }

  @Test
  void deleteReadingClub_deberiaBorrarMembers_yClub_siEsModerador() {
    // given
    given(readingClubRepository.existsById("c1")).willReturn(true);
    given(readingClubRepository.findById("c1"))
        .willReturn(Optional.of(ReadingClubEntity.builder().id("c1").moderatorId("u1").build()));

    willDoNothing().given(readingClubMemberRepository).deleteAllByReadingClubId("c1");
    willDoNothing().given(readingClubRepository).deleteById("c1");

    // when
    boolean result = sut.deleteReadingClub("c1", "u1");

    // then
    assertTrue(result);
    then(readingClubMemberRepository).should().deleteAllByReadingClubId("c1");
    then(readingClubRepository).should().deleteById("c1");
  }

  @Test
  void deleteReadingClub_deberiaRetornarFalse_siFallaAlBorrar() {
    // given
    given(readingClubRepository.existsById("c1")).willReturn(true);
    given(readingClubRepository.findById("c1"))
        .willReturn(Optional.of(ReadingClubEntity.builder().id("c1").moderatorId("u1").build()));

    willThrow(new RuntimeException("boom"))
        .given(readingClubMemberRepository)
        .deleteAllByReadingClubId("c1");

    // when
    boolean result = sut.deleteReadingClub("c1", "u1");

    // then
    assertFalse(result);
    then(readingClubRepository).should(never()).deleteById("c1");
  }

  // ---------------- searchReadingClubs / getReadingClubsByBookId ----------------

  @Test
  void searchReadingClubs_deberiaMapear_ySetearMemberCount() {
    // given
    given(readingClubRepository.searchReadingClubs("harry"))
        .willReturn(List.of(ReadingClubEntity.builder().id("c1").name("Harry").build()));
    given(readingClubMemberRepository.countByReadingClubId("c1")).willReturn(9L);

    // when
    List<ReadingClub> result = sut.searchReadingClubs("harry");

    // then
    assertEquals(1, result.size());
    assertEquals(9L, result.get(0).getMemberCount());
  }

  @Test
  void getReadingClubsByBookId_deberiaMapear_ySetearMemberCount() {
    // given
    given(readingClubRepository.findByBookIdOrderByDateCreatedDesc("b1"))
        .willReturn(List.of(ReadingClubEntity.builder().id("c1").bookId("b1").build()));
    given(readingClubMemberRepository.countByReadingClubId("c1")).willReturn(4L);

    // when
    List<ReadingClub> result = sut.getReadingClubsByBookId("b1");

    // then
    assertEquals(1, result.size());
    assertEquals(4L, result.get(0).getMemberCount());
  }

  // ---------------- getMemberCount / isUserMember ----------------

  @Test
  void getMemberCount_deberiaDelegarEnRepo() {
    // given
    given(readingClubMemberRepository.countByReadingClubId("c1")).willReturn(12L);

    // when
    long result = sut.getMemberCount("c1");

    // then
    assertEquals(12L, result);
    then(readingClubMemberRepository).should().countByReadingClubId("c1");
  }

  @Test
  void isUserMember_deberiaDelegarEnRepo() {
    // given
    given(readingClubMemberRepository.existsByReadingClubIdAndUserId("c1", "u1")).willReturn(true);

    // when
    boolean result = sut.isUserMember("c1", "u1");

    // then
    assertTrue(result);
    then(readingClubMemberRepository).should().existsByReadingClubIdAndUserId("c1", "u1");
  }

  // ---------------- updateMeeting ----------------

  @Test
  void updateMeeting_deberiaRetornarEmpty_siClubNoExiste() {
    // given
    given(readingClubRepository.findById("c1")).willReturn(Optional.empty());

    // when
    Optional<ReadingClub> result = sut.updateMeeting("c1", "u1", LocalDateTime.now(), 10);

    // then
    assertTrue(result.isEmpty());
    then(readingClubRepository).should(never()).save(any());
  }

  @Test
  void updateMeeting_deberiaRetornarEmpty_siUserNoEsModerador() {
    // given
    ReadingClubEntity club = ReadingClubEntity.builder().id("c1").moderatorId("u9").build();
    given(readingClubRepository.findById("c1")).willReturn(Optional.of(club));

    // when
    Optional<ReadingClub> result = sut.updateMeeting("c1", "u1", LocalDateTime.now(), 10);

    // then
    assertTrue(result.isEmpty());
    then(readingClubRepository).should(never()).save(any());
  }

  @Test
  void updateMeeting_deberiaGuardar_yRetornarClubActualizado_conMemberCount() {
    // given
    ReadingClubEntity club = ReadingClubEntity.builder().id("c1").moderatorId("u1").build();
    given(readingClubRepository.findById("c1")).willReturn(Optional.of(club));

    LocalDateTime next = LocalDateTime.now().plusDays(2);

    given(readingClubRepository.save(any(ReadingClubEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, ReadingClubEntity.class));

    given(readingClubMemberRepository.countByReadingClubId("c1")).willReturn(6L);

    ArgumentCaptor<ReadingClubEntity> captor = ArgumentCaptor.forClass(ReadingClubEntity.class);

    // when
    Optional<ReadingClub> result = sut.updateMeeting("c1", "u1", next, 15);

    // then
    assertTrue(result.isPresent());
    assertEquals("c1", result.get().getId());
    assertEquals(6L, result.get().getMemberCount());
    assertEquals(15, result.get().getCurrentChapter());
    assertEquals(next, result.get().getNextMeeting());

    then(readingClubRepository).should().save(captor.capture());
    ReadingClubEntity saved = captor.getValue();
    assertEquals(next, saved.getNextMeeting());
    assertEquals(15, saved.getCurrentChapter());
    assertNotNull(saved.getLastUpdated());
  }

  @Test
  void updateMeeting_deberiaRetornarEmpty_siExplotaExcepcion() {
    // given
    ReadingClubEntity club = ReadingClubEntity.builder().id("c1").moderatorId("u1").build();
    given(readingClubRepository.findById("c1")).willReturn(Optional.of(club));

    willThrow(new RuntimeException("boom"))
        .given(readingClubRepository)
        .save(any(ReadingClubEntity.class));

    // when
    Optional<ReadingClub> result = sut.updateMeeting("c1", "u1", LocalDateTime.now(), 1);

    // then
    assertTrue(result.isEmpty());
  }
}
