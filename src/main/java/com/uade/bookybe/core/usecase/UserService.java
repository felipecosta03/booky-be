package com.uade.bookybe.core.usecase;

import com.uade.bookybe.core.model.User;
import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.router.dto.user.UserPreviewDto;
import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

public interface UserService {
  Optional<User> getUserById(String id);

  Optional<User> updateUser(String id, User user, MultipartFile image);

  boolean deleteUser(String id);

  boolean followUser(String followerId, String followedId);

  boolean unfollowUser(String followerId, String followedId);

  List<User> getFollowers(String userId);

  List<User> getFollowing(String userId);

  Optional<User> signUp(UserSignUp userSignUp);

  Optional<User> signIn(String email, String password);

  /**
   * Search users who have specific books available for exchange
   * @param bookIds List of book IDs to search for
   * @param requestingUserId ID of the user making the request (to get their address for distance calculation)
   * @param page Page number (0-based)
   * @param limit Number of results per page
   * @return Map with users and their matching book counts, ordered by distance if requesting user has address
   */
  List<UserPreviewDto> searchUsersByBooks(List<String> bookIds, String requestingUserId);
}
