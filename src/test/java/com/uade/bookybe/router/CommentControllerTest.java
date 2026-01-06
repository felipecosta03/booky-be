package com.uade.bookybe.router;

import com.uade.bookybe.core.model.Comment;
import com.uade.bookybe.core.usecase.CommentService;
import com.uade.bookybe.router.dto.comment.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CommentController commentController;

    private Comment testComment;

    @BeforeEach
    void setUp() {
        testComment = Comment.builder()
                .id("comment123")
                .postId("post123")
                .userId("user123")
                .body("Test comment")
                .build();

        when(authentication.getName()).thenReturn("user123");
    }

    @Test
    void createComment_Success() {
        CreateCommentDto dto = new CreateCommentDto();
        dto.setPostId("post123");
        dto.setBody("Test comment");

        when(commentService.createComment("user123", "post123", "Test comment"))
                .thenReturn(Optional.of(testComment));

        ResponseEntity<CommentDto> response = commentController.createComment(dto, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void createComment_BadRequest() {
        CreateCommentDto dto = new CreateCommentDto();
        dto.setPostId("post123");
        dto.setBody("Test");

        when(commentService.createComment(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        ResponseEntity<CommentDto> response = commentController.createComment(dto, authentication);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getCommentsByPostId_Success() {
        when(commentService.getCommentsByPostId("post123")).thenReturn(Arrays.asList(testComment));

        ResponseEntity<List<CommentDto>> response = commentController.getCommentsByPostId("post123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getCommentById_Success() {
        when(commentService.getCommentById("comment123")).thenReturn(Optional.of(testComment));

        ResponseEntity<CommentDto> response = commentController.getCommentById("comment123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getCommentById_NotFound() {
        when(commentService.getCommentById("comment123")).thenReturn(Optional.empty());

        ResponseEntity<CommentDto> response = commentController.getCommentById("comment123");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getCommentsByUserId_Success() {
        when(commentService.getCommentsByUserId("user123")).thenReturn(Arrays.asList(testComment));

        ResponseEntity<List<CommentDto>> response = commentController.getCommentsByUserId("user123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteComment_Success() {
        when(commentService.deleteComment("comment123", "user123")).thenReturn(true);

        ResponseEntity<Void> response = commentController.deleteComment("comment123", authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteComment_Failed() {
        when(commentService.deleteComment("comment123", "user123")).thenReturn(false);

        ResponseEntity<Void> response = commentController.deleteComment("comment123", authentication);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}

