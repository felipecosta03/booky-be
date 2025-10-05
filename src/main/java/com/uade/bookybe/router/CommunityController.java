package com.uade.bookybe.router;

import com.uade.bookybe.core.model.Community;
import com.uade.bookybe.core.usecase.CommunityService;
import com.uade.bookybe.router.dto.community.CommunityDto;
import com.uade.bookybe.router.dto.community.CreateCommunityDto;
import com.uade.bookybe.router.mapper.CommunityDtoMapper;
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
@RequestMapping("/communities")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Communities", description = "API para gestión de comunidades literarias")
public class CommunityController {

  private final CommunityService communityService;

  @Operation(summary = "Crear nueva comunidad", description = "Crea una nueva comunidad literaria")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Comunidad creada exitosamente",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CommunityDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content)
      })
  @PostMapping
  public ResponseEntity<CommunityDto> createCommunity(
      @Parameter(description = "Datos de la comunidad", required = true) @RequestBody @Valid
          CreateCommunityDto createCommunityDto,
      Authentication authentication) {

    log.info(
        "Creating community: {} by user: {}",
        createCommunityDto.getName(),
        authentication.getName());

    String adminId = authentication.getName();

    return communityService
        .createCommunity(adminId, createCommunityDto.getName(), createCommunityDto.getDescription())
        .map(CommunityDtoMapper.INSTANCE::toDto)
        .map(
            communityDto -> {
              log.info("Community created successfully with ID: {}", communityDto.getId());
              return ResponseEntity.status(HttpStatus.CREATED).body(communityDto);
            })
        .orElseGet(
            () -> {
              log.warn("Failed to create community: {}", createCommunityDto.getName());
              return ResponseEntity.badRequest().build();
            });
  }

  @Operation(
      summary = "Obtener todas las comunidades",
      description = "Obtiene la lista de todas las comunidades disponibles")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Comunidades obtenidas exitosamente",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping
  public ResponseEntity<List<CommunityDto>> getAllCommunities() {
    log.info("Getting all communities");

    List<Community> communities = communityService.getAllCommunities();
    List<CommunityDto> communityDtos =
        communities.stream().map(CommunityDtoMapper.INSTANCE::toDto).collect(Collectors.toList());

    log.info("Retrieved {} communities", communityDtos.size());
    return ResponseEntity.ok(communityDtos);
  }

  @Operation(
      summary = "Obtener comunidad por ID",
      description = "Obtiene los detalles de una comunidad específica")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Comunidad encontrada",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "404",
            description = "Comunidad no encontrada",
            content = @Content)
      })
  @GetMapping("/{id}")
  public ResponseEntity<CommunityDto> getCommunityById(
      @Parameter(description = "ID de la comunidad", required = true) @PathVariable String id) {

    log.info("Getting community by ID: {}", id);

    return communityService
        .getCommunityById(id)
        .map(CommunityDtoMapper.INSTANCE::toDto)
        .map(ResponseEntity::ok)
        .orElseGet(
            () -> {
              log.warn("Community not found with ID: {}", id);
              return ResponseEntity.notFound().build();
            });
  }

  @Operation(
      summary = "Obtener comunidades del usuario",
      description = "Obtiene las comunidades donde el usuario es miembro")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Comunidades del usuario obtenidas exitosamente",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/user/{userId}")
  public ResponseEntity<List<CommunityDto>> getUserCommunities(
      @Parameter(description = "ID del usuario", required = true) @PathVariable String userId) {

    log.info("Getting communities for user: {}", userId);

    List<Community> communities = communityService.getUserCommunities(userId);
    List<CommunityDto> communityDtos =
        communities.stream().map(CommunityDtoMapper.INSTANCE::toDto).collect(Collectors.toList());

    log.info("Retrieved {} communities for user: {}", communityDtos.size(), userId);
    return ResponseEntity.ok(communityDtos);
  }

  @Operation(
      summary = "Unirse a una comunidad",
      description = "El usuario se une a una comunidad específica")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Unido a la comunidad exitosamente"),
        @ApiResponse(
            responseCode = "400",
            description = "Error al unirse a la comunidad",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Comunidad no encontrada",
            content = @Content)
      })
  @PostMapping("/{communityId}/join")
  public ResponseEntity<Void> joinCommunity(
      @Parameter(description = "ID de la comunidad", required = true) @PathVariable
          String communityId,
      Authentication authentication) {

    log.info("User {} joining community: {}", authentication.getName(), communityId);

    String userId = authentication.getName();

    if (communityService.joinCommunity(communityId, userId)) {
      log.info("User {} successfully joined community: {}", userId, communityId);
      return ResponseEntity.ok().build();
    } else {
      log.warn("Failed to join community: {} for user: {}", communityId, userId);
      return ResponseEntity.badRequest().build();
    }
  }

  @Operation(
      summary = "Salir de una comunidad",
      description = "El usuario sale de una comunidad específica")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Salió de la comunidad exitosamente"),
        @ApiResponse(
            responseCode = "400",
            description = "Error al salir de la comunidad",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Comunidad no encontrada",
            content = @Content)
      })
  @DeleteMapping("/{communityId}/leave")
  public ResponseEntity<Void> leaveCommunity(
      @Parameter(description = "ID de la comunidad", required = true) @PathVariable
          String communityId,
      Authentication authentication) {

    log.info("User {} leaving community: {}", authentication.getName(), communityId);

    String userId = authentication.getName();

    if (communityService.leaveCommunity(communityId, userId)) {
      log.info("User {} successfully left community: {}", userId, communityId);
      return ResponseEntity.ok().build();
    } else {
      log.warn("Failed to leave community: {} for user: {}", communityId, userId);
      return ResponseEntity.badRequest().build();
    }
  }

  @Operation(
      summary = "Buscar comunidades",
      description = "Busca comunidades por nombre o descripción")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Búsqueda completada exitosamente",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/search")
  public ResponseEntity<List<CommunityDto>> searchCommunities(
      @Parameter(description = "Término de búsqueda", required = true) @RequestParam String q,
      Authentication authentication) {

    log.info("Searching communities with query: {}", q);

    String userId = authentication.getName();

    List<Community> communities = communityService.searchCommunities(q);
    List<CommunityDto> communityDtos =
        communities.stream().map(CommunityDtoMapper.INSTANCE::toDto).collect(Collectors.toList());

    communityDtos.forEach(
        communityDto -> {
          communityDto.setJoinAvailable(
              !communityService.isUserMember(communityDto.getId(), userId));
        });

    log.info("Found {} communities for query: {}", communityDtos.size(), q);
    return ResponseEntity.ok(communityDtos);
  }

  @Operation(
      summary = "Eliminar comunidad",
      description =
          "Elimina una comunidad y todas sus relaciones (posts, comentarios, miembros). Solo el administrador puede eliminar la comunidad.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Comunidad eliminada exitosamente"),
        @ApiResponse(
            responseCode = "403",
            description = "No tienes permisos para eliminar esta comunidad"),
        @ApiResponse(responseCode = "404", description = "Comunidad no encontrada"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
      })
  @DeleteMapping("/{communityId}")
  public ResponseEntity<Void> deleteCommunity(
      @Parameter(description = "ID de la comunidad a eliminar") @PathVariable String communityId,
      Authentication authentication) {

    String userId = authentication.getName();
    log.info("User {} attempting to delete community: {}", userId, communityId);

    boolean deleted = communityService.deleteCommunity(communityId, userId);

    if (deleted) {
      log.info("Community {} successfully deleted by user {}", communityId, userId);
      return ResponseEntity.noContent().build();
    } else {
      log.warn(
          "Failed to delete community {} by user {} - either not found or no permissions",
          communityId,
          userId);
      return ResponseEntity.notFound().build();
    }
  }
}
