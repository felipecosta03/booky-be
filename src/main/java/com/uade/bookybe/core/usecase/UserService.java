package com.uade.bookybe.core.usecase;

import com.uade.bookybe.core.model.User;
import com.uade.bookybe.core.model.UserSignUp;
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
}
