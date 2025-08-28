package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.exception.ConflictException;
import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.model.User;
import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.core.port.ImageStoragePort;
import com.uade.bookybe.core.usecase.UserService;
import com.uade.bookybe.core.usecase.GamificationService;
import com.uade.bookybe.infraestructure.entity.AddressEntity;
import com.uade.bookybe.infraestructure.entity.UserEntity;
import com.uade.bookybe.infraestructure.mapper.UserEntityMapper;
import com.uade.bookybe.infraestructure.repository.UserBookRepository;
import com.uade.bookybe.infraestructure.repository.UserRepository;
import com.uade.bookybe.router.dto.user.UserPreviewDto;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final UserBookRepository userBookRepository;
  private final BCryptPasswordEncoder passwordEncoder;
  private final ImageStoragePort imageStoragePort;
  private final GamificationService gamificationService;

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
      AddressEntity addressEntity =
          AddressEntity.builder()
              .id(UUID.randomUUID().toString())
              .state(user.getAddress().getState())
              .country(user.getAddress().getCountry())
              .longitude(user.getAddress().getLongitude())
              .latitude(user.getAddress().getLatitude())
              .build();
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
  @Transactional
  public boolean deleteUser(String id) {
    if (!userRepository.existsById(id)) {
      log.warn("Attempted to delete non-existent user: {}", id);
      return false;
    }
    
    try {
      log.info("Deleting user: {}", id);
      
      // Manual cleanup of gamification data first
      // This ensures gamification data is deleted even if FK constraints are not properly configured
      try {
        boolean gamificationDeleted = gamificationService.deleteUserGamificationData(id);
        if (gamificationDeleted) {
          log.info("Successfully deleted gamification data for user: {}", id);
        }
      } catch (Exception e) {
        log.warn("Could not delete gamification data for user {}: {}", id, e.getMessage());
      }
      
      // Delete the user (should CASCADE to related tables)
      userRepository.deleteById(id);
      log.info("User {} deleted successfully", id);
      return true;
      
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      log.error("Foreign key constraint violation when deleting user {}: {}", id, e.getMessage());
      
      // More specific error message
      String message = e.getMessage();
      if (message.contains("gamification_profiles")) {
        throw new RuntimeException("Cannot delete user: gamification profile exists. Please run FK migration script.", e);
      } else if (message.contains("user_achievements")) {
        throw new RuntimeException("Cannot delete user: user achievements exist. Please run FK migration script.", e);
      } else {
        throw new RuntimeException("Cannot delete user due to foreign key constraints: " + message, e);
      }
      
    } catch (Exception e) {
      log.error("Unexpected error deleting user {}: {}", id, e.getMessage(), e);
      throw new RuntimeException("Failed to delete user: " + id, e);
    }
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
    
    // Inicializar perfil de gamificación automáticamente para nuevo usuario
    gamificationService.initializeUserProfile(saved.getId());
    
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

  @Override
  public List<User> searchUsersByUsername(String searchTerm) {
    log.info("Searching for users with username containing: {}", searchTerm);
    
    List<UserEntity> userEntities = userRepository.findByUsernameContainingIgnoreCase(searchTerm);
    
    log.info("Found {} users matching search term: {}", userEntities.size(), searchTerm);
    
    return userEntities.stream()
        .map(UserEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  public List<UserPreviewDto> searchUsersByBooks(List<String> bookIds, String requestingUserId) {
    List<Object[]> userResults =
        userBookRepository.findUsersByBookIds(bookIds, requestingUserId, bookIds.size());

    return userResults.stream().map(this::mapToUserPreviewDto).collect(Collectors.toList());
  }

  private UserPreviewDto mapToUserPreviewDto(Object[] result) {
    UserPreviewDto dto = new UserPreviewDto();
    dto.setId((String) result[0]);
    dto.setUsername((String) result[1]);
    dto.setName((String) result[2]);
    dto.setLastname((String) result[3]);
    dto.setImage((String) result[4]);
    return dto;
  }
}
