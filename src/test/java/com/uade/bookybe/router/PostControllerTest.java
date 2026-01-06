package com.uade.bookybe.router;

import com.uade.bookybe.core.model.Post;
import com.uade.bookybe.core.usecase.CommentService;
import com.uade.bookybe.core.usecase.PostService;
import com.uade.bookybe.router.dto.post.*;
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
class PostControllerTest {

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PostController postController;

    private Post testPost;

    @BeforeEach
    void setUp() {
        testPost = Post.builder()
                .id("post123")
                .userId("user123")
                .body("Test post")
                .communityId("community123")
                .build();

        when(authentication.getName()).thenReturn("user123");
    }

    @Test
    void createPost_Success() {
        CreatePostDto dto = new CreatePostDto();
        dto.setBody("Test post");
        dto.setCommunityId("community123");

        when(postService.createPost(anyString(), anyString(), anyString(), any()))
                .thenReturn(Optional.of(testPost));

        ResponseEntity<PostDto> response = postController.createPost(dto, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(postService).createPost("user123", "Test post", "community123", null);
    }

    @Test
    void createPost_Failed() {
        CreatePostDto dto = new CreatePostDto();
        dto.setBody("Test post");

        when(postService.createPost(anyString(), anyString(), any(), any()))
                .thenReturn(Optional.empty());

        ResponseEntity<PostDto> response = postController.createPost(dto, authentication);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getPostById_Success() {
        when(postService.getPostById("post123")).thenReturn(Optional.of(testPost));
        when(commentService.countCommentsByPostId("post123")).thenReturn(5);
        when(postService.isPostLikedByUser("post123", "user123")).thenReturn(true);

        ResponseEntity<PostDto> response = postController.getPostById("post123", authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().getCommentsCount().intValue());
        assertTrue(response.getBody().getIsLikedByUser());
    }

    @Test
    void getPostById_NotFound() {
        when(postService.getPostById("post123")).thenReturn(Optional.empty());

        ResponseEntity<PostDto> response = postController.getPostById("post123", authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

