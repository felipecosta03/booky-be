package com.uade.bookybe.router;

import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.model.constant.GamificationActivity;
import com.uade.bookybe.core.usecase.GamificationService;
import com.uade.bookybe.router.dto.gamification.*;
import com.uade.bookybe.router.mapper.GamificationDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gamification")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gamification", description = "API para el sistema de gamificación")
public class GamificationController {

  private final GamificationService gamificationService;

  @Operation(summary = "Obtener perfil de gamificación del usuario")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Perfil obtenido exitosamente"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
      })
  @GetMapping("/profile/{userId}")
  public ResponseEntity<GamificationProfileDto> getUserProfile(
      @Parameter(description = "ID del usuario") @PathVariable String userId) {
    log.info("Getting gamification profile for user: {}", userId);

    return gamificationService
        .getUserProfile(userId)
        .map(
            profile -> {
              GamificationProfileDto dto = GamificationDtoMapper.INSTANCE.toDto(profile);
              return ResponseEntity.ok(dto);
            })
        .orElseThrow(
            () -> new NotFoundException("Gamification profile not found for user: " + userId));
  }

  @Operation(summary = "Inicializar perfil de gamificación (Uso interno - normalmente automático)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Perfil inicializado exitosamente"),
        @ApiResponse(responseCode = "200", description = "El perfil ya existía")
      })
  @PostMapping("/profile/{userId}/initialize")
  public ResponseEntity<GamificationProfileDto> initializeUserProfile(
      @Parameter(description = "ID del usuario") @PathVariable String userId) {
    log.info("Manual initialization of gamification profile for user: {}", userId);

    return gamificationService
        .initializeUserProfile(userId)
        .map(
            profile -> {
              GamificationProfileDto dto = GamificationDtoMapper.INSTANCE.toDto(profile);
              return ResponseEntity.ok(dto); // 200 en lugar de 201, ya que puede ya existir
            })
        .orElseThrow(() -> new RuntimeException("Failed to initialize gamification profile"));
  }



  @Operation(summary = "Obtener todos los logros disponibles")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Logros obtenidos exitosamente")})
  @GetMapping("/achievements")
  public ResponseEntity<List<AchievementDto>> getAllAchievements() {
    log.info("Getting all achievements");

    List<AchievementDto> achievements =
        gamificationService.getAllAchievements().stream()
            .map(GamificationDtoMapper.INSTANCE::toDto)
            .collect(Collectors.toList());

    return ResponseEntity.ok(achievements);
  }

  @Operation(summary = "Obtener logros del usuario")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Logros del usuario obtenidos exitosamente")
      })
  @GetMapping("/achievements/{userId}")
  public ResponseEntity<List<UserAchievementDto>> getUserAchievements(
      @Parameter(description = "ID del usuario") @PathVariable String userId) {
    log.info("Getting achievements for user: {}", userId);

    List<UserAchievementDto> achievements =
        gamificationService.getUserAchievements(userId).stream()
            .map(GamificationDtoMapper.INSTANCE::toDto)
            .collect(Collectors.toList());

    return ResponseEntity.ok(achievements);
  }

  @Operation(summary = "Obtener logros no notificados del usuario")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Logros no notificados obtenidos exitosamente")
      })
  @GetMapping("/achievements/{userId}/unnotified")
  public ResponseEntity<List<UserAchievementDto>> getUnnotifiedAchievements(
      @Parameter(description = "ID del usuario") @PathVariable String userId) {
    log.info("Getting unnotified achievements for user: {}", userId);

    List<UserAchievementDto> achievements =
        gamificationService.getUnnotifiedAchievements(userId).stream()
            .map(GamificationDtoMapper.INSTANCE::toDto)
            .collect(Collectors.toList());

    return ResponseEntity.ok(achievements);
  }

  @Operation(summary = "Marcar logros como notificados")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Logros marcados como notificados exitosamente")
      })
  @PutMapping("/achievements/{userId}/mark-notified")
  public ResponseEntity<Void> markAchievementsAsNotified(
      @Parameter(description = "ID del usuario") @PathVariable String userId,
      @RequestBody List<String> achievementIds) {
    log.info("Marking achievements as notified for user: {}", userId);

    gamificationService.markAchievementsAsNotified(userId, achievementIds);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Obtener todos los niveles de usuario")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Niveles obtenidos exitosamente")})
  @GetMapping("/levels")
  public ResponseEntity<List<UserLevelDto>> getAllUserLevels() {
    log.info("Getting all user levels");

    List<UserLevelDto> levels =
        gamificationService.getAllUserLevels().stream()
            .map(GamificationDtoMapper.INSTANCE::toDto)
            .collect(Collectors.toList());

    return ResponseEntity.ok(levels);
  }



  @Operation(summary = "Verificar y otorgar logros pendientes")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Verificación completada, nuevos logros otorgados si aplicaba")
      })
  @PostMapping("/achievements/{userId}/check")
  public ResponseEntity<List<UserAchievementDto>> checkAndAwardAchievements(
      @Parameter(description = "ID del usuario") @PathVariable String userId) {
    log.info("Checking and awarding achievements for user: {}", userId);

    List<UserAchievementDto> newAchievements =
        gamificationService.checkAndAwardAchievements(userId).stream()
            .map(GamificationDtoMapper.INSTANCE::toDto)
            .collect(Collectors.toList());

    return ResponseEntity.ok(newAchievements);
  }

  @Operation(summary = "Obtener todas las actividades de gamificación y sus puntos")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Actividades obtenidas exitosamente")
      })
  @GetMapping("/activities")
  public ResponseEntity<List<GamificationActivityDto>> getAllGamificationActivities() {
    log.info("Getting all gamification activities");

    List<GamificationActivityDto> activities =
        Arrays.stream(GamificationActivity.values())
            .map(
                activity ->
                    GamificationActivityDto.builder()
                        .name(activity.name())
                        .points(activity.getPoints())
                        .description(activity.getDescription())
                        .build())
            .collect(Collectors.toList());

    return ResponseEntity.ok(activities);
  }

  @Operation(summary = "Eliminar datos de gamificación de un usuario (Admin)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Datos de gamificación eliminados exitosamente"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
      })
  @DeleteMapping("/profile/{userId}/cleanup")
  public ResponseEntity<String> deleteUserGamificationData(
      @Parameter(description = "ID del usuario") @PathVariable String userId) {
    log.info("Admin cleanup of gamification data for user: {}", userId);

    boolean deleted = gamificationService.deleteUserGamificationData(userId);

    if (deleted) {
      return ResponseEntity.ok("Gamification data deleted successfully for user: " + userId);
    } else {
      return ResponseEntity.status(500)
          .body("Failed to delete gamification data for user: " + userId);
    }
  }
}
