package com.uade.bookybe.core.usecase;

import com.uade.bookybe.router.dto.user.UserDto;
import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.core.model.User;

import java.util.Optional;

public interface UserService {
  Optional<UserDto> getUserById(String id);

  Optional<UserDto> updateUser(String id, UserDto userDto);

  boolean deleteUser(String id);

  boolean followUser(String followerId, String followedId);

  Optional<User> signUp(UserSignUp userSignUp);
}
