package com.uade.bookybe.core.usecase.impl;

import ch.qos.logback.core.util.StringUtil;
import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.model.Post;
import com.uade.bookybe.core.port.ImageStoragePort;
import com.uade.bookybe.core.usecase.PostService;
import com.uade.bookybe.infraestructure.entity.PostEntity;
import com.uade.bookybe.infraestructure.mapper.PostEntityMapper;
import com.uade.bookybe.infraestructure.repository.CommunityRepository;
import com.uade.bookybe.infraestructure.repository.PostRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PostServiceImpl implements PostService {

  private final PostRepository postRepository;
  private final ImageStoragePort imageStoragePort;
  private final CommunityRepository communityRepository;

  @Override
  public Optional<Post> createPost(
      String userId, String body, String communityId, MultipartFile image) {
    log.info("Creating post for user: {}, community: {}", userId, communityId);

    // Validate community if communityId is provided
    if (!StringUtil.isNullOrEmpty(communityId)) {
      boolean communityExists = communityRepository.existsById(communityId);
      if (!communityExists) {
        log.warn("Community not found with ID: {}", communityId);
        throw new NotFoundException("Community not found with ID: " + communityId);
      }
    }

    try {
      PostEntity postEntity =
          PostEntity.builder()
              .id(UUID.randomUUID().toString())
              .userId(userId)
              .body(body)
              .communityId(communityId)
              .dateCreated(LocalDateTime.now())
              .build();

      // Handle image upload if provided
      if (image != null && !image.isEmpty()) {
        Optional<String> imageUrlOpt = imageStoragePort.uploadImage(image, "booky/posts");
        if (imageUrlOpt.isPresent()) {
          postEntity.setImage(imageUrlOpt.get());
        }
      }

      PostEntity savedPost = postRepository.save(postEntity);
      Post post = PostEntityMapper.INSTANCE.toModel(savedPost);

      log.info("Post created successfully with ID: {}", savedPost.getId());
      return Optional.of(post);
    } catch (Exception e) {
      log.error("Error creating post for user: {}", userId, e);
      return Optional.empty();
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Post> getPostById(String postId) {
    log.info("Getting post by ID: {}", postId);

    return postRepository.findById(postId).map(PostEntityMapper.INSTANCE::toModel);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Post> getPostsByUserId(String userId) {
    log.info("Getting posts for user: {}", userId);

    return postRepository.findByUserIdOrderByDateCreatedDesc(userId).stream()
        .map(PostEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Post> getPostsByCommunityId(String communityId) {
    log.info("Getting posts for community: {}", communityId);

    return postRepository.findByCommunityIdOrderByDateCreatedDesc(communityId).stream()
        .map(PostEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Post> getGeneralPosts() {
    log.info("Getting general posts (not in communities)");

    return postRepository.findGeneralPostsOrderByDateCreatedDesc().stream()
        .map(PostEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Post> getAllPosts() {
    log.info("Getting all posts");

    return postRepository.findAllWithUserOrderByDateCreatedDesc().stream()
        .map(PostEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Post> getUserFeed(String userId) {
    log.info("Getting user feed for user: {}", userId);

    // Obtener posts de usuarios seguidos
    return postRepository.findPostsFromFollowedUsers(userId).stream()
        .map(PostEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Post> updatePost(String postId, String userId, String body) {
    log.info("Updating post: {} by user: {}", postId, userId);

    Optional<PostEntity> postEntityOpt = postRepository.findById(postId);
    if (postEntityOpt.isEmpty()) {
      log.warn("Post not found with ID: {}", postId);
      return Optional.empty();
    }

    PostEntity postEntity = postEntityOpt.get();

    // Verificar que el usuario es el autor del post
    if (!postEntity.getUserId().equals(userId)) {
      log.warn("User {} tried to update post {} but is not the author", userId, postId);
      return Optional.empty();
    }

    postEntity.setBody(body);
    PostEntity updatedPost = postRepository.save(postEntity);

    log.info("Post updated successfully: {}", postId);
    return Optional.of(PostEntityMapper.INSTANCE.toModel(updatedPost));
  }

  @Override
  public boolean deletePost(String postId, String userId) {
    log.info("Deleting post: {} by user: {}", postId, userId);

    Optional<PostEntity> postEntityOpt = postRepository.findById(postId);
    if (postEntityOpt.isEmpty()) {
      log.warn("Post not found with ID: {}", postId);
      return false;
    }

    PostEntity postEntity = postEntityOpt.get();

    // Verificar que el usuario es el autor del post
    if (!postEntity.getUserId().equals(userId)) {
      log.warn("User {} tried to delete post {} but is not the author", userId, postId);
      return false;
    }

    try {
      postRepository.delete(postEntity);
      log.info("Post deleted successfully: {}", postId);
      return true;
    } catch (Exception e) {
      log.error("Error deleting post: {}", postId, e);
      return false;
    }
  }
}
