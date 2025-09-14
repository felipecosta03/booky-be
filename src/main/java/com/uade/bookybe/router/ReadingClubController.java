package com.uade.bookybe.router;

import com.uade.bookybe.core.model.ReadingClub;
import com.uade.bookybe.core.usecase.ReadingClubService;
import com.uade.bookybe.router.dto.readingclub.CreateReadingClubDto;
import com.uade.bookybe.router.dto.readingclub.ReadingClubDto;
import com.uade.bookybe.router.dto.readingclub.UpdateReadingClubMeetingDto;
import com.uade.bookybe.router.mapper.ReadingClubDtoMapper;
import com.uade.bookybe.router.mapper.ReadingClubDtoMapperWithNestedObjects;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reading-clubs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reading Clubs", description = "API para gestión de clubes de lectura")
public class ReadingClubController {

  private final ReadingClubService readingClubService;
  private final ReadingClubDtoMapperWithNestedObjects readingClubDtoMapperWithNestedObjects;

  @Operation(
      summary = "Obtener todos los clubes de lectura",
      description = "Obtiene la lista de todos los clubes de lectura disponibles")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Clubes obtenidos exitosamente",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping
  public ResponseEntity<List<ReadingClubDto>> getAllReadingClubs() {
    log.info("Getting all reading clubs");

    List<ReadingClub> readingClubs = readingClubService.getAllReadingClubs();
    List<ReadingClubDto> readingClubDtos =
        readingClubs.stream()
            .map(readingClubDtoMapperWithNestedObjects::toDtoWithNestedObjects)
            .collect(Collectors.toList());

    log.info("Retrieved {} reading clubs", readingClubDtos.size());
    return ResponseEntity.ok(readingClubDtos);
  }

