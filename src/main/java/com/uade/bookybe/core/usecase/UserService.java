package com.uade.bookybe.core.usecase;

import com.uade.bookybe.router.dto.user.UserDto;
import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.core.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface UserService {
  Optional<UserDto> getUserById(String id);

  Optional<UserDto> updateUser(String id, UserDto userDto);

  Optional<UserDto> updateUserWithImage(String id, User user, MultipartFile image);

  boolean deleteUser(String id);

  boolean followUser(String followerId, String followedId);
  
  boolean unfollowUser(String followerId, String followedId);

  List<User> getFollowers(String userId);
  
  List<User> getFollowing(String userId);

  Optional<User> signUp(UserSignUp userSignUp);
  
  Optional<User> signIn(String email, String password);
}
