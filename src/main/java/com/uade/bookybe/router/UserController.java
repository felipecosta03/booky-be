package com.uade.bookybe.router;

import com.uade.bookybe.core.model.User;
import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.core.usecase.UserService;
import com.uade.bookybe.router.dto.user.*;
import com.uade.bookybe.router.mapper.UserDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "User Management",
    description = "APIs for user registration, authentication, and profile management")
public class UserController {
  private final UserService userService;

  @Operation(
      summary = "Get user by ID",
      description = "Retrieves a user's profile information by their unique ID")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
      })
  @GetMapping("/users/{id}")
  public ResponseEntity<UserDto> getUser(
      @Parameter(
              description = "User ID",
              required = true,
              example = "123e4567-e89b-12d3-a456-426614174000")
          @PathVariable
          String id) {
    log.info("Getting user with ID: {}", id);

    return userService
        .getUserById(id)
        .map(UserDtoMapper.INSTANCE::toDto)
        .map(
            user -> {
              log.info("User found: {}", user.getUsername());
              return ResponseEntity.ok(user);
            })
        .orElseGet(
            () -> {
              log.warn("User not found with ID: {}", id);
              return ResponseEntity.notFound().build();
            });
  }

  @Operation(
      summary = "Register new user",
      description = "Creates a new user account with the provided information")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User registered successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data or user already exists",
            content = @Content)
      })
  @PostMapping("/sign-up")
  public ResponseEntity<UserDto> signUp(
      @Parameter(description = "User registration data", required = true) @Valid @RequestBody
          UserSignUpDto userSignUpDto) {
    log.info("Attempting to register new user with email: {}", userSignUpDto.getEmail());

    UserSignUp userSignUp = UserDtoMapper.INSTANCE.toModel(userSignUpDto);
    return userService
        .signUp(userSignUp)
        .map(UserDtoMapper.INSTANCE::toDto)
        .map(
            user -> {
              log.info("User registered successfully: {}", user.getUsername());
              return ResponseEntity.ok(user);
            })
        .orElseGet(
            () -> {
              log.warn("Failed to register user with email: {}", userSignUpDto.getEmail());
              return ResponseEntity.badRequest().build();
            });
  }

  @Operation(summary = "User login", description = "Authenticates a user with email and password")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content)
      })
  @PostMapping("/sign-in")
  public ResponseEntity<UserDto> signIn(
      @Parameter(description = "User login credentials", required = true) @Valid @RequestBody
          UserSignInDto userSignInDto) {
    log.info("Login attempt for email: {}", userSignInDto.getEmail());

    return userService
        .signIn(userSignInDto.getEmail(), userSignInDto.getPassword())
        .map(UserDtoMapper.INSTANCE::toDto)
        .map(
            user -> {
              log.info("User logged in successfully: {}", user.getUsername());
              return ResponseEntity.ok(user);
            })
        .orElseGet(
            () -> {
              log.warn("Failed login attempt for email: {}", userSignInDto.getEmail());
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            });
  }

  @Operation(
      summary = "Update user profile",
      description = "Updates user profile information and optionally uploads a profile image")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content)
      })
  @PutMapping(value = "/users", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<UserDto> updateUserWithImage(
      @Parameter(description = "User update data", required = true) @RequestPart("user") @Valid
          UserUpdateDto userUpdateDto,
      @Parameter(description = "Profile image file (optional)")
          @RequestPart(value = "image", required = false)
          MultipartFile image) {

    log.info("Updating user with ID: {}", userUpdateDto.getId());

    if (userUpdateDto.getId() == null || userUpdateDto.getId().isBlank()) {
      log.warn("Update attempt with missing user ID");
      return ResponseEntity.badRequest().build();
    }

    User user = UserDtoMapper.INSTANCE.toModel(userUpdateDto);
    return userService
        .updateUser(userUpdateDto.getId(), user, image)
        .map(UserDtoMapper.INSTANCE::toDto)
        .map(
            updatedUser -> {
              log.info("User updated successfully: {}", updatedUser.getUsername());
              return ResponseEntity.ok(updatedUser);
            })
        .orElseGet(
            () -> {
              log.warn("Failed to update user with ID: {}", userUpdateDto.getId());
              return ResponseEntity.notFound().build();
            });
  }

  @Operation(
      summary = "Get user followers",
      description = "Retrieves the list of users following the specified user")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Followers retrieved successfully",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/users/{id}/followers")
  public ResponseEntity<List<UserPreviewDto>> getFollowers(
      @Parameter(description = "User ID", required = true) @PathVariable String id) {
    log.info("Getting followers for user ID: {}", id);

    List<User> followers = userService.getFollowers(id);
    List<UserPreviewDto> followersDto =
        followers.stream().map(UserDtoMapper.INSTANCE::toPreviewDto).collect(Collectors.toList());

    log.info("Found {} followers for user ID: {}", followersDto.size(), id);
    return ResponseEntity.ok(followersDto);
  }

  @Operation(
      summary = "Get users followed by user",
      description = "Retrieves the list of users that the specified user is following")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Following list retrieved successfully",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/users/{id}/following")
  public ResponseEntity<List<UserPreviewDto>> getFollowing(
      @Parameter(description = "User ID", required = true) @PathVariable String id) {
    log.info("Getting following list for user ID: {}", id);

    List<User> following = userService.getFollowing(id);
    List<UserPreviewDto> followingDto =
        following.stream().map(UserDtoMapper.INSTANCE::toPreviewDto).collect(Collectors.toList());

    log.info("User ID: {} is following {} users", id, followingDto.size());
    return ResponseEntity.ok(followingDto);
  }

  @Operation(
      summary = "Follow user",
      description = "Creates a follow relationship between two users")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "202", description = "Follow successful"),
        @ApiResponse(
            responseCode = "400",
            description = "Follow failed (user already followed or invalid IDs)")
      })
  @PostMapping("/users/{followerId}/follow/{followedId}")
  public ResponseEntity<Void> followUser(
      @Parameter(description = "ID of the user who wants to follow", required = true) @PathVariable
          String followerId,
      @Parameter(description = "ID of the user to be followed", required = true) @PathVariable
          String followedId) {
    log.info("User {} attempting to follow user {}", followerId, followedId);

    boolean followed = userService.followUser(followerId, followedId);
    if (followed) {
      log.info("User {} successfully followed user {}", followerId, followedId);
      return ResponseEntity.accepted().build();
    } else {
      log.warn("Failed to follow - User {} could not follow user {}", followerId, followedId);
      return ResponseEntity.badRequest().build();
    }
  }

  @Operation(
      summary = "Unfollow user",
      description = "Removes a follow relationship between two users")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Unfollow successful"),
        @ApiResponse(
            responseCode = "400",
            description = "Unfollow failed (user was not being followed)")
      })
  @DeleteMapping("/users/{followerId}/follow/{followedId}")
  public ResponseEntity<Void> unfollowUser(
      @Parameter(description = "ID of the user who wants to unfollow", required = true)
          @PathVariable
          String followerId,
      @Parameter(description = "ID of the user to be unfollowed", required = true) @PathVariable
          String followedId) {
    log.info("User {} attempting to unfollow user {}", followerId, followedId);

    boolean unfollowed = userService.unfollowUser(followerId, followedId);
    if (unfollowed) {
      log.info("User {} successfully unfollowed user {}", followerId, followedId);
      return ResponseEntity.noContent().build();
    } else {
      log.warn("Failed to unfollow - User {} could not unfollow user {}", followerId, followedId);
      return ResponseEntity.badRequest().build();
    }
  }

  @Operation(summary = "Delete user", description = "Permanently deletes a user account")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  @DeleteMapping("/users/{id}")
  public ResponseEntity<Void> deleteUser(
      @Parameter(description = "User ID to delete", required = true) @PathVariable String id) {
    log.info("Attempting to delete user with ID: {}", id);

    boolean deleted = userService.deleteUser(id);
    if (deleted) {
      log.info("User deleted successfully with ID: {}", id);
      return ResponseEntity.noContent().build();
    } else {
      log.warn("Failed to delete user - User not found with ID: {}", id);
      return ResponseEntity.notFound().build();
    }
  }
}
