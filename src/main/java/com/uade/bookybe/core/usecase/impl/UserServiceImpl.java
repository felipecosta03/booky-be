package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.model.User;
import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.core.usecase.UserService;
import com.uade.bookybe.infraestructure.entity.UserEntity;
import com.uade.bookybe.infraestructure.mapper.UserEntityMapper;
import com.uade.bookybe.infraestructure.repository.UserRepository;
import com.uade.bookybe.router.dto.user.UserDto;
import com.uade.bookybe.router.mapper.UserDtoMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;

  @Override
  public Optional<UserDto> getUserById(Long id) {
    return userRepository
        .findById(id)
        .map(UserEntityMapper.INSTANCE::toModel)
        .map(UserDtoMapper.INSTANCE::toDto);
  }

  @Override
  public Optional<UserDto> updateUser(Long id, UserDto userDto) {
    Optional<UserEntity> existing = userRepository.findById(id);
    if (existing.isEmpty()) return Optional.empty();
    User user = UserDtoMapper.INSTANCE.toModel(userDto);
    user.setId(id);
    if (user.getPassword() != null && !user.getPassword().isBlank()) {
      user.setPassword(passwordEncoder.encode(user.getPassword()));
    } else {
      user.setPassword(existing.get().getPassword());
    }
    UserEntity entity = UserEntityMapper.INSTANCE.toEntity(user);
    UserEntity saved = userRepository.save(entity);
    return Optional.ofNullable(
        UserDtoMapper.INSTANCE.toDto(UserEntityMapper.INSTANCE.toModel(saved)));
  }

  @Override
  public boolean deleteUser(Long id) {
    if (!userRepository.existsById(id)) return false;
    userRepository.deleteById(id);
    return true;
  }

  @Override
  @Transactional
  public boolean followUser(Long followerId, Long followedId) {
    if (userRepository.isFollowing(followerId, followedId)) {
      return false;
    }
    userRepository.follow(followerId, followedId);
    return true;
  }

  @Override
  public Optional<User> signUp(UserSignUp userSignUp) {
    User user = buildUserBySignUp(userSignUp);
    UserEntity entity = UserEntityMapper.INSTANCE.toEntity(user);
    UserEntity saved = userRepository.save(entity);
    return Optional.ofNullable(UserEntityMapper.INSTANCE.toModel(saved));
  }

  private User buildUserBySignUp(UserSignUp userSignUp) {
    return User.builder()
        .email(userSignUp.getEmail())
        .name(userSignUp.getName())
        .dateCreated(LocalDateTime.now())
        .username(userSignUp.getUsername())
        .lastname(userSignUp.getLastname())
        .password(passwordEncoder.encode(userSignUp.getPassword()))
        .build();
  }
}
