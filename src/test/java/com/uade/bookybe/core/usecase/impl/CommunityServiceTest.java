package com.uade.bookybe.core.usecase.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.exception.UnauthorizedException;
import com.uade.bookybe.core.model.Community;
import com.uade.bookybe.core.usecase.GamificationService;
import com.uade.bookybe.infraestructure.entity.CommentEntity;
import com.uade.bookybe.infraestructure.entity.CommunityEntity;
import com.uade.bookybe.infraestructure.entity.CommunityMemberEntity;
import com.uade.bookybe.infraestructure.entity.CommunityMemberId;
import com.uade.bookybe.infraestructure.entity.PostEntity;
import com.uade.bookybe.infraestructure.repository.CommentRepository;
import com.uade.bookybe.infraestructure.repository.CommunityMemberRepository;
import com.uade.bookybe.infraestructure.repository.CommunityRepository;
import com.uade.bookybe.infraestructure.repository.PostRepository;
import com.uade.bookybe.infraestructure.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CommunityServiceImplTest {

    @Mock private CommunityRepository communityRepository;
    @Mock private CommunityMemberRepository communityMemberRepository;
    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private UserRepository userRepository;
    @Mock private GamificationService gamificationService;

    @InjectMocks private CommunityServiceImpl sut;

    // Helpers para SecurityContextHolder (joinAvailable)
    private void mockAuthUser(String currentUserId) {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn(currentUserId);

        SecurityContext ctx = mock(SecurityContext.class);
        given(ctx.getAuthentication()).willReturn(auth);

        SecurityContextHolder.setContext(ctx);
    }

    // IMPORTANTE: limpiamos el SecurityContext para no contaminar otros tests
    private void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    // ---------------- createCommunity ----------------

    @Test
    void createCommunity_deberiaRetornarEmpty_cuandoAdminNoExiste() {
        // given
        given(userRepository.existsById("admin1")).willReturn(false);

        // when
        Optional<Community> result = sut.createCommunity("admin1", "Nombre", "Desc");

        // then
        assertTrue(result.isEmpty());
        then(communityRepository).should(never()).save(any());
        then(gamificationService).shouldHaveNoInteractions();
    }

    @Test
    void createCommunity_deberiaRetornarEmpty_cuandoNombreYaExiste() {
        // given
        given(userRepository.existsById("admin1")).willReturn(true);
        given(communityRepository.existsByName("Nombre")).willReturn(true);

        // when
        Optional<Community> result = sut.createCommunity("admin1", "Nombre", "Desc");

        // then
        assertTrue(result.isEmpty());
        then(communityRepository).should(never()).save(any());
        then(gamificationService).shouldHaveNoInteractions();
    }

    @Test
    void createCommunity_deberiaCrearCommunity_joinAdminComoMiembro_yOtorgarGamification() {
        // given
        String adminId = "admin1";
        String name = "Nombre";
        String description = "Desc";
        String communityId = "c1";

        mockAuthUser(adminId); // para enrichWithJoinAvailable

        given(userRepository.existsById(adminId)).willReturn(true);
        given(communityRepository.existsByName(name)).willReturn(false);

        // save devuelve entity con id (el service genera UUID, pero en test no dependemos)
        CommunityEntity savedEntity = CommunityEntity.builder()
                .id(communityId)
                .adminId(adminId)
                .name(name)
                .description(description)
                .build();

        given(communityRepository.save(any(CommunityEntity.class))).willReturn(savedEntity);

        // joinCommunity llamado internamente:
        given(communityRepository.existsById(communityId)).willReturn(true);
        given(userRepository.existsById(adminId)).willReturn(true);
        given(communityMemberRepository.existsById(any(CommunityMemberId.class))).willReturn(false);
        given(communityMemberRepository.save(any(CommunityMemberEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, CommunityMemberEntity.class));

        // joinAvailable = !isUserMember => isUserMember usa existsById(memberId)
        // Como arriba devolvemos false, joinAvailable debería quedar true (admin no es miembro aún en check final)
        // memberCount
        given(communityMemberRepository.countByCommunityId(communityId)).willReturn(3L);

        // when
        Optional<Community> result = sut.createCommunity(adminId, name, description);

        // then
        assertTrue(result.isPresent());
        assertEquals(communityId, result.get().getId());
        assertEquals(name, result.get().getName());
        assertEquals(description, result.get().getDescription());
        assertEquals(adminId, result.get().getAdminId());
        assertEquals(3L, result.get().getMemberCount());
        assertTrue(result.get().isJoinAvailable());

        then(gamificationService).should().processCommunityCreated(adminId);

        clearSecurity();
    }


    // ---------------- getCommunityById (incluye enrich) ----------------

    @Test
    void getCommunityById_deberiaRetornarEmpty_cuandoNoExiste() {
        // given
        given(communityRepository.findById("c1")).willReturn(Optional.empty());

        // when
        Optional<Community> result = sut.getCommunityById("c1");

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    void getCommunityById_deberiaEnriquecerJoinAvailable_yMemberCount() {
        // given
        String communityId = "c1";
        String currentUser = "u-current";

        mockAuthUser(currentUser);

        given(communityRepository.findById(communityId))
                .willReturn(Optional.of(CommunityEntity.builder().id(communityId).name("N").adminId("admin").build()));

        // joinAvailable depende de isUserMember -> existsById(memberId)
        given(communityMemberRepository.existsById(new CommunityMemberId(communityId, currentUser)))
                .willReturn(false);

        given(communityMemberRepository.countByCommunityId(communityId)).willReturn(10L);

        // when
        Optional<Community> result = sut.getCommunityById(communityId);

        // then
        assertTrue(result.isPresent());
        assertEquals(communityId, result.get().getId());
        assertTrue(result.get().isJoinAvailable());
        assertEquals(10L, result.get().getMemberCount());

        clearSecurity();
    }

    // ---------------- getCommunitiesByAdminId / getAll / search ----------------

    @Test
    void getCommunitiesByAdminId_deberiaMapearYEnriquecerJoinAvailable() {
        // given
        mockAuthUser("u-current");

        given(communityRepository.findByAdminIdOrderByDateCreatedDesc("admin1"))
                .willReturn(List.of(
                        CommunityEntity.builder().id("c1").adminId("admin1").name("A").build(),
                        CommunityEntity.builder().id("c2").adminId("admin1").name("B").build()
                ));

        given(communityMemberRepository.existsById(new CommunityMemberId("c1", "u-current"))).willReturn(false);
        given(communityMemberRepository.existsById(new CommunityMemberId("c2", "u-current"))).willReturn(true);

        // when
        List<Community> result = sut.getCommunitiesByAdminId("admin1");

        // then
        assertEquals(2, result.size());
        assertTrue(result.get(0).isJoinAvailable());  // no es miembro
        assertFalse(result.get(1).isJoinAvailable()); // sí es miembro

        clearSecurity();
    }

    @Test
    void getAllCommunities_deberiaMapearYEnriquecerMemberCount_yJoinAvailable() {
        // given
        mockAuthUser("u-current");

        given(communityRepository.findAllWithAdminOrderByDateCreatedDesc())
                .willReturn(List.of(
                        CommunityEntity.builder().id("c1").name("A").adminId("a").build(),
                        CommunityEntity.builder().id("c2").name("B").adminId("b").build()
                ));

        given(communityMemberRepository.countByCommunityId("c1")).willReturn(2L);
        given(communityMemberRepository.countByCommunityId("c2")).willReturn(5L);

        given(communityMemberRepository.existsById(new CommunityMemberId("c1", "u-current"))).willReturn(false);
        given(communityMemberRepository.existsById(new CommunityMemberId("c2", "u-current"))).willReturn(false);

        // when
        List<Community> result = sut.getAllCommunities();

        // then
        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getMemberCount());
        assertEquals(5L, result.get(1).getMemberCount());
        assertTrue(result.get(0).isJoinAvailable());
        assertTrue(result.get(1).isJoinAvailable());

        clearSecurity();
    }

    @Test
    void searchCommunities_deberiaMapearYEnriquecerMemberCount() {
        // given
        given(communityRepository.searchCommunities("q"))
                .willReturn(List.of(
                        CommunityEntity.builder().id("c1").name("A").adminId("a").build()
                ));
        given(communityMemberRepository.countByCommunityId("c1")).willReturn(7L);

        // when
        List<Community> result = sut.searchCommunities("q");

        // then
        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getMemberCount());
    }

    // ---------------- updateCommunity ----------------

    @Test
    void updateCommunity_deberiaRetornarEmpty_cuandoNoExiste() {
        // given
        given(communityRepository.findById("c1")).willReturn(Optional.empty());

        // when
        Optional<Community> result = sut.updateCommunity("c1", "admin1", "N", "D");

        // then
        assertTrue(result.isEmpty());
        then(communityRepository).should(never()).save(any());
    }

    @Test
    void updateCommunity_deberiaRetornarEmpty_cuandoNoEsAdmin() {
        // given
        given(communityRepository.findById("c1"))
                .willReturn(Optional.of(CommunityEntity.builder().id("c1").adminId("adminX").name("Old").build()));

        // when
        Optional<Community> result = sut.updateCommunity("c1", "admin1", "N", "D");

        // then
        assertTrue(result.isEmpty());
        then(communityRepository).should(never()).save(any());
    }

    @Test
    void updateCommunity_deberiaRetornarEmpty_cuandoExisteOtroConMismoNombre() {
        // given
        CommunityEntity entity = CommunityEntity.builder()
                .id("c1")
                .adminId("admin1")
                .name("Old")
                .build();

        given(communityRepository.findById("c1")).willReturn(Optional.of(entity));
        given(communityRepository.existsByName("NewName")).willReturn(true);

        // when
        Optional<Community> result = sut.updateCommunity("c1", "admin1", "NewName", "D");

        // then
        assertTrue(result.isEmpty());
        then(communityRepository).should(never()).save(any());
    }

    @Test
    void updateCommunity_deberiaActualizar_yGuardar_cuandoValido() {
        // given
        CommunityEntity entity = CommunityEntity.builder()
                .id("c1")
                .adminId("admin1")
                .name("Old")
                .description("OldD")
                .build();

        given(communityRepository.findById("c1")).willReturn(Optional.of(entity));
        given(communityRepository.existsByName("New")).willReturn(false);
        given(communityRepository.save(any(CommunityEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, CommunityEntity.class));

        // when
        Optional<Community> result = sut.updateCommunity("c1", "admin1", "New", "NewD");

        // then
        assertTrue(result.isPresent());
        assertEquals("New", result.get().getName());
        assertEquals("NewD", result.get().getDescription());
        then(communityRepository).should().save(argThat(e -> "New".equals(e.getName()) && "NewD".equals(e.getDescription())));
    }

    // ---------------- deleteCommunity ----------------

    @Test
    void deleteCommunity_deberiaRetornarFalse_cuandoNoExiste() {
        // given
        given(communityRepository.findById("c1")).willReturn(Optional.empty());

        // when
        boolean result = sut.deleteCommunity("c1", "admin1");

        // then
        assertFalse(result);
        then(communityRepository).should(never()).delete(any());
    }

    @Test
    void deleteCommunity_deberiaLanzarUnauthorized_cuandoNoEsAdmin() {
        // given
        given(communityRepository.findById("c1"))
                .willReturn(Optional.of(CommunityEntity.builder().id("c1").adminId("adminX").build()));

        // when + then
        assertThrows(UnauthorizedException.class, () -> sut.deleteCommunity("c1", "admin1"));
        then(communityRepository).should(never()).delete(any());
    }

    @Test
    void deleteCommunity_deberiaHacerCascadeDelete_yRetornarTrue_cuandoValido() {
        // given
        String communityId = "c1";
        String adminId = "admin1";

        CommunityEntity communityEntity = CommunityEntity.builder().id(communityId).adminId(adminId).build();
        given(communityRepository.findById(communityId)).willReturn(Optional.of(communityEntity));

        PostEntity p1 = PostEntity.builder().id("p1").communityId(communityId).build();
        PostEntity p2 = PostEntity.builder().id("p2").communityId(communityId).build();
        given(postRepository.findByCommunityIdOrderByDateCreatedDesc(communityId)).willReturn(List.of(p1, p2));

        CommentEntity c1 = CommentEntity.builder().id("cmt1").postId("p1").build();
        CommentEntity c2 = CommentEntity.builder().id("cmt2").postId("p2").build();
        given(commentRepository.findByPostIdOrderByDateCreatedDesc("p1")).willReturn(List.of(c1));
        given(commentRepository.findByPostIdOrderByDateCreatedDesc("p2")).willReturn(List.of(c2));

        CommunityMemberEntity m1 = CommunityMemberEntity.builder().communityId(communityId).userId("u1").build();
        CommunityMemberEntity m2 = CommunityMemberEntity.builder().communityId(communityId).userId("u2").build();
        given(communityMemberRepository.findByCommunityIdWithUser(communityId)).willReturn(List.of(m1, m2));

        willDoNothing().given(commentRepository).deleteAll(anyList());
        willDoNothing().given(postRepository).deleteAll(anyList());
        willDoNothing().given(communityMemberRepository).deleteAll(anyList());
        willDoNothing().given(communityRepository).delete(communityEntity);

        // when
        boolean result = sut.deleteCommunity(communityId, adminId);

        // then
        assertTrue(result);

        then(commentRepository).should().deleteAll(List.of(c1));
        then(commentRepository).should().deleteAll(List.of(c2));
        then(postRepository).should().deleteAll(List.of(p1, p2));
        then(communityMemberRepository).should().deleteAll(List.of(m1, m2));
        then(communityRepository).should().delete(communityEntity);
    }

    @Test
    void deleteCommunity_deberiaRetornarFalse_cuandoFallaAlgunaEliminacion() {
        // given
        String communityId = "c1";
        String adminId = "admin1";

        CommunityEntity communityEntity = CommunityEntity.builder().id(communityId).adminId(adminId).build();
        given(communityRepository.findById(communityId)).willReturn(Optional.of(communityEntity));

        given(postRepository.findByCommunityIdOrderByDateCreatedDesc(communityId)).willReturn(List.of());
        given(communityMemberRepository.findByCommunityIdWithUser(communityId)).willReturn(List.of());

        willThrow(new RuntimeException("boom")).given(communityRepository).delete(communityEntity);

        // when
        boolean result = sut.deleteCommunity(communityId, adminId);

        // then
        assertFalse(result);
    }

    // ---------------- joinCommunity ----------------

    @Test
    void joinCommunity_deberiaLanzarNotFound_cuandoCommunityNoExiste() {
        // given
        given(communityRepository.existsById("c1")).willReturn(false);

        // when + then
        assertThrows(NotFoundException.class, () -> sut.joinCommunity("c1", "u1"));

        then(userRepository).shouldHaveNoInteractions();
        then(communityMemberRepository).shouldHaveNoInteractions();
    }

    @Test
    void joinCommunity_deberiaLanzarNotFound_cuandoUserNoExiste() {
        // given
        given(communityRepository.existsById("c1")).willReturn(true);
        given(userRepository.existsById("u1")).willReturn(false);

        // when + then
        assertThrows(NotFoundException.class, () -> sut.joinCommunity("c1", "u1"));

        then(communityMemberRepository).shouldHaveNoInteractions();
    }

    @Test
    void joinCommunity_deberiaRetornarFalse_cuandoYaEsMiembro() {
        // given
        given(communityRepository.existsById("c1")).willReturn(true);
        given(userRepository.existsById("u1")).willReturn(true);
        given(communityMemberRepository.existsById(new CommunityMemberId("c1", "u1"))).willReturn(true);

        // when
        boolean result = sut.joinCommunity("c1", "u1");

        // then
        assertFalse(result);
        then(communityMemberRepository).should(never()).save(any());
        then(gamificationService).shouldHaveNoInteractions();
    }

    @Test
    void joinCommunity_deberiaGuardarMiembro_yOtorgarPuntos_cuandoUserNoEsAdmin() {
        // given
        String communityId = "c1";
        String userId = "u1";

        given(communityRepository.existsById(communityId)).willReturn(true);
        given(userRepository.existsById(userId)).willReturn(true);
        given(communityMemberRepository.existsById(new CommunityMemberId(communityId, userId))).willReturn(false);

        given(communityMemberRepository.save(any(CommunityMemberEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, CommunityMemberEntity.class));

        // para otorgar gamification: findById y comparar adminId != userId
        given(communityRepository.findById(communityId))
                .willReturn(Optional.of(CommunityEntity.builder().id(communityId).adminId("adminX").build()));

        // when
        boolean result = sut.joinCommunity(communityId, userId);

        // then
        assertTrue(result);
        then(communityMemberRepository).should().save(argThat(m -> communityId.equals(m.getCommunityId()) && userId.equals(m.getUserId())));
        then(gamificationService).should().processCommunityJoined(userId);
    }

    @Test
    void joinCommunity_noDeberiaOtorgarPuntos_cuandoUserEsAdminDeLaComunidad() {
        // given
        String communityId = "c1";
        String adminId = "admin1";

        given(communityRepository.existsById(communityId)).willReturn(true);
        given(userRepository.existsById(adminId)).willReturn(true);
        given(communityMemberRepository.existsById(new CommunityMemberId(communityId, adminId))).willReturn(false);
        given(communityMemberRepository.save(any(CommunityMemberEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, CommunityMemberEntity.class));

        given(communityRepository.findById(communityId))
                .willReturn(Optional.of(CommunityEntity.builder().id(communityId).adminId(adminId).build()));

        // when
        boolean result = sut.joinCommunity(communityId, adminId);

        // then
        assertTrue(result);
        then(gamificationService).shouldHaveNoInteractions();
    }

    @Test
    void joinCommunity_deberiaRetornarTrue_aunSiFallaGamificationLookup() {
        // given
        String communityId = "c1";
        String userId = "u1";

        given(communityRepository.existsById(communityId)).willReturn(true);
        given(userRepository.existsById(userId)).willReturn(true);
        given(communityMemberRepository.existsById(new CommunityMemberId(communityId, userId))).willReturn(false);
        given(communityMemberRepository.save(any(CommunityMemberEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, CommunityMemberEntity.class));

        // el try/catch interno atrapa excepciones de lookup de communityRepository.findById
        given(communityRepository.findById(communityId)).willThrow(new RuntimeException("boom"));

        // when
        boolean result = sut.joinCommunity(communityId, userId);

        // then
        assertTrue(result);
        then(gamificationService).shouldHaveNoInteractions();
    }

    @Test
    void joinCommunity_deberiaRetornarFalse_cuandoFallaElSave() {
        // given
        String communityId = "c1";
        String userId = "u1";

        given(communityRepository.existsById(communityId)).willReturn(true);
        given(userRepository.existsById(userId)).willReturn(true);
        given(communityMemberRepository.existsById(new CommunityMemberId(communityId, userId))).willReturn(false);

        given(communityMemberRepository.save(any(CommunityMemberEntity.class)))
                .willThrow(new RuntimeException("boom"));

        // when
        boolean result = sut.joinCommunity(communityId, userId);

        // then
        assertFalse(result);
    }

    // ---------------- leaveCommunity ----------------

    @Test
    void leaveCommunity_deberiaRetornarFalse_cuandoNoEsMiembro() {
        // given
        CommunityMemberId memberId = new CommunityMemberId("c1", "u1");
        given(communityMemberRepository.existsById(memberId)).willReturn(false);

        // when
        boolean result = sut.leaveCommunity("c1", "u1");

        // then
        assertFalse(result);
        then(communityMemberRepository).should(never()).deleteById(any());
    }

    @Test
    void leaveCommunity_deberiaBorrarYRetornarTrue_cuandoEsMiembro() {
        // given
        CommunityMemberId memberId = new CommunityMemberId("c1", "u1");
        given(communityMemberRepository.existsById(memberId)).willReturn(true);
        willDoNothing().given(communityMemberRepository).deleteById(memberId);

        // when
        boolean result = sut.leaveCommunity("c1", "u1");

        // then
        assertTrue(result);
        then(communityMemberRepository).should().deleteById(memberId);
    }

    @Test
    void leaveCommunity_deberiaRetornarFalse_cuandoDeleteFalla() {
        // given
        CommunityMemberId memberId = new CommunityMemberId("c1", "u1");
        given(communityMemberRepository.existsById(memberId)).willReturn(true);
        willThrow(new RuntimeException("boom")).given(communityMemberRepository).deleteById(memberId);

        // when
        boolean result = sut.leaveCommunity("c1", "u1");

        // then
        assertFalse(result);
    }

    // ---------------- getUserCommunities / isUserMember ----------------

    @Test
    void getUserCommunities_deberiaMapearDesdeMemberEntity_yEnriquecerMemberCount() {
        // given
        CommunityEntity c1 = CommunityEntity.builder().id("c1").name("A").adminId("a").build();
        CommunityEntity c2 = CommunityEntity.builder().id("c2").name("B").adminId("b").build();

        CommunityMemberEntity m1 = CommunityMemberEntity.builder().communityId("c1").userId("u1").build();
        CommunityMemberEntity m2 = CommunityMemberEntity.builder().communityId("c2").userId("u1").build();
        // asumimos que el entity tiene getCommunity() (como en tu código)
        m1.setCommunity(c1);
        m2.setCommunity(c2);

        given(communityMemberRepository.findByUserIdWithCommunity("u1")).willReturn(List.of(m1, m2));
        given(communityMemberRepository.countByCommunityId("c1")).willReturn(2L);
        given(communityMemberRepository.countByCommunityId("c2")).willReturn(5L);

        // when
        List<Community> result = sut.getUserCommunities("u1");

        // then
        assertEquals(2, result.size());
        assertEquals("c1", result.get(0).getId());
        assertEquals(2L, result.get(0).getMemberCount());
        assertEquals("c2", result.get(1).getId());
        assertEquals(5L, result.get(1).getMemberCount());
    }

    @Test
    void isUserMember_deberiaDelegarEnRepo() {
        // given
        CommunityMemberId memberId = new CommunityMemberId("c1", "u1");
        given(communityMemberRepository.existsById(memberId)).willReturn(true);

        // when
        boolean result = sut.isUserMember("c1", "u1");

        // then
        assertTrue(result);
        then(communityMemberRepository).should().existsById(memberId);
    }

    // ---------------- enrichWithMemberCount (error path) ----------------
    // Se prueba indirectamente: si countByCommunityId lanza excepción, memberCount = 0

    @Test
    void getAllCommunities_siFallaCount_deberiaSetearMemberCountCero() {
        // given
        mockAuthUser("u-current");

        given(communityRepository.findAllWithAdminOrderByDateCreatedDesc())
                .willReturn(List.of(CommunityEntity.builder().id("c1").name("A").adminId("a").build()));

        given(communityMemberRepository.countByCommunityId("c1")).willThrow(new RuntimeException("boom"));
        given(communityMemberRepository.existsById(new CommunityMemberId("c1", "u-current"))).willReturn(false);

        // when
        List<Community> result = sut.getAllCommunities();

        // then
        assertEquals(1, result.size());
        assertEquals(0L, result.get(0).getMemberCount());

        clearSecurity();
    }
}
