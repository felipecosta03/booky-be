package com.uade.bookybe.router;

import com.uade.bookybe.core.model.User;
import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.core.service.JwtService;
import com.uade.bookybe.core.usecase.UserService;
import com.uade.bookybe.router.dto.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user123")
                .username("testuser")
                .email("test@example.com")
                .build();
    }

    @Test
    void getUser_Success() {
        // Arrange
        when(userService.getUserById("user123")).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<UserDto> response = userController.getUser("user123");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(userService).getUserById("user123");
    }

    @Test
    void getUser_NotFound() {
        // Arrange
        when(userService.getUserById("user123")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<UserDto> response = userController.getUser("user123");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void signUp_Success() {
        // Arrange
        UserSignUpDto dto = new UserSignUpDto();
        dto.setEmail("test@example.com");
        dto.setPassword("password123");
        dto.setUsername("testuser");

        when(userService.signUp(any(UserSignUp.class))).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<UserDto> response = userController.signUp(dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(userService).signUp(any(UserSignUp.class));
    }

    @Test
    void signUp_Failed() {
        // Arrange
        UserSignUpDto dto = new UserSignUpDto();
        dto.setEmail("test@example.com");
        dto.setPassword("password123");

        when(userService.signUp(any(UserSignUp.class))).thenReturn(Optional.empty());

        // Act
        ResponseEntity<UserDto> response = userController.signUp(dto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void signIn_Success() {
        // Arrange
        UserSignInDto dto = new UserSignInDto();
        dto.setEmail("test@example.com");
        dto.setPassword("password123");

        when(userService.signIn("test@example.com", "password123")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken("user123", "test@example.com")).thenReturn("jwt-token");

        // Act
        ResponseEntity<UserSignInResponseDto> response = userController.signIn(dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("jwt-token", response.getBody().getToken());
        assertNotNull(response.getBody().getUser());
        verify(userService).signIn("test@example.com", "password123");
        verify(jwtService).generateToken("user123", "test@example.com");
    }

    @Test
    void signIn_Unauthorized() {
        // Arrange
        UserSignInDto dto = new UserSignInDto();
        dto.setEmail("test@example.com");
        dto.setPassword("wrongpassword");

        when(userService.signIn("test@example.com", "wrongpassword")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<UserSignInResponseDto> response = userController.signIn(dto);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void updateUser_Success() {
        // Arrange
        UserUpdateDto dto = new UserUpdateDto();
        dto.setId("user123");

        when(userService.updateUser(anyString(), any(User.class), any())).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<UserDto> response = userController.updateUser(dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(userService).updateUser(eq("user123"), any(User.class), any());
    }

    @Test
    void updateUser_MissingId() {
        // Arrange
        UserUpdateDto dto = new UserUpdateDto();
        dto.setId("");

        // Act
        ResponseEntity<UserDto> response = userController.updateUser(dto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(userService, never()).updateUser(anyString(), any(), any());
    }

    @Test
    void updateUser_NotFound() {
        // Arrange
        UserUpdateDto dto = new UserUpdateDto();
        dto.setId("user123");

        when(userService.updateUser(anyString(), any(User.class), any())).thenReturn(Optional.empty());

        // Act
        ResponseEntity<UserDto> response = userController.updateUser(dto);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getFollowers_Success() {
        // Arrange
        List<User> followers = Arrays.asList(testUser);
        when(userService.getFollowers("user123")).thenReturn(followers);

        // Act
        ResponseEntity<List<UserPreviewDto>> response = userController.getFollowers("user123");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(userService).getFollowers("user123");
    }

    @Test
    void getFollowing_Success() {
        // Arrange
        List<User> following = Arrays.asList(testUser);
        when(userService.getFollowing("user123")).thenReturn(following);

        // Act
        ResponseEntity<List<UserPreviewDto>> response = userController.getFollowing("user123");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(userService).getFollowing("user123");
    }

    @Test
    void followUser_Success() {
        // Arrange
        FollowUserDto dto = new FollowUserDto();
        dto.setTargetUserId("user456");

        when(authentication.getName()).thenReturn("user123");
        when(userService.followUser("user123", "user456")).thenReturn(true);

        // Act
        ResponseEntity<Void> response = userController.followUser(dto, authentication);

        // Assert
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(userService).followUser("user123", "user456");
    }

    @Test
    void followUser_Failed() {
        // Arrange
        FollowUserDto dto = new FollowUserDto();
        dto.setTargetUserId("user456");

        when(authentication.getName()).thenReturn("user123");
        when(userService.followUser("user123", "user456")).thenReturn(false);

        // Act
        ResponseEntity<Void> response = userController.followUser(dto, authentication);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void unfollowUser_Success() {
        // Arrange
        FollowUserDto dto = new FollowUserDto();
        dto.setTargetUserId("user456");

        when(authentication.getName()).thenReturn("user123");
        when(userService.unfollowUser("user123", "user456")).thenReturn(true);

        // Act
        ResponseEntity<Void> response = userController.unfollowUser(dto, authentication);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService).unfollowUser("user123", "user456");
    }

    @Test
    void unfollowUser_Failed() {
        // Arrange
        FollowUserDto dto = new FollowUserDto();
        dto.setTargetUserId("user456");

        when(authentication.getName()).thenReturn("user123");
        when(userService.unfollowUser("user123", "user456")).thenReturn(false);

        // Act
        ResponseEntity<Void> response = userController.unfollowUser(dto, authentication);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void deleteUser_Success() {
        // Arrange
        when(userService.deleteUser("user123")).thenReturn(true);

        // Act
        ResponseEntity<Void> response = userController.deleteUser("user123");

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService).deleteUser("user123");
    }

    @Test
    void deleteUser_NotFound() {
        // Arrange
        when(userService.deleteUser("user123")).thenReturn(false);

        // Act
        ResponseEntity<Void> response = userController.deleteUser("user123");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void searchUsersByUsername_Success() {
        // Arrange
        List<User> users = Arrays.asList(testUser);
        when(userService.searchUsersByUsername("test")).thenReturn(users);

        // Act
        ResponseEntity<List<UserPreviewDto>> response = userController.searchUsersByUsername("test");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(userService).searchUsersByUsername("test");
    }

    @Test
    void searchUsersByUsername_TooShort() {
        // Arrange & Act
        ResponseEntity<List<UserPreviewDto>> response = userController.searchUsersByUsername("a");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(userService, never()).searchUsersByUsername(anyString());
    }

    @Test
    void searchUsersByBooks_Success() {
        // Arrange
        SearchUsersByBooksDto dto = new SearchUsersByBooksDto();
        dto.setBookIds(Arrays.asList("book1", "book2"));

        when(authentication.getName()).thenReturn("user123");
        when(userService.searchUsersByBooks(anyList(), anyString()))
                .thenReturn(Arrays.asList(new UserPreviewDto()));

        // Act
        ResponseEntity<List<UserPreviewDto>> response = userController.searchUsersByBooks(dto, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(userService).searchUsersByBooks(dto.getBookIds(), "user123");
    }

    @Test
    void searchUsersByLocation_Success() {
        // Arrange
        SearchUsersByLocationDto dto = new SearchUsersByLocationDto();
        dto.setBottomLeftLatitude(-34.6);
        dto.setBottomLeftLongitude(-58.5);
        dto.setTopRightLatitude(-34.5);
        dto.setTopRightLongitude(-58.3);

        when(userService.searchUsersByLocation(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Arrays.asList(new UserPreviewDto()));

        // Act
        ResponseEntity<List<UserPreviewDto>> response = userController.searchUsersByLocation(dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(userService).searchUsersByLocation(-34.6, -58.5, -34.5, -58.3);
    }

    @Test
    void searchUsersByLocation_InvalidBounds() {
        // Arrange
        SearchUsersByLocationDto dto = new SearchUsersByLocationDto();
        dto.setBottomLeftLatitude(-34.5);
        dto.setBottomLeftLongitude(-58.3);
        dto.setTopRightLatitude(-34.6);
        dto.setTopRightLongitude(-58.5);

        // Act
        ResponseEntity<List<UserPreviewDto>> response = userController.searchUsersByLocation(dto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(userService, never()).searchUsersByLocation(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }
}

