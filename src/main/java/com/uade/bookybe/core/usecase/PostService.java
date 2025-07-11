package com.uade.bookybe.core.usecase;

import com.uade.bookybe.core.model.Post;
import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public interface PostService {
  
  Optional<Post> createPost(String userId, String body, String communityId, MultipartFile image);
  
  Optional<Post> getPostById(String postId);
  
  List<Post> getPostsByUserId(String userId);
  
  List<Post> getPostsByCommunityId(String communityId);
  
  List<Post> getGeneralPosts();
  
  List<Post> getAllPosts();
  
  List<Post> getUserFeed(String userId);
  
  Optional<Post> updatePost(String postId, String userId, String body);
  
  boolean deletePost(String postId, String userId);
} 