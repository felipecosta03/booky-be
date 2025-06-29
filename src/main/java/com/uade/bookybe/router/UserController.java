package com.uade.bookybe.router;

import com.uade.bookybe.core.model.User;
import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.core.usecase.UserService;
import com.uade.bookybe.router.dto.user.*;
import com.uade.bookybe.router.mapper.UserDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping("/users/{id}")
  public ResponseEntity<UserDto> getUser(@PathVariable String id) {
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

  @PostMapping("/sign-in")
  public ResponseEntity<UserDto> signIn(@RequestBody UserSignInDto userSignInDto) {
    if (userSignInDto.getEmail() == null || userSignInDto.getEmail().isBlank() ||
        userSignInDto.getPassword() == null || userSignInDto.getPassword().isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    return userService
        .signIn(userSignInDto.getEmail(), userSignInDto.getPassword())
        .map(UserDtoMapper.INSTANCE::toDto)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }

  @PutMapping("/users")
  public ResponseEntity<UserDto> updateUserWithImage(
      @RequestPart("user") UserUpdateDto userUpdateDto,
      @RequestPart(value = "image", required = false) MultipartFile image) {
    
    if (userUpdateDto.getId() == null || userUpdateDto.getId().isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    
    User user = UserDtoMapper.INSTANCE.toModel(userUpdateDto);
    return userService
        .updateUserWithImage(userUpdateDto.getId(), user, image)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/users/{id}/followers")
  public ResponseEntity<List<UserPreviewDto>> getFollowers(@PathVariable String id) {
    List<User> followers = userService.getFollowers(id);
    List<UserPreviewDto> followersDto = followers.stream()
        .map(UserDtoMapper.INSTANCE::toPreviewDto)
        .collect(Collectors.toList());
    return ResponseEntity.ok(followersDto);
  }

  @GetMapping("/users/{id}/following")
  public ResponseEntity<List<UserPreviewDto>> getFollowing(@PathVariable String id) {
    List<User> following = userService.getFollowing(id);
    List<UserPreviewDto> followingDto = following.stream()
        .map(UserDtoMapper.INSTANCE::toPreviewDto)
        .collect(Collectors.toList());
    return ResponseEntity.ok(followingDto);
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

  @DeleteMapping("/users/{followerId}/follow/{followedId}")
  public ResponseEntity<Void> unfollowUser(
      @PathVariable String followerId, @PathVariable String followedId) {
    boolean unfollowed = userService.unfollowUser(followerId, followedId);
    if (unfollowed) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/users/{id}")
  public ResponseEntity<UserDto> updateUser(@PathVariable String id, @RequestBody UserDto userDto) {
    return userService
        .updateUser(id, userDto)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @DeleteMapping("/users/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable String id) {
    boolean deleted = userService.deleteUser(id);
    if (!deleted) return ResponseEntity.notFound().build();
    return ResponseEntity.noContent().build();
  }
}
