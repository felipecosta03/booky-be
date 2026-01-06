package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.model.Comment;
import com.uade.bookybe.core.usecase.GamificationService;
import com.uade.bookybe.infraestructure.entity.CommentEntity;
import com.uade.bookybe.infraestructure.repository.CommentRepository;
import com.uade.bookybe.infraestructure.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private GamificationService gamificationService;

    @InjectMocks private CommentServiceImpl sut;

    // ---------------- createComment ----------------

    @Test
    void createComment_deberiaLanzarNotFound_siPostNoExiste() {
        // given
        given(postRepository.existsById("p1")).willReturn(false);

        // when + then
        NotFoundException ex =
                assertThrows(NotFoundException.class, () -> sut.createComment("u1", "p1", "hola"));
        assertTrue(ex.getMessage().contains("Post not found"));

        then(commentRepository).shouldHaveNoInteractions();
        then(gamificationService).shouldHaveNoInteractions();
    }

    @Test
    void createComment_deberiaGuardar_yRetornarComment_yOtorgarGamification() {
        // given
        given(postRepository.existsById("p1")).willReturn(true);

        given(commentRepository.save(any(CommentEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, CommentEntity.class));

        ArgumentCaptor<CommentEntity> captor = ArgumentCaptor.forClass(CommentEntity.class);

        // when
        Optional<Comment> result = sut.createComment("u1", "p1", "hola");

        // then
        assertTrue(result.isPresent());
        assertEquals("u1", result.get().getUserId());
        assertEquals("p1", result.get().getPostId());
        assertEquals("hola", result.get().getBody());
        assertNotNull(result.get().getDateCreated());

        then(commentRepository).should().save(captor.capture());
        CommentEntity saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals("u1", saved.getUserId());
        assertEquals("p1", saved.getPostId());
        assertEquals("hola", saved.getBody());
        assertNotNull(saved.getDateCreated());

        then(gamificationService).should().processCommentCreated("u1");
    }

    @Test
    void createComment_deberiaRetornarEmpty_siRepositorioTiraExcepcion() {
        // given
        given(postRepository.existsById("p1")).willReturn(true);
        willThrow(new RuntimeException("boom"))
                .given(commentRepository).save(any(CommentEntity.class));

        // when
        Optional<Comment> result = sut.createComment("u1", "p1", "hola");

        // then
        assertTrue(result.isEmpty());
        then(gamificationService).shouldHaveNoInteractions();
    }

    // ---------------- getCommentById ----------------

    @Test
    void getCommentById_deberiaMapear_yRetornarOptional() {
        // given
        CommentEntity entity = CommentEntity.builder().id("c1").userId("u1").postId("p1").body("x").build();
        given(commentRepository.findById("c1")).willReturn(Optional.of(entity));

        // when
        Optional<Comment> result = sut.getCommentById("c1");

        // then
        assertTrue(result.isPresent());
        assertEquals("c1", result.get().getId());
        then(commentRepository).should().findById("c1");
    }

    @Test
    void getCommentById_deberiaRetornarEmpty_siNoExiste() {
        // given
        given(commentRepository.findById("c1")).willReturn(Optional.empty());

        // when
        Optional<Comment> result = sut.getCommentById("c1");

        // then
        assertTrue(result.isEmpty());
    }

    // ---------------- getCommentsByPostId ----------------

    @Test
    void getCommentsByPostId_deberiaMapearLista() {
        // given
        given(commentRepository.findByPostIdWithUserOrderByDateCreatedDesc("p1"))
                .willReturn(List.of(
                        CommentEntity.builder().id("c1").postId("p1").userId("u1").body("a").build(),
                        CommentEntity.builder().id("c2").postId("p1").userId("u2").body("b").build()
                ));

        // when
        List<Comment> result = sut.getCommentsByPostId("p1");

        // then
        assertEquals(2, result.size());
        assertEquals("c1", result.get(0).getId());
        assertEquals("c2", result.get(1).getId());
        then(commentRepository).should().findByPostIdWithUserOrderByDateCreatedDesc("p1");
    }

    // ---------------- getCommentsByUserId ----------------

    @Test
    void getCommentsByUserId_deberiaMapearLista() {
        // given
        given(commentRepository.findByUserIdOrderByDateCreatedDesc("u1"))
                .willReturn(List.of(
                        CommentEntity.builder().id("c1").postId("p1").userId("u1").body("a").build()
                ));

        // when
        List<Comment> result = sut.getCommentsByUserId("u1");

        // then
        assertEquals(1, result.size());
        assertEquals("c1", result.get(0).getId());
        then(commentRepository).should().findByUserIdOrderByDateCreatedDesc("u1");
    }

    // ---------------- updateComment ----------------

    @Test
    void updateComment_deberiaRetornarEmpty_siNoExiste() {
        // given
        given(commentRepository.findById("c1")).willReturn(Optional.empty());

        // when
        Optional<Comment> result = sut.updateComment("c1", "u1", "nuevo");

        // then
        assertTrue(result.isEmpty());
        then(commentRepository).should(never()).save(any(CommentEntity.class));
    }

    @Test
    void updateComment_deberiaRetornarEmpty_siUserNoEsAutor() {
        // given
        CommentEntity entity = CommentEntity.builder().id("c1").userId("u9").postId("p1").body("a").build();
        given(commentRepository.findById("c1")).willReturn(Optional.of(entity));

        // when
        Optional<Comment> result = sut.updateComment("c1", "u1", "nuevo");

        // then
        assertTrue(result.isEmpty());
        then(commentRepository).should(never()).save(any(CommentEntity.class));
    }

    @Test
    void updateComment_deberiaActualizar_yRetornarComment_siUserEsAutor() {
        // given
        CommentEntity entity = CommentEntity.builder().id("c1").userId("u1").postId("p1").body("a").build();
        given(commentRepository.findById("c1")).willReturn(Optional.of(entity));

        given(commentRepository.save(any(CommentEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, CommentEntity.class));

        ArgumentCaptor<CommentEntity> captor = ArgumentCaptor.forClass(CommentEntity.class);

        // when
        Optional<Comment> result = sut.updateComment("c1", "u1", "nuevo");

        // then
        assertTrue(result.isPresent());
        assertEquals("nuevo", result.get().getBody());

        then(commentRepository).should().save(captor.capture());
        assertEquals("nuevo", captor.getValue().getBody());
    }

    // ---------------- deleteComment ----------------

    @Test
    void deleteComment_deberiaRetornarFalse_siNoExiste() {
        // given
        given(commentRepository.findById("c1")).willReturn(Optional.empty());

        // when
        boolean result = sut.deleteComment("c1", "u1");

        // then
        assertFalse(result);
        then(commentRepository).should(never()).delete(any(CommentEntity.class));
    }

    @Test
    void deleteComment_deberiaRetornarFalse_siUserNoEsAutor() {
        // given
        CommentEntity entity = CommentEntity.builder().id("c1").userId("u9").build();
        given(commentRepository.findById("c1")).willReturn(Optional.of(entity));

        // when
        boolean result = sut.deleteComment("c1", "u1");

        // then
        assertFalse(result);
        then(commentRepository).should(never()).delete(any(CommentEntity.class));
    }

    @Test
    void deleteComment_deberiaEliminar_yRetornarTrue_siUserEsAutor() {
        // given
        CommentEntity entity = CommentEntity.builder().id("c1").userId("u1").build();
        given(commentRepository.findById("c1")).willReturn(Optional.of(entity));

        willDoNothing().given(commentRepository).delete(entity);

        // when
        boolean result = sut.deleteComment("c1", "u1");

        // then
        assertTrue(result);
        then(commentRepository).should().delete(entity);
    }

    @Test
    void deleteComment_deberiaRetornarFalse_siDeleteTiraExcepcion() {
        // given
        CommentEntity entity = CommentEntity.builder().id("c1").userId("u1").build();
        given(commentRepository.findById("c1")).willReturn(Optional.of(entity));

        willThrow(new RuntimeException("boom")).given(commentRepository).delete(entity);

        // when
        boolean result = sut.deleteComment("c1", "u1");

        // then
        assertFalse(result);
    }

    // ---------------- countCommentsByPostId ----------------

    @Test
    void countCommentsByPostId_deberiaDelegarEnRepo() {
        // given
        given(commentRepository.countByPostId("p1")).willReturn(7);

        // when
        Integer result = sut.countCommentsByPostId("p1");

        // then
        assertEquals(7, result);
        then(commentRepository).should().countByPostId("p1");
    }
}
