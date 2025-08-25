package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.model.Comment;
import com.uade.bookybe.core.usecase.CommentService;
import com.uade.bookybe.core.usecase.GamificationService;
import com.uade.bookybe.infraestructure.entity.CommentEntity;
import com.uade.bookybe.infraestructure.mapper.CommentEntityMapper;
import com.uade.bookybe.infraestructure.repository.CommentRepository;
import com.uade.bookybe.infraestructure.repository.PostRepository;
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
public class CommentServiceImpl implements CommentService {

  private final CommentRepository commentRepository;
  private final PostRepository postRepository;
  private final GamificationService gamificationService;

  @Override
  public Optional<Comment> createComment(String userId, String postId, String body) {
    log.info("Creating comment for post: {} by user: {}", postId, userId);

    // Verificar que el post existe
    if (!postRepository.existsById(postId)) {
      log.warn("Post not found with ID: {}", postId);
      throw new NotFoundException("Post not found with ID: " + postId);
    }

    try {
      CommentEntity commentEntity =
          CommentEntity.builder()
              .id(UUID.randomUUID().toString())
              .body(body)
              .userId(userId)
              .postId(postId)
              .dateCreated(LocalDateTime.now())
              .build();

      CommentEntity savedComment = commentRepository.save(commentEntity);
      Comment comment = CommentEntityMapper.INSTANCE.toModel(savedComment);

      log.info("Comment created successfully with ID: {}", savedComment.getId());
      
      // Award gamification points for creating comment
      gamificationService.processCommentCreated(userId);
      
      return Optional.of(comment);

    } catch (Exception e) {
      log.error("Error creating comment for post: {} by user: {}", postId, userId, e);
      return Optional.empty();
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Comment> getCommentById(String commentId) {
    log.info("Getting comment by ID: {}", commentId);

    return commentRepository.findById(commentId).map(CommentEntityMapper.INSTANCE::toModel);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Comment> getCommentsByPostId(String postId) {
    log.info("Getting comments for post: {}", postId);

    return commentRepository.findByPostIdWithUserOrderByDateCreatedDesc(postId).stream()
        .map(CommentEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Comment> getCommentsByUserId(String userId) {
    log.info("Getting comments for user: {}", userId);

    return commentRepository.findByUserIdOrderByDateCreatedDesc(userId).stream()
        .map(CommentEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Comment> updateComment(String commentId, String userId, String body) {
    log.info("Updating comment: {} by user: {}", commentId, userId);

    Optional<CommentEntity> commentEntityOpt = commentRepository.findById(commentId);
    if (commentEntityOpt.isEmpty()) {
      log.warn("Comment not found with ID: {}", commentId);
      return Optional.empty();
    }

    CommentEntity commentEntity = commentEntityOpt.get();

    // Verificar que el usuario es el autor del comentario
    if (!commentEntity.getUserId().equals(userId)) {
      log.warn("User {} tried to update comment {} but is not the author", userId, commentId);
      return Optional.empty();
    }

    commentEntity.setBody(body);
    CommentEntity updatedComment = commentRepository.save(commentEntity);

    log.info("Comment updated successfully: {}", commentId);
    return Optional.of(CommentEntityMapper.INSTANCE.toModel(updatedComment));
  }

  @Override
  public boolean deleteComment(String commentId, String userId) {
    log.info("Deleting comment: {} by user: {}", commentId, userId);

    Optional<CommentEntity> commentEntityOpt = commentRepository.findById(commentId);
    if (commentEntityOpt.isEmpty()) {
      log.warn("Comment not found with ID: {}", commentId);
      return false;
    }

    CommentEntity commentEntity = commentEntityOpt.get();

    // Verificar que el usuario es el autor del comentario
    if (!commentEntity.getUserId().equals(userId)) {
      log.warn("User {} tried to delete comment {} but is not the author", userId, commentId);
      return false;
    }

    try {
      commentRepository.delete(commentEntity);
      log.info("Comment deleted successfully: {}", commentId);
      return true;
    } catch (Exception e) {
      log.error("Error deleting comment: {}", commentId, e);
      return false;
    }
  }
}
