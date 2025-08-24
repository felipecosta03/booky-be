package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.exception.UnauthorizedException;
import com.uade.bookybe.core.model.Community;
import com.uade.bookybe.core.usecase.CommunityService;
import com.uade.bookybe.infraestructure.entity.CommunityEntity;
import com.uade.bookybe.infraestructure.entity.CommunityMemberEntity;
import com.uade.bookybe.infraestructure.entity.CommunityMemberId;
import com.uade.bookybe.infraestructure.mapper.CommunityEntityMapper;
import com.uade.bookybe.infraestructure.repository.CommentRepository;
import com.uade.bookybe.infraestructure.repository.CommunityMemberRepository;
import com.uade.bookybe.infraestructure.repository.CommunityRepository;
import com.uade.bookybe.infraestructure.repository.PostRepository;
import com.uade.bookybe.infraestructure.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CommunityServiceImpl implements CommunityService {

  private final CommunityRepository communityRepository;
  private final CommunityMemberRepository communityMemberRepository;
  private final PostRepository postRepository;
  private final CommentRepository commentRepository;
  private final UserRepository userRepository;

  @Override
  public Optional<Community> createCommunity(String adminId, String name, String description) {
    log.info("Creating community: {} by admin: {}", name, adminId);

    // Verificar que el administrador existe
    if (!userRepository.existsById(adminId)) {
      log.warn("Admin user not found with ID: {}", adminId);
      return Optional.empty();
    }

    // Verificar que no existe una comunidad con el mismo nombre
    if (communityRepository.existsByName(name)) {
      log.warn("Community already exists with name: {}", name);
      return Optional.empty();
    }

    try {
      CommunityEntity communityEntity =
          CommunityEntity.builder()
              .id(UUID.randomUUID().toString())
              .name(name)
              .description(description)
              .adminId(adminId)
              .dateCreated(LocalDateTime.now())
              .build();

      CommunityEntity savedCommunity = communityRepository.save(communityEntity);

      // Agregar al administrador como miembro automáticamente
      joinCommunity(savedCommunity.getId(), adminId);

      Community community = CommunityEntityMapper.INSTANCE.toModel(savedCommunity);
      // Enriquecer con memberCount
      community = enrichWithMemberCount(community);

      log.info("Community created successfully with ID: {}", savedCommunity.getId());
      return Optional.of(community);

    } catch (Exception e) {
      log.error("Error creating community: {} by admin: {}", name, adminId, e);
      return Optional.empty();
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Community> getCommunityById(String communityId) {
    log.info("Getting community by ID: {}", communityId);

    return communityRepository
        .findById(communityId)
        .map(CommunityEntityMapper.INSTANCE::toModel)
        .map(this::enrichWithMemberCount);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Community> getCommunitiesByAdminId(String adminId) {
    log.info("Getting communities administered by user: {}", adminId);

    return communityRepository.findByAdminIdOrderByDateCreatedDesc(adminId).stream()
        .map(CommunityEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Community> getAllCommunities() {
    log.info("Getting all communities");

    return communityRepository.findAllWithAdminOrderByDateCreatedDesc().stream()
        .map(CommunityEntityMapper.INSTANCE::toModel)
        .map(this::enrichWithMemberCount)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Community> searchCommunities(String query) {
    log.info("Searching communities with query: {}", query);

    return communityRepository.searchCommunities(query).stream()
        .map(CommunityEntityMapper.INSTANCE::toModel)
        .map(this::enrichWithMemberCount)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Community> updateCommunity(
      String communityId, String adminId, String name, String description) {
    log.info("Updating community: {} by admin: {}", communityId, adminId);

    Optional<CommunityEntity> communityEntityOpt = communityRepository.findById(communityId);
    if (communityEntityOpt.isEmpty()) {
      log.warn("Community not found with ID: {}", communityId);
      return Optional.empty();
    }

    CommunityEntity communityEntity = communityEntityOpt.get();

    // Verificar que el usuario es el administrador de la comunidad
    if (!communityEntity.getAdminId().equals(adminId)) {
      log.warn("User {} tried to update community {} but is not the admin", adminId, communityId);
      return Optional.empty();
    }

    // Verificar que no existe otra comunidad con el mismo nombre (excepto la actual)
    if (communityRepository.existsByName(name) && !communityEntity.getName().equals(name)) {
      log.warn("Another community already exists with name: {}", name);
      return Optional.empty();
    }

    communityEntity.setName(name);
    communityEntity.setDescription(description);
    CommunityEntity updatedCommunity = communityRepository.save(communityEntity);

    log.info("Community updated successfully: {}", communityId);
    return Optional.of(CommunityEntityMapper.INSTANCE.toModel(updatedCommunity));
  }

  @Override
  public boolean deleteCommunity(String communityId, String adminId) {
    log.info("Deleting community: {} by admin: {}", communityId, adminId);

    Optional<CommunityEntity> communityEntityOpt = communityRepository.findById(communityId);
    if (communityEntityOpt.isEmpty()) {
      log.warn("Community not found with ID: {}", communityId);
      return false;
    }

    CommunityEntity communityEntity = communityEntityOpt.get();

    // Verificar que el usuario es el administrador de la comunidad
    if (!communityEntity.getAdminId().equals(adminId)) {
      throw new UnauthorizedException("User is not the admin of the community");
    }

    try {
      // Orden de eliminación para mantener integridad referencial:
      // 1. Comentarios en posts de la comunidad
      // 2. Posts de la comunidad
      // 3. Miembros de la comunidad
      // 4. La comunidad misma

      log.info("Starting cascade deletion for community: {}", communityId);

      // 1. Eliminar todos los comentarios de posts de esta comunidad
      var postsInCommunity = postRepository.findByCommunityIdOrderByDateCreatedDesc(communityId);
      int totalComments = 0;
      for (var post : postsInCommunity) {
        var comments = commentRepository.findByPostIdOrderByDateCreatedDesc(post.getId());
        totalComments += comments.size();
        commentRepository.deleteAll(comments);
      }
      log.info("Deleted {} comments from community posts", totalComments);

      // 2. Eliminar todos los posts de la comunidad
      int totalPosts = postsInCommunity.size();
      postRepository.deleteAll(postsInCommunity);
      log.info("Deleted {} posts from community", totalPosts);

      // 3. Eliminar todos los miembros de la comunidad
      var members = communityMemberRepository.findByCommunityIdWithUser(communityId);
      int totalMembers = members.size();
      communityMemberRepository.deleteAll(members);
      log.info("Deleted {} members from community", totalMembers);

      // 4. Finalmente, eliminar la comunidad
      communityRepository.delete(communityEntity);

      log.info(
          "Community deleted successfully: {} (with {} posts, {} comments, {} members)",
          communityId,
          totalPosts,
          totalComments,
          totalMembers);
      return true;
    } catch (Exception e) {
      log.error("Error deleting community: {}", communityId, e);
      return false;
    }
  }

  @Override
  public boolean joinCommunity(String communityId, String userId) {
    log.info("User {} joining community: {}", userId, communityId);

    // Verificar que la comunidad existe
    if (!communityRepository.existsById(communityId)) {
      throw new NotFoundException("Community not found with ID: " + communityId);
    }

    // Verificar que el usuario existe
    if (!userRepository.existsById(userId)) {
      throw new NotFoundException("User not found with ID: " + userId);
    }

    // Verificar que el usuario no es ya miembro
    CommunityMemberId memberId = new CommunityMemberId(communityId, userId);
    if (communityMemberRepository.existsById(memberId)) {
      log.warn("User {} is already a member of community {}", userId, communityId);
      return false;
    }

    try {
      CommunityMemberEntity memberEntity =
          CommunityMemberEntity.builder().communityId(communityId).userId(userId).build();

      communityMemberRepository.save(memberEntity);
      log.info("User {} successfully joined community: {}", userId, communityId);
      return true;
    } catch (Exception e) {
      log.error("Error joining community: {} by user: {}", communityId, userId, e);
      return false;
    }
  }

  @Override
  public boolean leaveCommunity(String communityId, String userId) {
    log.info("User {} leaving community: {}", userId, communityId);

    CommunityMemberId memberId = new CommunityMemberId(communityId, userId);

    if (!communityMemberRepository.existsById(memberId)) {
      log.warn("User {} is not a member of community {}", userId, communityId);
      return false;
    }

    try {
      communityMemberRepository.deleteById(memberId);
      log.info("User {} successfully left community: {}", userId, communityId);
      return true;
    } catch (Exception e) {
      log.error("Error leaving community: {} by user: {}", communityId, userId, e);
      return false;
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<Community> getUserCommunities(String userId) {
    log.info("Getting communities for user: {}", userId);

    return communityMemberRepository.findByUserIdWithCommunity(userId).stream()
        .map(memberEntity -> CommunityEntityMapper.INSTANCE.toModel(memberEntity.getCommunity()))
        .map(this::enrichWithMemberCount)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isUserMember(String communityId, String userId) {
    CommunityMemberId memberId = new CommunityMemberId(communityId, userId);
    return communityMemberRepository.existsById(memberId);
  }

  /**
   * Enriquece el modelo Community con el memberCount calculado dinámicamente. Este método realiza
   * una consulta adicional para obtener el número de miembros.
   *
   * @param community El modelo Community a enriquecer
   * @return Community con memberCount actualizado
   */
  private Community enrichWithMemberCount(Community community) {
    if (community == null || community.getId() == null) {
      return community;
    }

    try {
      long memberCount = communityMemberRepository.countByCommunityId(community.getId());
      community.setMemberCount(memberCount);
      log.debug("Community {} enriched with member count: {}", community.getId(), memberCount);
    } catch (Exception e) {
      log.warn(
          "Error calculating member count for community {}: {}", community.getId(), e.getMessage());
      community.setMemberCount(0);
    }

    return community;
  }
}
