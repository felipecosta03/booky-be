package com.uade.bookybe.core.usecase;

import com.uade.bookybe.core.model.Comment;
import java.util.List;
import java.util.Optional;

public interface CommentService {
  
  Optional<Comment> createComment(String userId, String postId, String body);
  
  Optional<Comment> getCommentById(String commentId);
  
  List<Comment> getCommentsByPostId(String postId);
  
  List<Comment> getCommentsByUserId(String userId);
  
  Optional<Comment> updateComment(String commentId, String userId, String body);
  
  boolean deleteComment(String commentId, String userId);
} 