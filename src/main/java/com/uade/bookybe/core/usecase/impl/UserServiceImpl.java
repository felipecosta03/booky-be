package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.exception.ConflictException;
import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.model.User;
import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.core.port.ImageStoragePort;
import com.uade.bookybe.core.usecase.UserService;
import com.uade.bookybe.infraestructure.entity.AddressEntity;
import com.uade.bookybe.infraestructure.entity.UserEntity;
import com.uade.bookybe.infraestructure.mapper.UserEntityMapper;
import com.uade.bookybe.infraestructure.repository.UserRepository;
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
  private final ImageStoragePort imageStoragePort;

  @Override
  public Optional<User> getUserById(String id) {
    return userRepository.findById(id).map(UserEntityMapper.INSTANCE::toModel);
  }

  @Override
  public Optional<User> updateUser(String id, User user, MultipartFile image) {
    UserEntity existing =
        userRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

    // Actualizar campos del usuario
    if (user.getName() != null) {
      existing.setName(user.getName());
    }
    if (user.getLastname() != null) {
      existing.setLastname(user.getLastname());
    }
    if (user.getDescription() != null) {
      existing.setDescription(user.getDescription());
    }

    // Actualizar dirección si se proporciona
    if (user.getAddress() != null) {
      AddressEntity addressEntity = new AddressEntity();
      addressEntity.setId(user.getAddress().getId());
      addressEntity.setState(user.getAddress().getState());
      addressEntity.setCountry(user.getAddress().getCountry());
      addressEntity.setLongitude(user.getAddress().getLongitude());
      addressEntity.setLatitude(user.getAddress().getLatitude());
      existing.setAddress(addressEntity);
    }

    // Manejar imagen
    if (image != null && !image.isEmpty()) {
      // Eliminar imagen anterior si existe
      if (existing.getImage() != null && !existing.getImage().isBlank()) {
        imageStoragePort.deleteImage(existing.getImage());
      }

      // Subir nueva imagen
      Optional<String> uploadedImageUrl = imageStoragePort.uploadImage(image, "booky/users");
      if (uploadedImageUrl.isPresent()) {
        existing.setImage(uploadedImageUrl.get());
      }
    } else if (user.getImage() != null) {
      existing.setImage(user.getImage());
    }

    // Actualizar password solo si se proporciona
    if (user.getPassword() != null && !user.getPassword().isBlank()) {
      existing.setPassword(passwordEncoder.encode(user.getPassword()));
    }

    UserEntity saved = userRepository.save(existing);
    return Optional.ofNullable(UserEntityMapper.INSTANCE.toModel(saved));
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
    userRepository
        .findByEmail(userSignUp.getEmail())
        .ifPresent(
            (a) -> {
              throw new ConflictException("User already exists");
            });

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
