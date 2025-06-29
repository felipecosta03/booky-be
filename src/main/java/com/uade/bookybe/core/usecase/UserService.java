package com.uade.bookybe.core.usecase;

import com.uade.bookybe.router.dto.user.UserDto;
import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.core.model.User;

import java.util.Optional;

public interface UserService {
  Optional<UserDto> getUserById(Long id);

  Optional<UserDto> updateUser(Long id, UserDto userDto);

  boolean deleteUser(Long id);

  boolean followUser(Long followerId, Long followedId);

  Optional<User> signUp(UserSignUp userSignUp);
}