  @Operation(
      summary = "Obtener club de lectura por ID",
      description = "Obtiene los detalles de un club de lectura específico")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Club encontrado exitosamente",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ReadingClubDto.class))),
        @ApiResponse(responseCode = "404", description = "Club no encontrado", content = @Content)
      })
  @GetMapping("/{id}")
  public ResponseEntity<ReadingClubDto> getReadingClubById(
      @Parameter(description = "ID del club de lectura", required = true) @PathVariable String id) {
    log.info("Getting reading club with ID: {}", id);

    return readingClubService
        .getReadingClubById(id)
            .map(readingClubDtoMapperWithNestedObjects::toDtoWithNestedObjects)
        .map(
            club -> {
              log.info("Reading club found: {}", club.getName());
              return ResponseEntity.ok(club);
            })
        .orElseGet(
            () -> {
              log.warn("Reading club not found with ID: {}", id);
              return ResponseEntity.notFound().build();
            });
  }

  @Operation(
      summary = "Obtener clubes de lectura por usuario",
      description = "Obtiene todos los clubes de lectura donde el usuario es miembro")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Clubes obtenidos exitosamente",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/user/{userId}")
  public ResponseEntity<List<ReadingClubDto>> getReadingClubsByUserId(
      @Parameter(description = "ID del usuario", required = true) @PathVariable String userId) {
    log.info("Getting reading clubs for user: {}", userId);

    List<ReadingClub> readingClubs = readingClubService.getReadingClubsByUserId(userId);
    List<ReadingClubDto> readingClubDtos =
        readingClubs.stream()
            .map(ReadingClubDtoMapper.INSTANCE::toDto)
            .collect(Collectors.toList());

    log.info("Retrieved {} reading clubs for user: {}", readingClubDtos.size(), userId);
    return ResponseEntity.ok(readingClubDtos);
  }

  @Operation(
      summary = "Obtener clubes de lectura por comunidad",
      description = "Obtiene todos los clubes de lectura de una comunidad específica")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Clubes obtenidos exitosamente",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/community/{communityId}")
  public ResponseEntity<List<ReadingClubDto>> getReadingClubsByCommunityId(
      @Parameter(description = "ID de la comunidad", required = true) @PathVariable
          String communityId) {
    log.info("Getting reading clubs for community: {}", communityId);

    List<ReadingClub> readingClubs = readingClubService.getReadingClubsByCommunityId(communityId);
    List<ReadingClubDto> readingClubDtos =
        readingClubs.stream()
            .map(readingClubDtoMapperWithNestedObjects::toDtoWithNestedObjects)
            .collect(Collectors.toList());

    String userId = SecurityContextHolder.getContext().getAuthentication().getName();
    readingClubDtos.forEach(
        readingClubDto ->
            readingClubDto.setJoinAvailable(
                !readingClubService.isUserMember(readingClubDto.getId(), userId)));

    log.info("Retrieved {} reading clubs for community: {}", readingClubDtos.size(), communityId);
    return ResponseEntity.ok(readingClubDtos);
  }

  @Operation(
      summary = "Crear nuevo club de lectura",
      description = "Crea un nuevo club de lectura en una comunidad")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Club creado exitosamente",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ReadingClubDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content)
      })
  @PostMapping
  public ResponseEntity<ReadingClubDto> createReadingClub(
      @Parameter(description = "Datos del club de lectura", required = true) @RequestBody @Valid
          CreateReadingClubDto createReadingClubDto,
      Authentication authentication) {

    log.info(
        "Creating reading club: {} by user: {}",
        createReadingClubDto.getName(),
        authentication.getName());

    String moderatorId = authentication.getName();

    return readingClubService
        .createReadingClub(
            moderatorId,
            createReadingClubDto.getName(),
            createReadingClubDto.getDescription(),
            createReadingClubDto.getCommunityId(),
            createReadingClubDto.getBookId(),
            createReadingClubDto.getNextMeeting())
        .map(ReadingClubDtoMapper.INSTANCE::toDto)
        .map(
            readingClubDto -> {
              log.info("Reading club created successfully with ID: {}", readingClubDto.getId());
              return ResponseEntity.status(HttpStatus.CREATED).body(readingClubDto);
            })
        .orElseGet(
            () -> {
              log.warn("Failed to create reading club: {}", createReadingClubDto.getName());
              return ResponseEntity.badRequest().build();
            });
  }

  @Operation(
      summary = "Unirse a un club de lectura",
      description = "El usuario se une a un club de lectura específico")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Unido al club exitosamente"),
        @ApiResponse(
            responseCode = "400",
            description = "Error al unirse al club",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Club no encontrado", content = @Content)
      })
  @PostMapping("/{clubId}/join")
  public ResponseEntity<Void> joinReadingClub(
      @Parameter(description = "ID del club de lectura", required = true) @PathVariable
          String clubId,
      Authentication authentication) {

    log.info("User {} joining reading club: {}", authentication.getName(), clubId);

    String userId = authentication.getName();

    if (readingClubService.joinReadingClub(clubId, userId)) {
      log.info("User {} successfully joined reading club: {}", userId, clubId);
      return ResponseEntity.ok().build();
    } else {
      log.warn("Failed to join reading club: {} for user: {}", clubId, userId);
      return ResponseEntity.badRequest().build();
    }
  }

  @Operation(
      summary = "Salir de un club de lectura",
      description = "El usuario abandona un club de lectura específico")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Salió del club exitosamente"),
        @ApiResponse(
            responseCode = "400",
            description = "Error al salir del club o usuario no es miembro",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Club no encontrado", content = @Content)
      })
  @PostMapping("/{clubId}/leave")
  public ResponseEntity<Void> leaveReadingClub(
      @Parameter(description = "ID del club de lectura", required = true) @PathVariable
          String clubId,
      Authentication authentication) {

    log.info("User {} leaving reading club: {}", authentication.getName(), clubId);

    String userId = authentication.getName();

    // NotFoundException and BadRequestException will be handled by GlobalExceptionHandler
    if (readingClubService.leaveReadingClub(clubId, userId)) {
      log.info("User {} successfully left reading club: {}", userId, clubId);
      return ResponseEntity.ok().build();
    } else {
      // This should never happen now since exceptions are thrown for error cases
      log.warn(
          "Unexpected false return from leaveReadingClub for user: {} and club: {}",
          userId,
          clubId);
      return ResponseEntity.badRequest().build();
    }
  }

  @Operation(
      summary = "Eliminar club de lectura",
      description = "Elimina un club de lectura específico (solo el moderador puede eliminarlo)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Club eliminado exitosamente"),
        @ApiResponse(
            responseCode = "400",
            description = "Error al eliminar el club o usuario no es moderador",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Club no encontrado", content = @Content)
      })
  @DeleteMapping("/{clubId}")
  public ResponseEntity<Void> deleteReadingClub(
      @Parameter(description = "ID del club de lectura", required = true) @PathVariable
          String clubId,
      Authentication authentication) {

    log.info("User {} attempting to delete reading club: {}", authentication.getName(), clubId);

    String userId = authentication.getName();

    // NotFoundException will be handled by GlobalExceptionHandler
    if (readingClubService.deleteReadingClub(clubId, userId)) {
      log.info("Reading club {} successfully deleted by user: {}", clubId, userId);
      return ResponseEntity.noContent().build();
    } else {
      log.warn(
          "Failed to delete reading club: {} by user: {} (user might not be moderator)",
          clubId,
          userId);
      return ResponseEntity.badRequest().build();
    }
  }

  @Operation(
      summary = "Buscar clubes de lectura",
      description = "Busca clubes de lectura por nombre o descripción")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Búsqueda realizada exitosamente",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/search")
  public ResponseEntity<List<ReadingClubDto>> searchReadingClubs(
      @Parameter(description = "Término de búsqueda", required = true) @RequestParam String q) {
    log.info("Searching reading clubs with query: {}", q);

    List<ReadingClub> readingClubs = readingClubService.searchReadingClubs(q);
    List<ReadingClubDto> readingClubDtos =
        readingClubs.stream()
            .map(ReadingClubDtoMapper.INSTANCE::toDto)
            .collect(Collectors.toList());

    log.info("Found {} reading clubs for query: {}", readingClubDtos.size(), q);
    return ResponseEntity.ok(readingClubDtos);
  }

  @Operation(
      summary = "Actualizar información de reunión del club de lectura",
      description = "Actualiza la fecha de la próxima reunión y el capítulo actual del club de lectura. Solo el moderador puede realizar esta acción.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Información de reunión actualizada exitosamente",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ReadingClubDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solo el moderador puede actualizar la reunión", content = @Content),
        @ApiResponse(responseCode = "404", description = "Club de lectura no encontrado", content = @Content)
      })
  @PutMapping("/{clubId}/meeting")
  public ResponseEntity<ReadingClubDto> updateMeeting(
      @Parameter(description = "ID del club de lectura", required = true) @PathVariable String clubId,
      @Parameter(description = "Información de la reunión a actualizar", required = true) @RequestBody @Valid
          UpdateReadingClubMeetingDto updateMeetingDto,
      Authentication authentication) {

    log.info(
        "Updating meeting for reading club: {} by user: {}",
        clubId,
        authentication.getName());

    String userId = authentication.getName();

    return readingClubService
        .updateMeeting(
            clubId,
            userId,
            updateMeetingDto.getNextMeeting(),
            updateMeetingDto.getCurrentChapter())
        .map(ReadingClubDtoMapper.INSTANCE::toDto)
        .map(
            readingClubDto -> {
              log.info("Meeting updated successfully for reading club: {}", clubId);
              return ResponseEntity.ok(readingClubDto);
            })
        .orElseGet(
            () -> {
              log.warn("Failed to update meeting for reading club: {} by user: {}", clubId, userId);
              return ResponseEntity.notFound().build();
            });
  }
}
