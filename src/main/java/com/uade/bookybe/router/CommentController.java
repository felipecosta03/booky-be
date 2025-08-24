package com.uade.bookybe.router;

import com.uade.bookybe.core.model.Comment;
import com.uade.bookybe.core.usecase.CommentService;
import com.uade.bookybe.router.dto.comment.CommentDto;
import com.uade.bookybe.router.dto.comment.CreateCommentDto;
import com.uade.bookybe.router.mapper.CommentDtoMapper;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Comments", description = "API para gestión de comentarios en posts")
public class CommentController {

  private final CommentService commentService;

  @Operation(
      summary = "Crear nuevo comentario",
      description = "Crea un nuevo comentario en un post específico")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Comentario creado exitosamente",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CommentDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Post no encontrado", content = @Content)
      })
  @PostMapping
  public ResponseEntity<CommentDto> createComment(
      @Parameter(description = "Datos del comentario", required = true) @RequestBody @Valid
          CreateCommentDto createCommentDto,
      Authentication authentication) {

    log.info(
        "Creating comment on post: {} by user: {}",
        createCommentDto.getPostId(),
        authentication.getName());

    String userId = authentication.getName();

    return commentService
        .createComment(userId, createCommentDto.getPostId(), createCommentDto.getBody())
        .map(CommentDtoMapper.INSTANCE::toDto)
        .map(
            commentDto -> {
              log.info("Comment created successfully with ID: {}", commentDto.getId());
              return ResponseEntity.status(HttpStatus.CREATED).body(commentDto);
            })
        .orElseGet(
            () -> {
              log.warn(
                  "Failed to create comment on post: {} for user: {}",
                  createCommentDto.getPostId(),
                  userId);
              return ResponseEntity.badRequest().build();
            });
  }

  @Operation(
      summary = "Obtener comentarios de un post",
      description = "Obtiene todos los comentarios de un post específico")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Comentarios obtenidos exitosamente",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/post/{postId}")
  public ResponseEntity<List<CommentDto>> getCommentsByPostId(
      @Parameter(description = "ID del post", required = true) @PathVariable String postId) {

    log.info("Getting comments for post: {}", postId);

    List<Comment> comments = commentService.getCommentsByPostId(postId);
    List<CommentDto> commentDtos =
        comments.stream().map(CommentDtoMapper.INSTANCE::toDto).collect(Collectors.toList());

    log.info("Retrieved {} comments for post: {}", commentDtos.size(), postId);
    return ResponseEntity.ok(commentDtos);
  }

  @Operation(
      summary = "Obtener comentario por ID",
      description = "Obtiene un comentario específico por su ID")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Comentario encontrado",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "404",
            description = "Comentario no encontrado",
            content = @Content)
      })
  @GetMapping("/{id}")
  public ResponseEntity<CommentDto> getCommentById(
      @Parameter(description = "ID del comentario", required = true) @PathVariable String id) {

    log.info("Getting comment by ID: {}", id);

    return commentService
        .getCommentById(id)
        .map(CommentDtoMapper.INSTANCE::toDto)
        .map(ResponseEntity::ok)
        .orElseGet(
            () -> {
              log.warn("Comment not found with ID: {}", id);
              return ResponseEntity.notFound().build();
            });
  }

  @Operation(
      summary = "Obtener comentarios del usuario",
      description = "Obtiene todos los comentarios realizados por un usuario")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Comentarios del usuario obtenidos exitosamente",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/user/{userId}")
  public ResponseEntity<List<CommentDto>> getCommentsByUserId(
      @Parameter(description = "ID del usuario", required = true) @PathVariable String userId) {

    log.info("Getting comments for user: {}", userId);

    List<Comment> comments = commentService.getCommentsByUserId(userId);
    List<CommentDto> commentDtos =
        comments.stream().map(CommentDtoMapper.INSTANCE::toDto).collect(Collectors.toList());

    log.info("Retrieved {} comments for user: {}", commentDtos.size(), userId);
    return ResponseEntity.ok(commentDtos);
  }

  @Operation(
      summary = "Eliminar comentario",
      description = "Elimina un comentario específico (solo el autor puede eliminarlo)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Comentario eliminado exitosamente"),
        @ApiResponse(
            responseCode = "403",
            description = "No tienes permisos para eliminar este comentario",
            content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Comentario no encontrado",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content)
      })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteComment(
      @Parameter(description = "ID del comentario", required = true) @PathVariable String id,
      Authentication authentication) {

    log.info("Deleting comment: {} by user: {}", id, authentication.getName());

    String userId = authentication.getName();

    if (commentService.deleteComment(id, userId)) {
      log.info("Comment {} deleted successfully by user: {}", id, userId);
      return ResponseEntity.ok().build();
    } else {
      log.warn("Failed to delete comment: {} by user: {}", id, userId);
      return ResponseEntity.badRequest().build();
    }
  }
}
