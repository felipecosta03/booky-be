package com.uade.bookybe.router;

import com.uade.bookybe.core.model.UserRate;
import com.uade.bookybe.core.usecase.UserRateService;
import com.uade.bookybe.router.dto.rate.CreateUserRateDto;
import com.uade.bookybe.router.dto.rate.UserRateDto;
import com.uade.bookybe.router.mapper.UserRateDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Rating", description = "APIs for managing exchange ratings")
@RequestMapping("/ratings")
public class UserRateController {

    private final UserRateService userRateService;

    @Operation(
            summary = "Create a rating for an exchange",
            description = "Creates a new rating for a completed exchange")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Rating created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserRateDto.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data or exchange cannot be rated",
                            content = @Content),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Exchange not found",
                            content = @Content)
            })
    @PostMapping("/exchanges/{exchangeId}")
    public ResponseEntity<UserRateDto> createRating(
            @Parameter(description = "Exchange ID", required = true) @PathVariable String exchangeId,
            @Parameter(description = "Rating details", required = true) @Valid @RequestBody CreateUserRateDto createDto) {

        // Get current user from security context
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            log.warn("No authenticated user found for rating creation");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Creating rating for exchange {} by user {} with rating {}", 
                exchangeId, currentUserId, createDto.getRating());

        Optional<UserRate> rating = userRateService.createRating(
                exchangeId, 
                currentUserId, 
                createDto.getRating(), 
                createDto.getComment());

        if (rating.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        UserRateDto responseDto = UserRateDtoMapper.INSTANCE.toDto(rating.get());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Operation(
            summary = "Get ratings for a user",
            description = "Retrieves all ratings received by a specific user (no authentication required)")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Ratings retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserRateDto.class)))
            })
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<UserRateDto>> getUserRatings(
            @Parameter(description = "User ID", required = true) @PathVariable String userId) {

        log.info("Getting ratings for user: {}", userId);

        List<UserRate> ratings = userRateService.getUserRatings(userId);
        List<UserRateDto> responseDtos = ratings.stream()
                .map(UserRateDtoMapper.INSTANCE::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDtos);
    }

    @Operation(
            summary = "Get ratings for an exchange",
            description = "Retrieves all ratings for a specific exchange")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Exchange ratings retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserRateDto.class)))
            })
    @GetMapping("/exchanges/{exchangeId}")
    public ResponseEntity<List<UserRateDto>> getExchangeRatings(
            @Parameter(description = "Exchange ID", required = true) @PathVariable String exchangeId) {

        log.info("Getting ratings for exchange: {}", exchangeId);

        List<UserRate> ratings = userRateService.getExchangeRatings(exchangeId);
        List<UserRateDto> responseDtos = ratings.stream()
                .map(UserRateDtoMapper.INSTANCE::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDtos);
    }

    @Operation(
            summary = "Check if user can rate exchange",
            description = "Checks if a user is eligible to rate a specific exchange")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Check completed successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Boolean.class)))
            })
    @GetMapping("/exchanges/{exchangeId}/can-rate")
    public ResponseEntity<Boolean> canUserRateExchange(
            @Parameter(description = "Exchange ID", required = true) @PathVariable String exchangeId,
            @Parameter(description = "User ID", required = true) @RequestParam String userId) {

        log.info("Checking if user {} can rate exchange {}", userId, exchangeId);

        boolean canRate = userRateService.canUserRateExchange(exchangeId, userId);
        return ResponseEntity.ok(canRate);
    }

    @Operation(
            summary = "Get user rating statistics",
            description = "Gets average rating and total count for a user")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Statistics retrieved successfully")
            })
    @GetMapping("/users/{userId}/stats")
    public ResponseEntity<UserRatingStatsDto> getUserRatingStats(
            @Parameter(description = "User ID", required = true) @PathVariable String userId) {

        log.info("Getting rating statistics for user: {}", userId);

        Double averageRating = userRateService.getUserAverageRating(userId);
        Long totalRatings = userRateService.getUserRatingCount(userId);

        UserRatingStatsDto stats = new UserRatingStatsDto(
                averageRating != null ? averageRating : 0.0,
                totalRatings != null ? totalRatings : 0L
        );

        return ResponseEntity.ok(stats);
    }

    /**
     * Helper method to get the current authenticated user ID
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getPrincipal().equals("anonymousUser")) {
            return (String) authentication.getPrincipal();
        }
        return null;
    }

    // Inner class for rating statistics
    public static class UserRatingStatsDto {
        private final Double averageRating;
        private final Long totalRatings;

        public UserRatingStatsDto(Double averageRating, Long totalRatings) {
            this.averageRating = averageRating;
            this.totalRatings = totalRatings;
        }

        public Double getAverageRating() {
            return averageRating;
        }

        public Long getTotalRatings() {
            return totalRatings;
        }
    }
}
