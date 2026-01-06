package com.uade.bookybe.core.usecase.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

import com.uade.bookybe.core.exception.BadRequestException;
import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.model.UserRate;
import com.uade.bookybe.core.model.constant.ExchangeStatus;
import com.uade.bookybe.infraestructure.entity.BookExchangeEntity;
import com.uade.bookybe.infraestructure.entity.UserRateEntity;
import com.uade.bookybe.infraestructure.repository.BookExchangeRepository;
import com.uade.bookybe.infraestructure.repository.UserRateRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRateServiceImplTest {

    @Mock private UserRateRepository userRateRepository;
    @Mock private BookExchangeRepository bookExchangeRepository;

    @InjectMocks private UserRateServiceImpl sut;

    // ---------------- createRating ----------------

    @Test
    void createRating_deberiaLanzarBadRequest_cuandoRatingMenorA1() {
        // when + then
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> sut.createRating("ex1", "u1", 0, "c")
        );
        assertTrue(ex.getMessage().contains("between 1 and 5"));

        then(bookExchangeRepository).shouldHaveNoInteractions();
        then(userRateRepository).shouldHaveNoInteractions();
    }

    @Test
    void createRating_deberiaLanzarBadRequest_cuandoRatingMayorA5() {
        // when + then
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> sut.createRating("ex1", "u1", 6, "c")
        );
        assertTrue(ex.getMessage().contains("between 1 and 5"));

        then(bookExchangeRepository).shouldHaveNoInteractions();
        then(userRateRepository).shouldHaveNoInteractions();
    }

    @Test
    void createRating_deberiaLanzarNotFound_cuandoExchangeNoExiste() {
        // given
        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.empty());

        // when + then
        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> sut.createRating("ex1", "u1", 5, "ok")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("exchange not found"));

        then(userRateRepository).shouldHaveNoInteractions();
    }

    @Test
    void createRating_deberiaLanzarBadRequest_cuandoExchangeNoEstaCompleted() {
        // given
        BookExchangeEntity exchange = BookExchangeEntity.builder()
                .id("ex1")
                .status(ExchangeStatus.PENDING)
                .requesterId("u1")
                .ownerId("u2")
                .build();

        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.of(exchange));

        // when + then
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> sut.createRating("ex1", "u1", 5, "ok")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("completed"));

        then(userRateRepository).shouldHaveNoInteractions();
    }

    @Test
    void createRating_deberiaLanzarBadRequest_cuandoUserNoEsParteDelExchange() {
        // given
        BookExchangeEntity exchange = BookExchangeEntity.builder()
                .id("ex1")
                .status(ExchangeStatus.COMPLETED)
                .requesterId("u1")
                .ownerId("u2")
                .build();

        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.of(exchange));

        // when + then
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> sut.createRating("ex1", "u3", 5, "ok")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("not part"));

        then(userRateRepository).shouldHaveNoInteractions();
    }

    @Test
    void createRating_deberiaLanzarBadRequest_cuandoUserYaRateoElExchange() {
        // given
        BookExchangeEntity exchange = BookExchangeEntity.builder()
                .id("ex1")
                .status(ExchangeStatus.COMPLETED)
                .requesterId("u1")
                .ownerId("u2")
                .build();

        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.of(exchange));
        given(userRateRepository.existsByUserIdAndExchangeId("u1", "ex1")).willReturn(true);

        // when + then
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> sut.createRating("ex1", "u1", 5, "ok")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("already rated"));

        then(userRateRepository).should(never()).save(any(UserRateEntity.class));
    }

    @Test
    void createRating_deberiaGuardarYRetornarRating_cuandoValido_yUserEsRequester() {
        // given
        String exchangeId = "ex1";
        String userId = "u1";
        int rating = 4;
        String comment = "bien";

        BookExchangeEntity exchange = BookExchangeEntity.builder()
                .id(exchangeId)
                .status(ExchangeStatus.COMPLETED)
                .requesterId(userId)
                .ownerId("u2")
                .build();

        given(bookExchangeRepository.findById(exchangeId)).willReturn(Optional.of(exchange));
        given(userRateRepository.existsByUserIdAndExchangeId(userId, exchangeId)).willReturn(false);

        given(userRateRepository.save(any(UserRateEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, UserRateEntity.class));

        ArgumentCaptor<UserRateEntity> captor = ArgumentCaptor.forClass(UserRateEntity.class);

        // when
        Optional<UserRate> result = sut.createRating(exchangeId, userId, rating, comment);

        // then
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUserId());
        assertEquals(exchangeId, result.get().getExchangeId());
        assertEquals(rating, result.get().getRating());
        assertEquals(comment, result.get().getComment());
        assertNotNull(result.get().getDateCreated());

        then(userRateRepository).should().save(captor.capture());
        UserRateEntity saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals(userId, saved.getUserId());
        assertEquals(exchangeId, saved.getExchangeId());
        assertEquals(rating, saved.getRating());
        assertEquals(comment, saved.getComment());
        assertNotNull(saved.getDateCreated());
    }

    @Test
    void createRating_deberiaGuardarYRetornarRating_cuandoValido_yUserEsOwner() {
        // given
        String exchangeId = "ex1";
        String userId = "u2";

        BookExchangeEntity exchange = BookExchangeEntity.builder()
                .id(exchangeId)
                .status(ExchangeStatus.COMPLETED)
                .requesterId("u1")
                .ownerId(userId)
                .build();

        given(bookExchangeRepository.findById(exchangeId)).willReturn(Optional.of(exchange));
        given(userRateRepository.existsByUserIdAndExchangeId(userId, exchangeId)).willReturn(false);
        given(userRateRepository.save(any(UserRateEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, UserRateEntity.class));

        // when
        Optional<UserRate> result = sut.createRating(exchangeId, userId, 5, null);

        // then
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUserId());
        assertEquals(exchangeId, result.get().getExchangeId());
        assertEquals(5, result.get().getRating());
        assertNull(result.get().getComment());
    }

    // ---------------- getUserRatings ----------------

    @Test
    void getUserRatings_deberiaMapearYRetornarLista() {
        // given
        String userId = "u1";
        given(userRateRepository.findRatingsForUser(userId))
                .willReturn(List.of(
                        UserRateEntity.builder().id("r1").userId(userId).exchangeId("ex1").rating(4).build(),
                        UserRateEntity.builder().id("r2").userId(userId).exchangeId("ex2").rating(5).build()
                ));

        // when
        List<UserRate> result = sut.getUserRatings(userId);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("r1", result.get(0).getId());
        assertEquals("r2", result.get(1).getId());
        then(userRateRepository).should().findRatingsForUser(userId);
    }

    // ---------------- getExchangeRatings ----------------

    @Test
    void getExchangeRatings_deberiaMapearYRetornarLista() {
        // given
        String exchangeId = "ex1";
        given(userRateRepository.findByExchangeIdOrderByDateCreatedDesc(exchangeId))
                .willReturn(List.of(
                        UserRateEntity.builder().id("r1").exchangeId(exchangeId).userId("u1").rating(3).build()
                ));

        // when
        List<UserRate> result = sut.getExchangeRatings(exchangeId);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("r1", result.get(0).getId());
        then(userRateRepository).should().findByExchangeIdOrderByDateCreatedDesc(exchangeId);
    }

    // ---------------- canUserRateExchange ----------------

    @Test
    void canUserRateExchange_deberiaRetornarFalse_cuandoExchangeNoExiste() {
        // given
        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.empty());

        // when
        boolean result = sut.canUserRateExchange("ex1", "u1");

        // then
        assertFalse(result);
        then(userRateRepository).shouldHaveNoInteractions();
    }

    @Test
    void canUserRateExchange_deberiaRetornarFalse_cuandoExchangeNoCompleted() {
        // given
        BookExchangeEntity exchange = BookExchangeEntity.builder()
                .id("ex1")
                .status(ExchangeStatus.CANCELLED)
                .requesterId("u1")
                .ownerId("u2")
                .build();
        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.of(exchange));

        // when
        boolean result = sut.canUserRateExchange("ex1", "u1");

        // then
        assertFalse(result);
        then(userRateRepository).shouldHaveNoInteractions();
    }

    @Test
    void canUserRateExchange_deberiaRetornarFalse_cuandoUserNoEsParte() {
        // given
        BookExchangeEntity exchange = BookExchangeEntity.builder()
                .id("ex1")
                .status(ExchangeStatus.COMPLETED)
                .requesterId("u1")
                .ownerId("u2")
                .build();
        given(bookExchangeRepository.findById("ex1")).willReturn(Optional.of(exchange));

        // when
        boolean result = sut.canUserRateExchange("ex1", "u3");

        // then
        assertFalse(result);
        then(userRateRepository).shouldHaveNoInteractions();
    }

    @Test
    void canUserRateExchange_deberiaRetornarFalse_cuandoUserYaRateo() {
        // given
        String exchangeId = "ex1";
        String userId = "u1";

        BookExchangeEntity exchange = BookExchangeEntity.builder()
                .id(exchangeId)
                .status(ExchangeStatus.COMPLETED)
                .requesterId(userId)
                .ownerId("u2")
                .build();
        given(bookExchangeRepository.findById(exchangeId)).willReturn(Optional.of(exchange));

        given(userRateRepository.existsByUserIdAndExchangeId(userId, exchangeId)).willReturn(true);

        // when
        boolean result = sut.canUserRateExchange(exchangeId, userId);

        // then
        assertFalse(result);
    }

    @Test
    void canUserRateExchange_deberiaRetornarTrue_cuandoCompleted_userEsParte_yNoRateo() {
        // given
        String exchangeId = "ex1";
        String userId = "u1";

        BookExchangeEntity exchange = BookExchangeEntity.builder()
                .id(exchangeId)
                .status(ExchangeStatus.COMPLETED)
                .requesterId(userId)
                .ownerId("u2")
                .build();
        given(bookExchangeRepository.findById(exchangeId)).willReturn(Optional.of(exchange));

        given(userRateRepository.existsByUserIdAndExchangeId(userId, exchangeId)).willReturn(false);

        // when
        boolean result = sut.canUserRateExchange(exchangeId, userId);

        // then
        assertTrue(result);
    }

    // ---------------- averages / counts / exists ----------------

    @Test
    void getUserAverageRating_deberiaDelegarEnRepo() {
        // given
        given(userRateRepository.getAverageRatingForUser("u1")).willReturn(4.25);

        // when
        Double result = sut.getUserAverageRating("u1");

        // then
        assertEquals(4.25, result);
        then(userRateRepository).should().getAverageRatingForUser("u1");
    }

    @Test
    void getUserRatingCount_deberiaDelegarEnRepo() {
        // given
        given(userRateRepository.countRatingsForUser("u1")).willReturn(10L);

        // when
        Long result = sut.getUserRatingCount("u1");

        // then
        assertEquals(10L, result);
        then(userRateRepository).should().countRatingsForUser("u1");
    }

    @Test
    void hasUserRatedExchange_deberiaDelegarEnRepo() {
        // given
        given(userRateRepository.existsByUserIdAndExchangeId("u1", "ex1")).willReturn(true);

        // when
        boolean result = sut.hasUserRatedExchange("ex1", "u1");

        // then
        assertTrue(result);
        then(userRateRepository).should().existsByUserIdAndExchangeId("u1", "ex1");
    }
}
