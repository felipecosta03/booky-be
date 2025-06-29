package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.model.User;
import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.core.usecase.UserService;
import com.uade.bookybe.infraestructure.entity.UserEntity;
import com.uade.bookybe.infraestructure.mapper.UserEntityMapper;
import com.uade.bookybe.infraestructure.repository.UserRepository;
import com.uade.bookybe.router.dto.user.UserDto;
import com.uade.bookybe.router.mapper.UserDtoMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;

  @Override
  public Optional<UserDto> getUserById(String id) {
    return userRepository
        .findById(id)
        .map(UserEntityMapper.INSTANCE::toModel)
        .map(UserDtoMapper.INSTANCE::toDto);
  }

  @Override
  public Optional<UserDto> updateUser(String id, UserDto userDto) {
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
  public Optional<UserDto> updateUserWithImage(String id, User user, MultipartFile image) {
    UserEntity existing =
        userRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

    if (image != null && !image.isEmpty()) {
      String imageName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
      user.setImage(imageName);
    }

    if (user.getPassword() != null && !user.getPassword().isBlank()) {
      existing.setPassword(passwordEncoder.encode(user.getPassword()));
    }

    UserEntity saved = userRepository.save(existing);
    return Optional.ofNullable(
        UserDtoMapper.INSTANCE.toDto(UserEntityMapper.INSTANCE.toModel(saved)));
  }

  @Override
  public boolean deleteUser(String id) {
    if (!userRepository.existsById(id)) return false;
    userRepository.deleteById(id);
    return true;
  }

  @Override
  @Transactional
  public boolean followUser(String followerId, String followedId) {
    if (userRepository.isFollowing(followerId, followedId)) {
      return false;
    }
    userRepository.follow(followerId, followedId);
    return true;
  }

  @Override
  @Transactional
  public boolean unfollowUser(String followerId, String followedId) {
    if (!userRepository.isFollowing(followerId, followedId)) {
      return false;
    }
    userRepository.unfollow(followerId, followedId);
    return true;
  }

  @Override
  public List<User> getFollowers(String userId) {
    return userRepository.findFollowers(userId).stream()
        .map(UserEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  public List<User> getFollowing(String userId) {
    return userRepository.findFollowing(userId).stream()
        .map(UserEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<User> signUp(UserSignUp userSignUp) {
    User user = buildUserBySignUp(userSignUp);
    UserEntity entity = UserEntityMapper.INSTANCE.toEntity(user);
    UserEntity saved = userRepository.save(entity);
    return Optional.ofNullable(UserEntityMapper.INSTANCE.toModel(saved));
  }

  @Override
  public Optional<User> signIn(String email, String password) {
    Optional<UserEntity> userEntity = userRepository.findByEmail(email);
    if (userEntity.isEmpty()) {
      return Optional.empty();
    }

    if (passwordEncoder.matches(password, userEntity.get().getPassword())) {
      return Optional.ofNullable(UserEntityMapper.INSTANCE.toModel(userEntity.get()));
    }

    return Optional.empty();
  }

  private User buildUserBySignUp(UserSignUp userSignUp) {
    return User.builder()
        .id(UUID.randomUUID().toString())
        .email(userSignUp.getEmail())
        .name(userSignUp.getName())
        .dateCreated(LocalDateTime.now())
        .username(userSignUp.getUsername())
        .lastname(userSignUp.getLastname())
        .password(passwordEncoder.encode(userSignUp.getPassword()))
        .build();
  }
}
