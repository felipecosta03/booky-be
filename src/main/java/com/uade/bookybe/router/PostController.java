package com.uade.bookybe.router;

import com.uade.bookybe.core.model.Post;
import com.uade.bookybe.core.usecase.CommentService;
import com.uade.bookybe.core.usecase.PostService;
import com.uade.bookybe.router.dto.post.CreatePostDto;
import com.uade.bookybe.router.dto.post.PostDto;
import com.uade.bookybe.router.mapper.PostDtoMapper;
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
@RequestMapping("/posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Posts", description = "API para gestión de publicaciones")
public class PostController {

  private final CommentService commentService;
  private final PostService postService;

  @Operation(
      summary = "Crear nueva publicación",
      description = "Crea una nueva publicación con texto e imagen opcional en base64")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Publicación creada exitosamente",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PostDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content)
      })
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<PostDto> createPost(
      @Parameter(
              description = "Datos de la publicación con imagen opcional en base64",
              required = true)
          @RequestBody
          @Valid
          CreatePostDto createPostDto,
      Authentication authentication) {

    log.info("Creating post for user: {}", authentication.getName());

    String userId = authentication.getName();

    return postService
        .createPost(
            userId,
            createPostDto.getBody(),
            createPostDto.getCommunityId(),
            createPostDto.getImage())
        .map(PostDtoMapper.INSTANCE::toDto)
        .map(
            postDto -> {
              log.info("Post created successfully with ID: {}", postDto.getId());
              return ResponseEntity.status(HttpStatus.CREATED).body(postDto);
            })
        .orElseGet(
            () -> {
              log.warn("Failed to create post for user: {}", userId);
              return ResponseEntity.badRequest().build();
            });
  }

  @Operation(
      summary = "Obtener publicación por ID",
      description = "Obtiene una publicación específica por su ID")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Publicación encontrada",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PostDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Publicación no encontrada",
            content = @Content)
      })
  @GetMapping("/{postId}")
  public ResponseEntity<PostDto> getPostById(
      @Parameter(description = "ID de la publicación", required = true) @PathVariable
          String postId) {

    log.info("Getting post by ID: {}", postId);

    return postService
        .getPostById(postId)
        .map(PostDtoMapper.INSTANCE::toDto)
        .map(ResponseEntity::ok)
        .orElseGet(
            () -> {
              log.warn("Post not found with ID: {}", postId);
              return ResponseEntity.notFound().build();
            });
  }

  @Operation(
      summary = "Get posts with filters",
      description = "Gets posts with optional filters: type (feed/general), userId, or communityId")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Posts retrieved successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized (for feed type)",
            content = @Content)
      })
  @GetMapping
  public ResponseEntity<List<PostDto>> getPosts(
      @Parameter(description = "Type of posts: 'feed' or 'general'") @RequestParam(required = false)
          String type,
      @Parameter(description = "Filter by user ID") @RequestParam(required = false) String userId,
      @Parameter(description = "Filter by community ID") @RequestParam(required = false)
          String communityId,
      Authentication authentication) {

    String requestingUserId = authentication != null ? authentication.getName() : null;

    log.info(
        "Getting posts with filters - type: {}, userId: {}, communityId: {}, requestingUserId: {}",
        type,
        userId,
        communityId,
        requestingUserId);

    List<Post> posts = postService.getPostsFiltered(type, userId, communityId, requestingUserId);
    List<PostDto> postDtos =
        posts.stream().map(PostDtoMapper.INSTANCE::toDto).collect(Collectors.toList());

    postDtos.forEach(
        postDto -> {
          postDto.setCommentsCount(commentService.countCommentsByPostId(postDto.getId()));
        });
    log.info("Retrieved {} posts with filters", postDtos.size());
    return ResponseEntity.ok(postDtos);
  }

  @Operation(
      summary = "Actualizar publicación",
      description = "Actualiza el contenido de una publicación existente")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Publicación actualizada exitosamente",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PostDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Publicación no encontrada",
            content = @Content)
      })
  @PutMapping("/{postId}")
  public ResponseEntity<PostDto> updatePost(
      @Parameter(description = "ID de la publicación", required = true) @PathVariable String postId,
      @Parameter(description = "Nuevo contenido de la publicación", required = true)
          @RequestBody
          @Valid
          CreatePostDto updatePostDto,
      Authentication authentication) {

    log.info("Updating post: {} by user: {}", postId, authentication.getName());

    String userId = authentication.getName();

    return postService
        .updatePost(postId, userId, updatePostDto.getBody())
        .map(PostDtoMapper.INSTANCE::toDto)
        .map(
            postDto -> {
              log.info("Post updated successfully: {}", postId);
              return ResponseEntity.ok(postDto);
            })
        .orElseGet(
            () -> {
              log.warn("Failed to update post: {} by user: {}", postId, userId);
              return ResponseEntity.notFound().build();
            });
  }

  @Operation(summary = "Eliminar publicación", description = "Elimina una publicación existente")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Publicación eliminada exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Publicación no encontrada",
            content = @Content)
      })
  @DeleteMapping("/{postId}")
  public ResponseEntity<Void> deletePost(
      @Parameter(description = "ID de la publicación", required = true) @PathVariable String postId,
      Authentication authentication) {

    log.info("Deleting post: {} by user: {}", postId, authentication.getName());

    String userId = authentication.getName();

    if (postService.deletePost(postId, userId)) {
      log.info("Post deleted successfully: {}", postId);
      return ResponseEntity.noContent().build();
    } else {
      log.warn("Failed to delete post: {} by user: {}", postId, userId);
      return ResponseEntity.notFound().build();
    }
  }
}
