package com.uade.bookybe.router;

import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.core.usecase.UserService;
import com.uade.bookybe.router.dto.user.*;
import com.uade.bookybe.router.mapper.UserDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping("/users/{id}")
  public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
    return userService
        .getUserById(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PostMapping("/sign-up")
  public ResponseEntity<UserDto> signUp(@RequestBody UserSignUpDto userSignUpDto) {
    if (userSignUpDto.getPassword() == null || userSignUpDto.getPassword().isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    UserSignUp userSignUp = UserDtoMapper.INSTANCE.toModel(userSignUpDto);
    return userService
        .signUp(userSignUp)
        .map(UserDtoMapper.INSTANCE::toDto)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.badRequest().build());
  }

  @PutMapping("/users")
  public ResponseEntity<UserDto> updateUser(
      @RequestPart("user") UserUpdateDto userDto,
      @RequestPart(value = "image", required = false) MultipartFile image) {

    return null;
  }

  @GetMapping("/users/{id}/followers")
  public ResponseEntity<UserPreviewDto> getFollowers(@PathVariable Long id) {
    return null;
  }

  @GetMapping("/users/{id}/following")
  public ResponseEntity<UserPreviewDto> getFollowing(@PathVariable Long id) {
    return null;
  }

  @GetMapping("/users/{id}/followers-requests")
  public ResponseEntity<FollowRequestDto> getFollowersRequests(@PathVariable Long id) {
    return null;
  }

  @PostMapping("/users/{id}/follow-requests")
  public ResponseEntity<Void> sendFollowRequest(@PathVariable Long id) {
    return null;
  }

  @PostMapping("/users/follow-requests/{requestId}/accept")
  public ResponseEntity<Void> acceptFollowRequest(@PathVariable String requestId) {
    return null;
  }

  @PostMapping("/users/follow-requests/{requestId}/reject")
  public ResponseEntity<Void> rejectFollowRequest(@PathVariable String requestId) {
    return null;
  }

  @PostMapping("/users/{followerId}/follow/{followedId}")
  public ResponseEntity<Void> followUser(
      @PathVariable Long followerId, @PathVariable Long followedId) {
    boolean followed = userService.followUser(followerId, followedId);
    if (followed) {
      return ResponseEntity.accepted().build();
    } else {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/users/{id}")
  public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody UserDto userDto) {
    return userService
        .updateUser(id, userDto)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @DeleteMapping("/users/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    boolean deleted = userService.deleteUser(id);
    if (!deleted) return ResponseEntity.notFound().build();
    return ResponseEntity.noContent().build();
  }
}
