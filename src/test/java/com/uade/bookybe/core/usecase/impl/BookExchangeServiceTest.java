package com.uade.bookybe.core.usecase.impl;

import static com.uade.bookybe.core.model.constant.ExchangeStatus.ACCEPTED;
import static com.uade.bookybe.core.model.constant.ExchangeStatus.COMPLETED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

import com.uade.bookybe.core.exception.BadRequestException;
import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.model.BookExchange;
import com.uade.bookybe.core.model.Chat;
import com.uade.bookybe.core.model.UserRate;
import com.uade.bookybe.core.model.constant.ExchangeStatus;
import com.uade.bookybe.core.usecase.ChatService;
import com.uade.bookybe.core.usecase.GamificationService;
import com.uade.bookybe.core.usecase.UserRateService;
import com.uade.bookybe.infraestructure.entity.BookExchangeEntity;
import com.uade.bookybe.infraestructure.entity.UserBookEntity;
import com.uade.bookybe.infraestructure.repository.BookExchangeRepository;
import com.uade.bookybe.infraestructure.repository.UserBookRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookExchangeServiceImplTest {

    @Mock private BookExchangeRepository bookExchangeRepository;
    @Mock private UserBookRepository userBookRepository;
    @Mock private GamificationService gamificationService;
    @Mock private UserRateService userRateService;
    @Mock private ChatService chatService;

    @InjectMocks private BookExchangeServiceImpl sut;

    // ---------------- createExchange ----------------

    @Test
    void createExchange_deberiaRetornarEmpty_cuandoRequesterYOwnerSonIguales() {
        // when
        Optional<BookExchange> result =
                sut.createExchange("u1", "u1", List.of("b1"), List.of("b2"));

        // then
        assertTrue(result.isEmpty());
        then(bookExchangeRepository).shouldHaveNoInteractions();
        then(userBookRepository).shouldHaveNoInteractions();
        then(chatService).shouldHaveNoInteractions();
        then(gamificationService).shouldHaveNoInteractions();
    }


    @Test
    void createExchange_deberiaCrearChat_guardarExchange_yOtorgarGamification() {
        // given
        String requesterId = "u1";
        String ownerId = "u2";
        List<String> ownerBookIds = List.of("ob1", "ob2");
        List<String> requesterBookIds = List.of("rb1");

        // validar books
        for (String id : ownerBookIds) {
            given(userBookRepository.existsByUserIdAndBookId(ownerId, id)).willReturn(true);
        }
        for (String id : requesterBookIds) {
            given(userBookRepository.existsByUserIdAndBookId(requesterId, id)).willReturn(true);
        }

        given(chatService.createOrGetChat(requesterId, ownerId))
                .willReturn(Optional.of(Chat.builder().id("chat1").build()));

        given(bookExchangeRepository.save(any(BookExchangeEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, BookExchangeEntity.class));

        ArgumentCaptor<BookExchangeEntity> captor = ArgumentCaptor.forClass(BookExchangeEntity.class);

        // when
        Optional<BookExchange> result =
                sut.createExchange(requesterId, ownerId, ownerBookIds, requesterBookIds);

        // then
        assertTrue(result.isPresent());
        assertEquals(requesterId, result.get().getRequesterId());
        assertEquals(ownerId, result.get().getOwnerId());
        assertEquals(ExchangeStatus.PENDING, result.get().getStatus());
        assertEquals("chat1", result.get().getChatId());

        then(bookExchangeRepository).should().save(captor.capture());
        BookExchangeEntity saved = captor.getValue();
        assertNotNull(saved.getId());
        assertTrue(saved.getId().startsWith("exchange-"));
        assertEquals("chat1", saved.getChatId());
        assertEquals(ownerBookIds, saved.getOwnerBookIds());
        assertEquals(requesterBookIds, saved.getRequesterBookIds());

        then(gamificationService).should().processExchangeCreated(requesterId);
    }

    @Test
    void createExchange_deberiaPermitirChatNulo_siChatServiceDevuelveEmpty() {
        // given
        String requesterId = "u1";
        String ownerId = "u2";

        given(userBookRepository.existsByUserIdAndBookId(ownerId, "ob1")).willReturn(true);
        given(userBookRepository.existsByUserIdAndBookId(requesterId, "rb1")).willReturn(true);

        given(chatService.createOrGetChat(requesterId, ownerId)).willReturn(Optional.empty());
        given(bookExchangeRepository.save(any(BookExchangeEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, BookExchangeEntity.class));

        // when
        Optional<BookExchange> result =
                sut.createExchange(requesterId, ownerId, List.of("ob1"), List.of("rb1"));

        // then
        assertTrue(result.isPresent());
        assertNull(result.get().getChatId());
        then(gamificationService).should().processExchangeCreated(requesterId);
    }

    // ---------------- getUserExchanges / getUserExchangesByStatus ----------------

    @Test
    void getUserExchanges_deberiaMapear_yEnriquecerConBooks() {
        // given
        String userId = "u1";
        BookExchangeEntity e = BookExchangeEntity.builder()
                .id("ex1")
                .requesterId("u1")
                .ownerId("u2")
                .status(ExchangeStatus.PENDING)
                .ownerBookIds(List.of("ob1"))
                .requesterBookIds(List.of("rb1"))
                .build();

        given(bookExchangeRepository.findByUserIdOrderByDateCreatedDesc(userId)).willReturn(List.of(e));

        UserBookEntity ob = UserBookEntity.builder().id("ob1").userId("u2").bookId("ob1").build();
        UserBookEntity rb = UserBookEntity.builder().id("rb1").userId("u1").bookId("rb1").build();

        given(userBookRepository.findByIdInWithBook(List.of("ob1"), "u2")).willReturn(List.of(ob));
        given(userBookRepository.findByIdInWithBook(List.of("rb1"), "u1")).willReturn(List.of(rb));

        // when
        List<BookExchange> result = sut.getUserExchanges(userId);

        // then
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getOwnerBooks());
        assertEquals(1, result.get(0).getOwnerBooks().size());
        assertNotNull(result.get(0).getRequesterBooks());
        assertEquals(1, result.get(0).getRequesterBooks().size());
    }

    @Test
    void getUserExchangesByStatus_deberiaFiltrarPorEstado_yEnriquecer() {
        // given
        String userId = "u1";
        ExchangeStatus status = ExchangeStatus.PENDING;

        BookExchangeEntity e = BookExchangeEntity.builder()
                .id("ex1")
                .requesterId("u1")
                .ownerId("u2")
                .status(status)
                .ownerBookIds(List.of())
                .requesterBookIds(List.of())
                .build();

        given(bookExchangeRepository.findByUserIdAndStatusOrderByDateCreatedDesc(userId, status))
                .willReturn(List.of(e));

        // when
        List<BookExchange> result = sut.getUserExchangesByStatus(userId, status);

        // then
        assertEquals(1, result.size());
        assertEquals(status, result.get(0).getStatus());
        then(userBookRepository).shouldHaveNoInteractions(); // no hay ids
    }

    // ---------------- getExchangeById ----------------

    @Test
    void getExchangeById_deberiaRetornarEmpty_cuandoNoExiste() {
        // given
        given(bookExchangeRepository.findByIdWithUsers("ex1")).willReturn(Optional.empty());

        // when
        Optional<BookExchange> result = sut.getExchangeById("ex1");

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    void getExchangeById_deberiaEnriquecerConBooks_yRatings_siCompleted() {
        // given
        BookExchangeEntity e = BookExchangeEntity.builder()
                .id("ex1")
                .requesterId("u1")
                .ownerId("u2")
                .status(COMPLETED)
                .ownerBookIds(List.of("ob1"))
                .requesterBookIds(List.of("rb1"))
                .build();

        given(bookExchangeRepository.findByIdWithUsers("ex1")).willReturn(Optional.of(e));

        given(userBookRepository.findByIdInWithBook(List.of("ob1"), "u2"))
                .willReturn(List.of(UserBookEntity.builder().id("ob1").userId("u2").bookId("ob1").build()));
        given(userBookRepository.findByIdInWithBook(List.of("rb1"), "u1"))
                .willReturn(List.of(UserBookEntity.builder().id("rb1").userId("u1").bookId("rb1").build()));

        UserRate r1 = UserRate.builder().id("r1").userId("u1").exchangeId("ex1").rating(5).build();
        UserRate r2 = UserRate.builder().id("r2").userId("u2").exchangeId("ex1").rating(4).build();
        given(userRateService.getExchangeRatings("ex1")).willReturn(List.of(r1, r2));

        // when
        Optional<BookExchange> result = sut.getExchangeById("ex1");

        // then
        assertTrue(result.isPresent());
        assertNotNull(result.get().getOwnerBooks());
        assertNotNull(result.get().getRequesterBooks());
        assertNotNull(result.get().getRequesterRate());
        assertNotNull(result.get().getOwnerRate());
        assertEquals("r1", result.get().getRequesterRate().getId());
        assertEquals("r2", result.get().getOwnerRate().getId());
    }

    // ---------------- updateExchangeStatus ----------------

    @Test
    void updateExchangeStatus_deberiaLanzarNotFound_cuandoNoExiste() {
        // given
        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.empty());

        // when + then
        assertThrows(NotFoundException.class,
                () -> sut.updateExchangeStatus("ex1", "u1", ExchangeStatus.ACCEPTED));

        then(bookExchangeRepository).should(never()).save(any());
    }

    @Test
    void updateExchangeStatus_deberiaRetornarEmpty_cuandoNoTienePermiso_paraAceptar() {
        // given
        BookExchangeEntity e = BookExchangeEntity.builder()
                .id("ex1")
                .requesterId("u1")
                .ownerId("u2")
                .status(ExchangeStatus.PENDING)
                .build();

        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.of(e));

        // when (intenta aceptar siendo requester)
        Optional<BookExchange> result = sut.updateExchangeStatus("ex1", "u1", ExchangeStatus.ACCEPTED);

        // then
        assertTrue(result.isEmpty());
        then(bookExchangeRepository).should(never()).save(any());
        then(gamificationService).shouldHaveNoInteractions();
    }

    @Test
    void updateExchangeStatus_deberiaAceptar_cuandoOwnerAcepta() {
        // given
        BookExchangeEntity e = BookExchangeEntity.builder()
                .id("ex1")
                .requesterId("u1")
                .ownerId("u2")
                .status(ExchangeStatus.PENDING)
                .ownerBookIds(List.of())
                .requesterBookIds(List.of())
                .build();

        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.of(e));
        given(bookExchangeRepository.save(any(BookExchangeEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, BookExchangeEntity.class));

        // when
        Optional<BookExchange> result = sut.updateExchangeStatus("ex1", "u2", ExchangeStatus.ACCEPTED);

        // then
        assertTrue(result.isPresent());
        assertEquals(ExchangeStatus.ACCEPTED, result.get().getStatus());
        then(gamificationService).shouldHaveNoInteractions();
    }

    @Test
    void updateExchangeStatus_deberiaCancelarSoloSiRequester_yNoCompleted() {
        // given
        BookExchangeEntity e = BookExchangeEntity.builder()
                .id("ex1")
                .requesterId("u1")
                .ownerId("u2")
                .status(ExchangeStatus.PENDING)
                .ownerBookIds(List.of())
                .requesterBookIds(List.of())
                .build();

        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.of(e));
        given(bookExchangeRepository.save(any(BookExchangeEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, BookExchangeEntity.class));

        // when
        Optional<BookExchange> result = sut.updateExchangeStatus("ex1", "u1", ExchangeStatus.CANCELLED);

        // then
        assertTrue(result.isPresent());
        assertEquals(ExchangeStatus.CANCELLED, result.get().getStatus());
    }

    @Test
    void updateExchangeStatus_deberiaRetornarEmpty_siCancelarPeroYaCompleted() {
        // given
        BookExchangeEntity e = BookExchangeEntity.builder()
                .id("ex1")
                .requesterId("u1")
                .ownerId("u2")
                .status(COMPLETED)
                .build();

        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.of(e));

        // when
        Optional<BookExchange> result = sut.updateExchangeStatus("ex1", "u1", ExchangeStatus.CANCELLED);

        // then
        assertTrue(result.isEmpty());
        then(bookExchangeRepository).should(never()).save(any());
    }

    @Test
    void updateExchangeStatus_deberiaCompletar_yOtorgarPuntos_aAmbosUsuarios() {
        // given
        BookExchangeEntity e = BookExchangeEntity.builder()
                .id("ex1")
                .requesterId("u1")
                .ownerId("u2")
                .status(ACCEPTED) // requisito
                .ownerBookIds(List.of())
                .requesterBookIds(List.of())
                .build();

        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.of(e));
        given(bookExchangeRepository.save(any(BookExchangeEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, BookExchangeEntity.class));

        // when (marca completed requester)
        Optional<BookExchange> result = sut.updateExchangeStatus("ex1", "u1", COMPLETED);

        // then
        assertTrue(result.isPresent());
        assertEquals(COMPLETED, result.get().getStatus());
        then(gamificationService).should().processExchangeCompleted("u1");
        then(gamificationService).should().processExchangeCompleted("u2");
    }

    @Test
    void updateExchangeStatus_deberiaRetornarEmpty_siCompletarPeroNoEstaAccepted() {
        // given
        BookExchangeEntity e = BookExchangeEntity.builder()
                .id("ex1")
                .requesterId("u1")
                .ownerId("u2")
                .status(ExchangeStatus.PENDING)
                .build();

        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.of(e));

        // when
        Optional<BookExchange> result = sut.updateExchangeStatus("ex1", "u1", COMPLETED);

        // then
        assertTrue(result.isEmpty());
        then(bookExchangeRepository).should(never()).save(any());
    }

    // ---------------- createCounterOffer ----------------

    @Test
    void createCounterOffer_deberiaLanzarNotFound_cuandoNoExiste() {
        // given
        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.empty());

        // when + then
        assertThrows(NotFoundException.class,
                () -> sut.createCounterOffer("ex1", "u1", List.of(), List.of()));
    }

    @Test
    void createCounterOffer_deberiaRetornarEmpty_cuandoNoEsOwner() {
        // given
        BookExchangeEntity original = BookExchangeEntity.builder()
                .id("ex1")
                .requesterId("u1")
                .ownerId("u2")
                .status(ExchangeStatus.PENDING)
                .build();

        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.of(original));

        // when
        Optional<BookExchange> result =
                sut.createCounterOffer("ex1", "u1", List.of("x"), List.of("y"));

        // then
        assertTrue(result.isEmpty());
        then(bookExchangeRepository).should(never()).save(argThat(e -> !e.getId().equals("ex1")));
    }

    @Test
    void createCounterOffer_deberiaRetornarEmpty_cuandoStatusNoEsPending() {
        // given
        BookExchangeEntity original = BookExchangeEntity.builder()
                .id("ex1")
                .requesterId("u1")
                .ownerId("u2")
                .status(ExchangeStatus.ACCEPTED)
                .build();

        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.of(original));

        // when
        Optional<BookExchange> result =
                sut.createCounterOffer("ex1", "u2", List.of("x"), List.of("y"));

        // then
        assertTrue(result.isEmpty());
        then(bookExchangeRepository).should(never()).save(argThat(e -> !e.getId().equals("ex1")));
    }

    @Test
    void createCounterOffer_deberiaLanzarBadRequest_cuandoBooksInvalidos() {
        // given
        BookExchangeEntity original = BookExchangeEntity.builder()
                .id("ex1")
                .requesterId("u1")
                .ownerId("u2")
                .status(ExchangeStatus.PENDING)
                .chatId("chat1")
                .build();

        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.of(original));

        // validación invertida:
        // validateUserBooks(ownerId, requesterBookIds) => u2 + ["rb1"] => false
        given(userBookRepository.existsByUserIdAndBookId("u2", "rb1")).willReturn(false);

        // when + then
        assertThrows(BadRequestException.class,
                () -> sut.createCounterOffer("ex1", "u2", List.of("ob1"), List.of("rb1")));
    }

    @Test
    void createCounterOffer_deberiaMarcarOriginalRejected_yCrearNuevaExchange_invirtiendoRoles_yReusarChat() {
        // given
        BookExchangeEntity original = BookExchangeEntity.builder()
                .id("ex1")
                .requesterId("u1")
                .ownerId("u2")
                .status(ExchangeStatus.PENDING)
                .chatId("chat1")
                .build();

        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.of(original));

        // Validación invertida:
        // validateUserBooks(ownerId=u2, requesterBookIds=["rb1"]) => true
        given(userBookRepository.existsByUserIdAndBookId("u2", "rb1")).willReturn(true);
        // validateUserBooks(requesterId=u1, ownerBookIds=["ob1"]) => true
        given(userBookRepository.existsByUserIdAndBookId("u1", "ob1")).willReturn(true);

        // save 1: original (REJECTED), save 2: new exchange
        given(bookExchangeRepository.save(any(BookExchangeEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, BookExchangeEntity.class));

        ArgumentCaptor<BookExchangeEntity> captor = ArgumentCaptor.forClass(BookExchangeEntity.class);

        // when
        Optional<BookExchange> result =
                sut.createCounterOffer("ex1", "u2", List.of("ob1"), List.of("rb1"));

        // then
        assertTrue(result.isPresent());
        // roles invertidos:
        assertEquals("u2", result.get().getRequesterId()); // former owner
        assertEquals("u1", result.get().getOwnerId());     // former requester
        assertEquals("chat1", result.get().getChatId());
        assertEquals(ExchangeStatus.PENDING, result.get().getStatus());

        then(bookExchangeRepository).should(times(2)).save(captor.capture());
        List<BookExchangeEntity> saves = captor.getAllValues();

        BookExchangeEntity savedOriginal = saves.get(0);
        assertEquals("ex1", savedOriginal.getId());
        assertEquals(ExchangeStatus.REJECTED, savedOriginal.getStatus());

        BookExchangeEntity savedNew = saves.get(1);
        assertNotNull(savedNew.getId());
        assertTrue(savedNew.getId().startsWith("exchange-"));
        assertEquals("u2", savedNew.getRequesterId());
        assertEquals("u1", savedNew.getOwnerId());
        assertEquals("chat1", savedNew.getChatId());
        assertEquals(List.of("ob1"), savedNew.getOwnerBookIds());
        assertEquals(List.of("rb1"), savedNew.getRequesterBookIds());
    }

    // ---------------- getExchangesAsRequester / getExchangesAsOwner ----------------

    @Test
    void getExchangesAsRequester_deberiaMapear_yEnriquecer() {
        // given
        BookExchangeEntity e = BookExchangeEntity.builder()
                .id("ex1")
                .requesterId("u1")
                .ownerId("u2")
                .status(ExchangeStatus.PENDING)
                .ownerBookIds(List.of())
                .requesterBookIds(List.of())
                .build();
        given(bookExchangeRepository.findByRequesterIdOrderByDateCreatedDesc("u1")).willReturn(List.of(e));

        // when
        List<BookExchange> result = sut.getExchangesAsRequester("u1");

        // then
        assertEquals(1, result.size());
        assertEquals("u1", result.get(0).getRequesterId());
    }

    @Test
    void getExchangesAsOwner_deberiaMapear_yEnriquecer() {
        // given
        BookExchangeEntity e = BookExchangeEntity.builder()
                .id("ex1")
                .requesterId("u1")
                .ownerId("u2")
                .status(ExchangeStatus.PENDING)
                .ownerBookIds(List.of())
                .requesterBookIds(List.of())
                .build();
        given(bookExchangeRepository.findByOwnerIdOrderByDateCreatedDesc("u2")).willReturn(List.of(e));

        // when
        List<BookExchange> result = sut.getExchangesAsOwner("u2");

        // then
        assertEquals(1, result.size());
        assertEquals("u2", result.get(0).getOwnerId());
    }

    // ---------------- getPendingExchangesCount ----------------

    @Test
    void getPendingExchangesCount_deberiaSumarRequesterYOwner() {
        // given
        given(bookExchangeRepository.countPendingExchangesByRequester("u1")).willReturn(2L);
        given(bookExchangeRepository.countPendingExchangesByOwner("u1")).willReturn(3L);

        // when
        long result = sut.getPendingExchangesCount("u1");

        // then
        assertEquals(5L, result);
    }
}
