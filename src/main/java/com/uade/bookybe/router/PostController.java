package com.uade.bookybe.router;

import com.uade.bookybe.core.model.Post;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Posts", description = "API para gestión de publicaciones")
public class PostController {

  private final PostService postService;

  @Operation(
      summary = "Crear nueva publicación",
      description = "Crea una nueva publicación con texto e imagen opcional")
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
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<PostDto> createPost(
      @Parameter(description = "Datos de la publicación", required = true)
          @RequestPart("post")
          @Valid
          CreatePostDto createPostDto,
      @Parameter(description = "Imagen opcional de la publicación")
          @RequestPart(value = "image", required = false)
          MultipartFile image,
      Authentication authentication) {

    log.info("Creating post for user: {}", authentication.getName());

    String userId = authentication.getName();

    return postService
        .createPost(userId, createPostDto.getBody(), createPostDto.getCommunityId(), image)
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
      summary = "Obtener feed del usuario",
      description = "Obtiene las publicaciones de los usuarios seguidos por el usuario actual")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Feed obtenido exitosamente",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content)
      })
  @GetMapping("/feed")
  public ResponseEntity<List<PostDto>> getUserFeed(Authentication authentication) {

    log.info("Getting feed for user: {}", authentication.getName());

    String userId = authentication.getName();
    List<Post> posts = postService.getUserFeed(userId);

    List<PostDto> postDtos =
        posts.stream().map(PostDtoMapper.INSTANCE::toDto).collect(Collectors.toList());

    log.info("Retrieved {} posts for user feed", postDtos.size());
    return ResponseEntity.ok(postDtos);
  }

  @Operation(
      summary = "Obtener publicaciones de un usuario",
      description = "Obtiene todas las publicaciones de un usuario específico")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Publicaciones obtenidas exitosamente",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/user/{userId}")
  public ResponseEntity<List<PostDto>> getPostsByUserId(
      @Parameter(description = "ID del usuario", required = true) @PathVariable String userId) {

    log.info("Getting posts for user: {}", userId);

    List<Post> posts = postService.getPostsByUserId(userId);
    List<PostDto> postDtos =
        posts.stream().map(PostDtoMapper.INSTANCE::toDto).collect(Collectors.toList());

    log.info("Retrieved {} posts for user: {}", postDtos.size(), userId);
    return ResponseEntity.ok(postDtos);
  }

  @Operation(
      summary = "Obtener publicaciones de una comunidad",
      description = "Obtiene todas las publicaciones de una comunidad específica")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Publicaciones obtenidas exitosamente",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/community/{communityId}")
  public ResponseEntity<List<PostDto>> getPostsByCommunityId(
      @Parameter(description = "ID de la comunidad", required = true) @PathVariable
          String communityId) {

    log.info("Getting posts for community: {}", communityId);

    List<Post> posts = postService.getPostsByCommunityId(communityId);
    List<PostDto> postDtos =
        posts.stream().map(PostDtoMapper.INSTANCE::toDto).collect(Collectors.toList());

    log.info("Retrieved {} posts for community: {}", postDtos.size(), communityId);
    return ResponseEntity.ok(postDtos);
  }

  @Operation(
      summary = "Obtener publicaciones generales",
      description = "Obtiene todas las publicaciones que no pertenecen a ninguna comunidad")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Publicaciones obtenidas exitosamente",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/general")
  public ResponseEntity<List<PostDto>> getGeneralPosts() {

    log.info("Getting general posts");

    List<Post> posts = postService.getGeneralPosts();
    List<PostDto> postDtos =
        posts.stream().map(PostDtoMapper.INSTANCE::toDto).collect(Collectors.toList());

    log.info("Retrieved {} general posts", postDtos.size());
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
