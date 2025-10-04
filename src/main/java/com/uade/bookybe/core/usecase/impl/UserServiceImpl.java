package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.exception.ConflictException;
import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.model.User;
import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.core.port.ImageStoragePort;
import com.uade.bookybe.core.usecase.GamificationService;
import com.uade.bookybe.core.usecase.UserRateService;
import com.uade.bookybe.core.usecase.UserService;
import com.uade.bookybe.infraestructure.entity.AddressEntity;
import com.uade.bookybe.infraestructure.entity.UserEntity;
import com.uade.bookybe.infraestructure.mapper.UserEntityMapper;
import com.uade.bookybe.infraestructure.repository.UserBookRepository;
import com.uade.bookybe.infraestructure.repository.UserRepository;
import com.uade.bookybe.router.dto.user.AddressDto;
import com.uade.bookybe.router.dto.user.RateUserDto;
import com.uade.bookybe.router.dto.user.UserPreviewDto;
import com.uade.bookybe.router.mapper.UserDtoMapper;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final UserBookRepository userBookRepository;
  private final BCryptPasswordEncoder passwordEncoder;
  private final ImageStoragePort imageStoragePort;
  private final GamificationService gamificationService;
  private final UserRateService userRateService;

  @Override
  public Optional<User> getUserById(String id) {
    return userRepository.findById(id).map(UserEntityMapper.INSTANCE::toModel);
  }

  @Override
  public Optional<User> updateUser(String id, User user, String imageBase64) {
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
              .city(user.getAddress().getCity())
              .country(user.getAddress().getCountry())
              .longitude(user.getAddress().getLongitude())
              .latitude(user.getAddress().getLatitude())
              .build();
      existing.setAddress(addressEntity);
    }

    // Manejar imagen con base64
    if (imageBase64 != null && !imageBase64.isBlank()) {
      // Eliminar imagen anterior si existe
      if (existing.getImage() != null && !existing.getImage().isBlank()) {
        imageStoragePort.deleteImage(existing.getImage());
      }

      // Subir nueva imagen usando base64
      Optional<String> uploadedImageUrl = imageStoragePort.uploadImage(imageBase64, "booky/users");
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
      // This ensures gamification data is deleted even if FK constraints are not properly
      // configured
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
        throw new RuntimeException(
            "Cannot delete user: gamification profile exists. Please run FK migration script.", e);
      } else if (message.contains("user_achievements")) {
        throw new RuntimeException(
            "Cannot delete user: user achievements exist. Please run FK migration script.", e);
      } else {
        throw new RuntimeException(
            "Cannot delete user due to foreign key constraints: " + message, e);
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

    // Manejar imagen durante el registro si se proporciona
    if (userSignUp.getImage() != null && !userSignUp.getImage().isBlank()) {
      Optional<String> uploadedImageUrl =
          imageStoragePort.uploadImage(userSignUp.getImage(), "booky/users");
      if (uploadedImageUrl.isPresent()) {
        entity.setImage(uploadedImageUrl.get());
      }
    }

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

    // Obtener las coordenadas del usuario que hace la búsqueda
    Optional<UserEntity> requestingUser = userRepository.findById(requestingUserId);
    Double requestingUserLat = null;
    Double requestingUserLon = null;

    if (requestingUser.isPresent() && requestingUser.get().getAddress() != null) {
      requestingUserLat = requestingUser.get().getAddress().getLatitude();
      requestingUserLon = requestingUser.get().getAddress().getLongitude();
    }

    final Double finalRequestingUserLat = requestingUserLat;
    final Double finalRequestingUserLon = requestingUserLon;

    List<UserPreviewDto> users = userResults.stream()
        .map(this::mapToUserPreviewDto)
        .map(this::enrichWithAddress)
        .map(this::enrichWithRate)
        .collect(Collectors.toList());

    // Ordenar por distancia si el usuario solicitante tiene coordenadas
    if (finalRequestingUserLat != null && finalRequestingUserLon != null) {
      users.sort((user1, user2) -> {
        Double distance1 = calculateEuclideanDistance(
            finalRequestingUserLat, finalRequestingUserLon,
            user1.getAddress() != null ? user1.getAddress().getLatitude() : null,
            user1.getAddress() != null ? user1.getAddress().getLongitude() : null
        );
        Double distance2 = calculateEuclideanDistance(
            finalRequestingUserLat, finalRequestingUserLon,
            user2.getAddress() != null ? user2.getAddress().getLatitude() : null,
            user2.getAddress() != null ? user2.getAddress().getLongitude() : null
        );

        // Los usuarios sin coordenadas van al final
        if (distance1 == null && distance2 == null) return 0;
        if (distance1 == null) return 1;
        if (distance2 == null) return -1;

        return Double.compare(distance1, distance2);
      });
    }

    return users;
  }

  /**
   * Calcula la distancia euclidiana entre dos puntos geográficos
   * @param lat1 Latitud del punto 1
   * @param lon1 Longitud del punto 1
   * @param lat2 Latitud del punto 2
   * @param lon2 Longitud del punto 2
   * @return Distancia euclidiana o null si alguna coordenada es null
   */
  private Double calculateEuclideanDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
    if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
      return null;
    }

    double deltaLat = lat2 - lat1;
    double deltaLon = lon2 - lon1;

    return Math.sqrt(deltaLat * deltaLat + deltaLon * deltaLon);
  }

  private UserPreviewDto enrichWithAddress(UserPreviewDto userPreviewDto) {
    userRepository
        .findById(userPreviewDto.getId())
        .ifPresent(
            userEntity -> {
              if (userEntity.getAddress() != null) {
                userPreviewDto.setAddress(
                    AddressDto.builder()
                        .city(userEntity.getAddress().getCity())
                        .state(userEntity.getAddress().getState())
                        .country(userEntity.getAddress().getCountry())
                        .latitude(userEntity.getAddress().getLatitude())
                        .longitude(userEntity.getAddress().getLongitude())
                        .build());
              }
            });
    return userPreviewDto;
  }

  private UserPreviewDto enrichWithRate(UserPreviewDto userPreviewDto) {

    Double averageRating = userRateService.getUserAverageRating(userPreviewDto.getId());
    Long countRating = userRateService.getUserRatingCount(userPreviewDto.getId());
    RateUserDto rateUserDto =
        RateUserDto.builder().totalRatings(countRating).averageRating(averageRating).build();
    userPreviewDto.setUserRate(rateUserDto);
    return userPreviewDto;
  }

  @Override
  public List<UserPreviewDto> searchUsersByLocation(
      Double bottomLeftLatitude,
      Double bottomLeftLongitude,
      Double topRightLatitude,
      Double topRightLongitude) {
    log.info(
        "Searching for users within geographic bounds: bottomLeft({}, {}), topRight({}, {})",
        bottomLeftLatitude,
        bottomLeftLongitude,
        topRightLatitude,
        topRightLongitude);

    List<UserEntity> userEntities =
        userRepository.findUsersByLocationBounds(
            bottomLeftLatitude, bottomLeftLongitude, topRightLatitude, topRightLongitude);

    log.info("Found {} users within geographic bounds", userEntities.size());

    return userEntities.stream()
        .map(UserEntityMapper.INSTANCE::toModel)
        .map(UserDtoMapper.INSTANCE::toPreviewDto)
        .collect(Collectors.toList());
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
