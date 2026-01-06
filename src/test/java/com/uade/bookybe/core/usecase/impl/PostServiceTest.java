package com.uade.bookybe.core.usecase.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.model.Post;
import com.uade.bookybe.core.port.ImageStoragePort;
import com.uade.bookybe.core.usecase.GamificationService;
import com.uade.bookybe.infraestructure.entity.PostEntity;
import com.uade.bookybe.infraestructure.repository.CommunityRepository;
import com.uade.bookybe.infraestructure.repository.PostRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

  @Mock private PostRepository postRepository;
  @Mock private ImageStoragePort imageStoragePort;
  @Mock private CommunityRepository communityRepository;
  @Mock private GamificationService gamificationService;

  @InjectMocks private PostServiceImpl sut;

  // ---------------- createPost ----------------

  @Test
  void createPost_deberiaLanzarNotFound_cuandoCommunityNoExiste_ySeEnviaCommunityId() {
    // given
    String userId = "u1";
    String communityId = "c1";

    given(communityRepository.existsById(communityId)).willReturn(false);

    // when + then
    NotFoundException ex =
        assertThrows(
            NotFoundException.class, () -> sut.createPost(userId, "body", communityId, null));
    assertTrue(ex.getMessage().contains("Community not found"));

    then(postRepository).shouldHaveNoInteractions();
    then(imageStoragePort).shouldHaveNoInteractions();
    then(gamificationService).shouldHaveNoInteractions();
  }

  @Test
  void createPost_deberiaCrearPost_sinImagen_yOtorgarGamification() {
    // given
    String userId = "u1";
    String body = "hola";
    String communityId = "c1";

    given(communityRepository.existsById(communityId)).willReturn(true);

    // devolvemos el mismo entity que se guarda
    given(postRepository.save(any(PostEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, PostEntity.class));

    ArgumentCaptor<PostEntity> captor = ArgumentCaptor.forClass(PostEntity.class);

    // when
    Optional<Post> result = sut.createPost(userId, body, communityId, null);

    // then
    assertTrue(result.isPresent());
    assertEquals(userId, result.get().getUserId());
    assertEquals(body, result.get().getBody());
    assertEquals(communityId, result.get().getCommunityId());

    then(postRepository).should().save(captor.capture());
    PostEntity saved = captor.getValue();
    assertNotNull(saved.getId());
    assertNotNull(saved.getDateCreated());
    assertEquals(userId, saved.getUserId());
    assertEquals(body, saved.getBody());
    assertEquals(communityId, saved.getCommunityId());
    assertNull(saved.getImage());

    then(imageStoragePort).shouldHaveNoInteractions();
    then(gamificationService).should().processPostCreated(userId);
  }

  @Test
  void createPost_deberiaSubirImagen_ySetearUrl_cuandoVieneBase64NoVacio() {
    // given
    String userId = "u1";
    String body = "hola";
    String communityId = null;
    String imageBase64 = "base64-xxx";

    // si communityId es null/vacio, no valida existencia
    given(imageStoragePort.uploadImage(imageBase64, "booky/posts"))
        .willReturn(Optional.of("https://img"));

    given(postRepository.save(any(PostEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, PostEntity.class));

    ArgumentCaptor<PostEntity> captor = ArgumentCaptor.forClass(PostEntity.class);

    // when
    Optional<Post> result = sut.createPost(userId, body, communityId, imageBase64);

    // then
    assertTrue(result.isPresent());
    assertEquals("https://img", result.get().getImage());

    then(imageStoragePort).should().uploadImage(imageBase64, "booky/posts");
    then(postRepository).should().save(captor.capture());
    assertEquals("https://img", captor.getValue().getImage());

    then(gamificationService).should().processPostCreated(userId);
  }

  @Test
  void createPost_noDeberiaSetearImagen_cuandoUploadDevuelveEmpty() {
    // given
    String userId = "u1";
    String body = "hola";
    String communityId = null;
    String imageBase64 = "base64-xxx";

    given(imageStoragePort.uploadImage(imageBase64, "booky/posts")).willReturn(Optional.empty());

    given(postRepository.save(any(PostEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, PostEntity.class));

    ArgumentCaptor<PostEntity> captor = ArgumentCaptor.forClass(PostEntity.class);

    // when
    Optional<Post> result = sut.createPost(userId, body, communityId, imageBase64);

    // then
    assertTrue(result.isPresent());
    assertNull(result.get().getImage());

    then(imageStoragePort).should().uploadImage(imageBase64, "booky/posts");
    then(postRepository).should().save(captor.capture());
    assertNull(captor.getValue().getImage());

    then(gamificationService).should().processPostCreated(userId);
  }

  @Test
  void createPost_deberiaRetornarEmpty_cuandoFallaElSave() {
    // given
    String userId = "u1";
    given(postRepository.save(any(PostEntity.class))).willThrow(new RuntimeException("boom"));

    // when
    Optional<Post> result = sut.createPost(userId, "body", null, null);

    // then
    assertTrue(result.isEmpty());
    then(gamificationService).shouldHaveNoInteractions();
  }

  // ---------------- getPostById ----------------

  @Test
  void getPostById_deberiaMapearYRetornarOptional() {
    // given
    String postId = "p1";
    given(postRepository.findById(postId))
        .willReturn(Optional.of(PostEntity.builder().id(postId).userId("u1").body("b").build()));

    // when
    Optional<Post> result = sut.getPostById(postId);

    // then
    assertTrue(result.isPresent());
    assertEquals(postId, result.get().getId());
    then(postRepository).should().findById(postId);
  }

  @Test
  void getPostById_deberiaRetornarEmpty_cuandoNoExiste() {
    // given
    given(postRepository.findById("p1")).willReturn(Optional.empty());

    // when
    Optional<Post> result = sut.getPostById("p1");

    // then
    assertTrue(result.isEmpty());
  }

  // ---------------- list queries ----------------

  @Test
  void getPostsByUserId_deberiaMapearYRetornarLista() {
    // given
    String userId = "u1";
    given(postRepository.findByUserIdOrderByDateCreatedDesc(userId))
        .willReturn(
            List.of(
                PostEntity.builder().id("p1").userId(userId).body("1").build(),
                PostEntity.builder().id("p2").userId(userId).body("2").build()));

    // when
    List<Post> result = sut.getPostsByUserId(userId);

    // then
    assertEquals(2, result.size());
    assertEquals("p1", result.get(0).getId());
    then(postRepository).should().findByUserIdOrderByDateCreatedDesc(userId);
  }

  @Test
  void getPostsByCommunityId_deberiaMapearYRetornarLista() {
    // given
    String communityId = "c1";
    given(postRepository.findByCommunityIdOrderByDateCreatedDesc(communityId))
        .willReturn(
            List.of(
                PostEntity.builder()
                    .id("p1")
                    .communityId(communityId)
                    .userId("u1")
                    .body("1")
                    .build()));

    // when
    List<Post> result = sut.getPostsByCommunityId(communityId);

    // then
    assertEquals(1, result.size());
    assertEquals(communityId, result.get(0).getCommunityId());
    then(postRepository).should().findByCommunityIdOrderByDateCreatedDesc(communityId);
  }

  @Test
  void getGeneralPosts_deberiaMapearYRetornarLista() {
    // given
    given(postRepository.findGeneralPostsOrderByDateCreatedDesc())
        .willReturn(List.of(PostEntity.builder().id("p1").userId("u1").body("1").build()));

    // when
    List<Post> result = sut.getGeneralPosts();

    // then
    assertEquals(1, result.size());
    then(postRepository).should().findGeneralPostsOrderByDateCreatedDesc();
  }

  @Test
  void getAllPosts_deberiaMapearYRetornarLista() {
    // given
    given(postRepository.findAllWithUserOrderByDateCreatedDesc())
        .willReturn(
            List.of(
                PostEntity.builder().id("p1").userId("u1").body("1").build(),
                PostEntity.builder().id("p2").userId("u2").body("2").build()));

    // when
    List<Post> result = sut.getAllPosts();

    // then
    assertEquals(2, result.size());
    then(postRepository).should().findAllWithUserOrderByDateCreatedDesc();
  }

  @Test
  void getUserFeed_deberiaMapearYRetornarLista() {
    // given
    String userId = "u1";
    given(postRepository.findPostsFromFollowedUsers(userId))
        .willReturn(List.of(PostEntity.builder().id("p1").userId("u2").body("1").build()));

    // when
    List<Post> result = sut.getUserFeed(userId);

    // then
    assertEquals(1, result.size());
    then(postRepository).should().findPostsFromFollowedUsers(userId);
  }

  // ---------------- getPostsFiltered ----------------

  @Test
  void getPostsFiltered_deberiaRetornarFeed_cuandoTypeEsFeed_yRequestingUserNoNull() {
    // given
    given(postRepository.findPostsFromFollowedUsers("u1"))
        .willReturn(List.of(PostEntity.builder().id("p1").userId("u2").body("b").build()));

    // when
    List<Post> result = sut.getPostsFiltered("feed", null, null, "u1");

    // then
    assertEquals(1, result.size());
    then(postRepository).should().findPostsFromFollowedUsers("u1");
    then(postRepository).should(never()).findGeneralPostsOrderByDateCreatedDesc();
    then(postRepository).should(never()).findAllWithUserOrderByDateCreatedDesc();
  }

  @Test
  void getPostsFiltered_deberiaRetornarGeneral_cuandoTypeEsGeneral() {
    // given
    given(postRepository.findGeneralPostsOrderByDateCreatedDesc())
        .willReturn(List.of(PostEntity.builder().id("p1").userId("u1").body("b").build()));

    // when
    List<Post> result = sut.getPostsFiltered("general", null, null, "uX");

    // then
    assertEquals(1, result.size());
    then(postRepository).should().findGeneralPostsOrderByDateCreatedDesc();
  }

  @Test
  void getPostsFiltered_deberiaRetornarPorUserId_cuandoUserIdNoNull() {
    // given
    given(postRepository.findByUserIdOrderByDateCreatedDesc("u1"))
        .willReturn(List.of(PostEntity.builder().id("p1").userId("u1").body("b").build()));

    // when
    List<Post> result = sut.getPostsFiltered("otro", "u1", null, null);

    // then
    assertEquals(1, result.size());
    then(postRepository).should().findByUserIdOrderByDateCreatedDesc("u1");
  }

  @Test
  void getPostsFiltered_deberiaRetornarPorCommunityId_cuandoCommunityIdNoNull_yUserIdNull() {
    // given
    given(postRepository.findByCommunityIdOrderByDateCreatedDesc("c1"))
        .willReturn(
            List.of(
                PostEntity.builder().id("p1").communityId("c1").userId("u1").body("b").build()));

    // when
    List<Post> result = sut.getPostsFiltered("otro", null, "c1", null);

    // then
    assertEquals(1, result.size());
    then(postRepository).should().findByCommunityIdOrderByDateCreatedDesc("c1");
  }

  @Test
  void getPostsFiltered_deberiaRetornarAll_cuandoNoHayFiltros() {
    // given
    given(postRepository.findAllWithUserOrderByDateCreatedDesc())
        .willReturn(List.of(PostEntity.builder().id("p1").userId("u1").body("b").build()));

    // when
    List<Post> result = sut.getPostsFiltered(null, null, null, null);

    // then
    assertEquals(1, result.size());
    then(postRepository).should().findAllWithUserOrderByDateCreatedDesc();
  }

  // ---------------- updatePost ----------------

  @Test
  void updatePost_deberiaRetornarEmpty_cuandoNoExiste() {
    // given
    given(postRepository.findById("p1")).willReturn(Optional.empty());

    // when
    Optional<Post> result = sut.updatePost("p1", "u1", "nuevo");

    // then
    assertTrue(result.isEmpty());
    then(postRepository).should(never()).save(any(PostEntity.class));
  }

  @Test
  void updatePost_deberiaRetornarEmpty_cuandoUsuarioNoEsAutor() {
    // given
    PostEntity entity = PostEntity.builder().id("p1").userId("author").body("old").build();
    given(postRepository.findById("p1")).willReturn(Optional.of(entity));

    // when
    Optional<Post> result = sut.updatePost("p1", "otro", "nuevo");

    // then
    assertTrue(result.isEmpty());
    then(postRepository).should(never()).save(any(PostEntity.class));
  }

  @Test
  void updatePost_deberiaActualizarBody_yGuardar() {
    // given
    PostEntity entity = PostEntity.builder().id("p1").userId("u1").body("old").build();
    given(postRepository.findById("p1")).willReturn(Optional.of(entity));

    given(postRepository.save(any(PostEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, PostEntity.class));

    // when
    Optional<Post> result = sut.updatePost("p1", "u1", "nuevo");

    // then
    assertTrue(result.isPresent());
    assertEquals("nuevo", result.get().getBody());
    then(postRepository).should().save(argThat(e -> "nuevo".equals(e.getBody())));
  }

  // ---------------- deletePost ----------------

  @Test
  void deletePost_deberiaRetornarFalse_cuandoNoExiste() {
    // given
    given(postRepository.findById("p1")).willReturn(Optional.empty());

    // when
    boolean result = sut.deletePost("p1", "u1");

    // then
    assertFalse(result);
    then(postRepository).should(never()).delete(any(PostEntity.class));
  }

  @Test
  void deletePost_deberiaRetornarFalse_cuandoUsuarioNoEsAutor() {
    // given
    PostEntity entity = PostEntity.builder().id("p1").userId("author").build();
    given(postRepository.findById("p1")).willReturn(Optional.of(entity));

    // when
    boolean result = sut.deletePost("p1", "otro");

    // then
    assertFalse(result);
    then(postRepository).should(never()).delete(any(PostEntity.class));
  }

  @Test
  void deletePost_deberiaBorrarYRetornarTrue_cuandoAutorCorrecto() {
    // given
    PostEntity entity = PostEntity.builder().id("p1").userId("u1").build();
    given(postRepository.findById("p1")).willReturn(Optional.of(entity));

    willDoNothing().given(postRepository).delete(entity);

    // when
    boolean result = sut.deletePost("p1", "u1");

    // then
    assertTrue(result);
    then(postRepository).should().delete(entity);
  }

  @Test
  void deletePost_deberiaRetornarFalse_cuandoDeleteLanzaExcepcion() {
    // given
    PostEntity entity = PostEntity.builder().id("p1").userId("u1").build();
    given(postRepository.findById("p1")).willReturn(Optional.of(entity));

    willThrow(new RuntimeException("boom")).given(postRepository).delete(entity);

    // when
    boolean result = sut.deletePost("p1", "u1");

    // then
    assertFalse(result);
  }

  // ---------------- toggleLike ----------------

  @Test
  void toggleLike_deberiaRetornarEmpty_cuandoPostNoExiste() {
    // given
    given(postRepository.findById("p1")).willReturn(Optional.empty());

    // when
    Optional<Post> result = sut.toggleLike("p1", "u1");

    // then
    assertTrue(result.isEmpty());
    then(postRepository).should(never()).save(any(PostEntity.class));
  }

  @Test
  void toggleLike_deberiaInicializarLikes_yAgregarLike_cuandoNoEstabaLikeado() {
    // given
    PostEntity entity = PostEntity.builder().id("p1").userId("author").likes(null).build();
    given(postRepository.findById("p1")).willReturn(Optional.of(entity));

    given(postRepository.save(any(PostEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, PostEntity.class));

    // when
    Optional<Post> result = sut.toggleLike("p1", "u1");

    // then
    assertTrue(result.isPresent());
    assertNotNull(entity.getLikes());
    assertTrue(entity.getLikes().contains("u1"));
    then(postRepository).should().save(entity);
  }

  @Test
  void toggleLike_deberiaRemoverLike_cuandoYaEstabaLikeado() {
    // given
    PostEntity entity =
        PostEntity.builder()
            .id("p1")
            .userId("author")
            .likes(new ArrayList<>(List.of("u1", "u2")))
            .build();

    given(postRepository.findById("p1")).willReturn(Optional.of(entity));
    given(postRepository.save(any(PostEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, PostEntity.class));

    // when
    Optional<Post> result = sut.toggleLike("p1", "u1");

    // then
    assertTrue(result.isPresent());
    assertFalse(entity.getLikes().contains("u1"));
    assertTrue(entity.getLikes().contains("u2"));
    then(postRepository).should().save(entity);
  }

  @Test
  void toggleLike_deberiaRetornarEmpty_cuandoSaveFalla() {
    // given
    PostEntity entity =
        PostEntity.builder().id("p1").userId("author").likes(new ArrayList<>()).build();

    given(postRepository.findById("p1")).willReturn(Optional.of(entity));
    given(postRepository.save(any(PostEntity.class))).willThrow(new RuntimeException("boom"));

    // when
    Optional<Post> result = sut.toggleLike("p1", "u1");

    // then
    assertTrue(result.isEmpty());
  }

  // ---------------- isPostLikedByUser ----------------

  @Test
  void isPostLikedByUser_deberiaRetornarFalse_cuandoPostNoExiste() {
    // given
    given(postRepository.findById("p1")).willReturn(Optional.empty());

    // when
    boolean result = sut.isPostLikedByUser("p1", "u1");

    // then
    assertFalse(result);
  }

  @Test
  void isPostLikedByUser_deberiaRetornarFalse_cuandoLikesEsNull() {
    // given
    PostEntity entity = PostEntity.builder().id("p1").likes(null).build();
    given(postRepository.findById("p1")).willReturn(Optional.of(entity));

    // when
    boolean result = sut.isPostLikedByUser("p1", "u1");

    // then
    assertFalse(result);
  }

  @Test
  void isPostLikedByUser_deberiaRetornarTrue_cuandoContieneUserId() {
    // given
    PostEntity entity = PostEntity.builder().id("p1").likes(new ArrayList<>(List.of("u1"))).build();
    given(postRepository.findById("p1")).willReturn(Optional.of(entity));

    // when
    boolean result = sut.isPostLikedByUser("p1", "u1");

    // then
    assertTrue(result);
  }
}
