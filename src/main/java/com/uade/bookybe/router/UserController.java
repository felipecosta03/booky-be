package com.uade.bookybe.router;

import com.uade.bookybe.core.model.User;
import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.core.service.JwtService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "User Management",
    description = "APIs for user registration, authentication, and profile management")
public class UserController {
  private final UserService userService;
  private final JwtService jwtService;

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
                    schema = @Schema(implementation = UserSignInResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content)
      })
  @PostMapping("/sign-in")
  public ResponseEntity<UserSignInResponseDto> signIn(
      @Parameter(description = "User login credentials", required = true) @Valid @RequestBody
          UserSignInDto userSignInDto) {
    log.info("Login attempt for email: {}", userSignInDto.getEmail());

    return userService
        .signIn(userSignInDto.getEmail(), userSignInDto.getPassword())
        .map(
            user -> {
              // Generate JWT token
              String token = jwtService.generateToken(user.getId(), user.getEmail());

              // Create response with token and user data
              UserSignInResponseDto response =
                  UserSignInResponseDto.builder()
                      .token(token)
                      .user(UserDtoMapper.INSTANCE.toDto(user))
                      .build();

              log.info("User logged in successfully: {}", user.getUsername());
              return ResponseEntity.ok(response);
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
  @PutMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UserDto> updateUser(
      @Parameter(description = "User update data with optional base64 image", required = true)
      @Valid @RequestBody UserUpdateDto userUpdateDto) {

    log.info("Updating user with ID: {}", userUpdateDto.getId());

    if (userUpdateDto.getId() == null || userUpdateDto.getId().isBlank()) {
      log.warn("Update attempt with missing user ID");
      return ResponseEntity.badRequest().build();
    }

    User user = UserDtoMapper.INSTANCE.toModel(userUpdateDto);
    return userService
        .updateUser(userUpdateDto.getId(), user, userUpdateDto.getImage())
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
      description = "Creates a follow relationship with the specified user")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "202", description = "Follow successful"),
        @ApiResponse(
            responseCode = "400",
            description = "Follow failed (user already followed or invalid ID)")
      })
  @PostMapping("/users/follow")
  public ResponseEntity<Void> followUser(
      @Parameter(description = "Follow request data", required = true) @Valid @RequestBody FollowUserDto followDto,
      Authentication authentication) {
    
    String followerId = authentication.getName();
    String followedId = followDto.getTargetUserId();
    
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
      description = "Removes a follow relationship with the specified user")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Unfollow successful"),
        @ApiResponse(
            responseCode = "400",
            description = "Unfollow failed (user was not being followed)")
      })
  @PostMapping("/users/unfollow")
  public ResponseEntity<Void> unfollowUser(
      @Parameter(description = "Unfollow request data", required = true) @Valid @RequestBody FollowUserDto followDto,
      Authentication authentication) {
    
    String followerId = authentication.getName();
    String followedId = followDto.getTargetUserId();
    
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

  @Operation(
      summary = "Search users by username",
      description = "Find users by partial username match (case insensitive)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users found successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid search term", content = @Content)
      })
  @GetMapping("/users/search")
  public ResponseEntity<List<UserPreviewDto>> searchUsersByUsername(
      @Parameter(description = "Username search term", required = true, example = "agus")
          @RequestParam("q") String searchTerm) {

    log.info("Searching for users with username containing: {}", searchTerm);

    if (searchTerm == null || searchTerm.trim().length() < 2) {
      log.warn("Search term too short or empty: {}", searchTerm);
      return ResponseEntity.badRequest().build();
    }

    List<User> users = userService.searchUsersByUsername(searchTerm.trim());
    List<UserPreviewDto> result = users.stream()
        .map(UserDtoMapper.INSTANCE::toPreviewDto)
        .collect(Collectors.toList());

    log.info("Found {} users matching username search: {}", result.size(), searchTerm);
    
    return ResponseEntity.ok(result);
  }

  @Operation(
      summary = "Search users by books for exchange",
      description = "Find users who have specific books available for exchange, ordered by distance if requesting user has address")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users found successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
      })
  @PostMapping("/users/search-by-books")
  public ResponseEntity<List<UserPreviewDto>> searchUsersByBooks(
      @Parameter(description = "Search criteria with book list", required = true)
          @Valid @RequestBody SearchUsersByBooksDto searchDto,
      Authentication authentication) {

    log.info("User {} searching for users with books: {}", authentication.getName(), searchDto.getBookIds());

    String requestingUserId = authentication.getName();
    
    List<UserPreviewDto> result = userService.searchUsersByBooks(
        searchDto.getBookIds(),
        requestingUserId
    );

    log.info("Found {} users for book search by user: {}", 
             result.size(), requestingUserId);
    
    return ResponseEntity.ok(result);
  }
}
