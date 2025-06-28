package com.uade.bookybe.router;

import com.uade.bookybe.core.usecase.UserService;
import com.uade.bookybe.router.dto.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping("/users/{id}")
  public ResponseEntity<UserDto> getUser(@PathVariable String id) {

    return null;
  }

  @PostMapping("/sign-up")
  public ResponseEntity<UserDto> signUp(@RequestBody UserSignUpDto userSignUpDto) {

    return null;
  }

  @PostMapping("/sign-in")
  public ResponseEntity<UserDto> createUser(@RequestBody UserSignUpDto userSignInDto) {

    return null;
  }

  @PutMapping("/users")
  public ResponseEntity<UserDto> updateUser(
      @RequestPart("user") UserUpdateDto userDto,
      @RequestPart(value = "image", required = false) MultipartFile image) {

    return null;
  }

  @GetMapping("/users/{id}/followers")
  public ResponseEntity<UserPreviewDto> getFollowers(@PathVariable String id) {
    return null;
  }

  @GetMapping("/users/{id}/following")
  public ResponseEntity<UserPreviewDto> getFollowing(@PathVariable String id) {
    return null;
  }

  @GetMapping("/users/{id}/followers-requests")
  public ResponseEntity<FollowRequestDto> getFollowersRequests(@PathVariable String id) {
    return null;
  }

  @PostMapping("/users/{id}/follow-requests")
  public ResponseEntity<Void> sendFollowRequest(@PathVariable String id) {
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
      @PathVariable String followerId, @PathVariable String followedId) {
    boolean followed = userService.followUser(followerId, followedId);
    if (followed) {
      return ResponseEntity.accepted().build();
    } else {
      return ResponseEntity.badRequest().build();
    }
  }
}
