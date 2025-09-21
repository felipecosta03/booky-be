package com.uade.bookybe.core.usecase;

import com.uade.bookybe.core.model.User;
import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.router.dto.user.UserPreviewDto;
import java.util.List;
import java.util.Optional;

public interface UserService {
  Optional<User> getUserById(String id);

  Optional<User> updateUser(String id, User user, String imageBase64);

  boolean deleteUser(String id);

  boolean followUser(String followerId, String followedId);

  boolean unfollowUser(String followerId, String followedId);

  List<User> getFollowers(String userId);

  List<User> getFollowing(String userId);

  Optional<User> signUp(UserSignUp userSignUp);

  Optional<User> signIn(String email, String password);

  /**
   * Search users by username (partial match, case insensitive)
   * @param searchTerm The username search term
   * @return List of users whose username contains the search term
   */
  List<User> searchUsersByUsername(String searchTerm);

  /**
   * Search users who have specific books available for exchange
   * @param bookIds List of book IDs to search for
   * @param requestingUserId ID of the user making the request (to get their address for distance calculation)
   * @return List of users with their matching book counts, ordered by distance if requesting user has address
   */
  List<UserPreviewDto> searchUsersByBooks(List<String> bookIds, String requestingUserId);
}
