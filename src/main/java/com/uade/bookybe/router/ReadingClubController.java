package com.uade.bookybe.router;

import com.uade.bookybe.core.model.ReadingClub;
import com.uade.bookybe.core.usecase.ReadingClubService;
import com.uade.bookybe.router.dto.readingclub.CreateReadingClubDto;
import com.uade.bookybe.router.dto.readingclub.ReadingClubDto;
import com.uade.bookybe.router.mapper.ReadingClubDtoMapper;
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
@RequestMapping("/reading-clubs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reading Clubs", description = "API para gestión de clubes de lectura")
public class ReadingClubController {

  private final ReadingClubService readingClubService;

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
    List<ReadingClubDto> readingClubDtos = readingClubs.stream()
        .map(ReadingClubDtoMapper.INSTANCE::toDto)
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

    return readingClubService.getReadingClubById(id)
        .map(ReadingClubDtoMapper.INSTANCE::toDto)
        .map(club -> {
          log.info("Reading club found: {}", club.getName());
          return ResponseEntity.ok(club);
        })
        .orElseGet(() -> {
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
    List<ReadingClubDto> readingClubDtos = readingClubs.stream()
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
      @Parameter(description = "ID de la comunidad", required = true) @PathVariable String communityId) {
    log.info("Getting reading clubs for community: {}", communityId);

    List<ReadingClub> readingClubs = readingClubService.getReadingClubsByCommunityId(communityId);
    List<ReadingClubDto> readingClubDtos = readingClubs.stream()
        .map(ReadingClubDtoMapper.INSTANCE::toDto)
        .collect(Collectors.toList());

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
      @Parameter(description = "Datos del club de lectura", required = true)
          @RequestBody @Valid CreateReadingClubDto createReadingClubDto,
      Authentication authentication) {

    log.info("Creating reading club: {} by user: {}", createReadingClubDto.getName(), authentication.getName());

    String moderatorId = authentication.getName();
    
    return readingClubService.createReadingClub(moderatorId, createReadingClubDto.getName(), 
            createReadingClubDto.getDescription(), createReadingClubDto.getBookId())
        .map(ReadingClubDtoMapper.INSTANCE::toDto)
        .map(readingClubDto -> {
          log.info("Reading club created successfully with ID: {}", readingClubDto.getId());
          return ResponseEntity.status(HttpStatus.CREATED).body(readingClubDto);
        })
        .orElseGet(() -> {
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
        @ApiResponse(responseCode = "400", description = "Error al unirse al club", content = @Content),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Club no encontrado", content = @Content)
      })
  @PostMapping("/{clubId}/join")
  public ResponseEntity<Void> joinReadingClub(
      @Parameter(description = "ID del club de lectura", required = true) @PathVariable String clubId,
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
    List<ReadingClubDto> readingClubDtos = readingClubs.stream()
        .map(ReadingClubDtoMapper.INSTANCE::toDto)
        .collect(Collectors.toList());

    log.info("Found {} reading clubs for query: {}", readingClubDtos.size(), q);
    return ResponseEntity.ok(readingClubDtos);
  }
} 