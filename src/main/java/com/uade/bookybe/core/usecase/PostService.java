package com.uade.bookybe.core.usecase;

import com.uade.bookybe.core.model.Post;
import java.util.List;
import java.util.Optional;

public interface PostService {

  Optional<Post> createPost(String userId, String body, String communityId, String imageBase64);

  Optional<Post> getPostById(String postId);

  List<Post> getPostsByUserId(String userId);

  List<Post> getPostsByCommunityId(String communityId);

  List<Post> getGeneralPosts();

  List<Post> getAllPosts();

  List<Post> getUserFeed(String userId);

  /** Gets posts with optional filters */
  List<Post> getPostsFiltered(String type, String userId, String communityId, String requestingUserId);

  Optional<Post> updatePost(String postId, String userId, String body);

  boolean deletePost(String postId, String userId);
}
