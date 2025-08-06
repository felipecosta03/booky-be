package com.uade.bookybe.router;

import com.uade.bookybe.core.model.BookExchange;
import com.uade.bookybe.core.model.constant.ExchangeStatus;
import com.uade.bookybe.core.usecase.BookExchangeService;
import com.uade.bookybe.router.dto.exchange.*;
import com.uade.bookybe.router.mapper.BookExchangeDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Book Exchange", description = "APIs for book exchange management")
@RequestMapping("/exchanges")
public class BookExchangeController {

  private final BookExchangeService bookExchangeService;

  @Operation(
      summary = "Create a new book exchange",
      description = "Creates a new book exchange request between two users")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Exchange created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookExchangeDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content),
        @ApiResponse(
            responseCode = "409",
            description = "Cannot create exchange with yourself",
            content = @Content)
      })
  @PostMapping
  public ResponseEntity<BookExchangeDto> createExchange(
      @Parameter(description = "Exchange creation details", required = true) @Valid @RequestBody
          CreateBookExchangeDto createDto) {

    log.info(
        "Creating exchange. RequesterId: {}, OwnerId: {}",
        createDto.getRequesterId(),
        createDto.getOwnerId());

    Optional<BookExchange> exchange =
        bookExchangeService.createExchange(
            createDto.getRequesterId(),
            createDto.getOwnerId(),
            createDto.getOwnerBookIds(),
            createDto.getRequesterBookIds());

    if (exchange.isEmpty()) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    BookExchangeDto responseDto = BookExchangeDtoMapper.INSTANCE.toDto(exchange.get());
    return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
  }

  @Operation(
      summary = "Get all exchanges for a user",
      description = "Retrieves all exchanges where the user is either requester or owner")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Exchanges retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookExchangeDto.class)))
      })
  @GetMapping("/users/{userId}")
  public ResponseEntity<List<BookExchangeDto>> getUserExchanges(
      @Parameter(description = "User ID", required = true) @PathVariable String userId,
      @Parameter(description = "Filter by exchange status") @RequestParam(required = false)
          ExchangeStatus status) {

    log.info("Getting exchanges for user: {} with status: {}", userId, status);

    List<BookExchange> exchanges;
    if (status != null) {
      exchanges = bookExchangeService.getUserExchangesByStatus(userId, status);
    } else {
      exchanges = bookExchangeService.getUserExchanges(userId);
    }

    List<BookExchangeDto> responseDtos =
        exchanges.stream().map(BookExchangeDtoMapper.INSTANCE::toDto).collect(Collectors.toList());

    return ResponseEntity.ok(responseDtos);
  }

  @Operation(
      summary = "Get exchanges where user is requester",
      description = "Retrieves exchanges where the user initiated the exchange request")
  @GetMapping("/users/{userId}/as-requester")
  public ResponseEntity<List<BookExchangeDto>> getExchangesAsRequester(
      @Parameter(description = "User ID", required = true) @PathVariable String userId) {

    log.info("Getting exchanges where user {} is requester", userId);

    List<BookExchange> exchanges = bookExchangeService.getExchangesAsRequester(userId);
    List<BookExchangeDto> responseDtos =
        exchanges.stream().map(BookExchangeDtoMapper.INSTANCE::toDto).collect(Collectors.toList());

    return ResponseEntity.ok(responseDtos);
  }

  @Operation(
      summary = "Get exchanges where user is owner",
      description = "Retrieves exchanges where the user owns the requested books")
  @GetMapping("/users/{userId}/as-owner")
  public ResponseEntity<List<BookExchangeDto>> getExchangesAsOwner(
      @Parameter(description = "User ID", required = true) @PathVariable String userId) {

    log.info("Getting exchanges where user {} is owner", userId);

    List<BookExchange> exchanges = bookExchangeService.getExchangesAsOwner(userId);
    List<BookExchangeDto> responseDtos =
        exchanges.stream().map(BookExchangeDtoMapper.INSTANCE::toDto).collect(Collectors.toList());

    return ResponseEntity.ok(responseDtos);
  }

  @Operation(
      summary = "Get exchange by ID",
      description = "Retrieves a specific exchange by its ID")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Exchange found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookExchangeDto.class))),
        @ApiResponse(responseCode = "404", description = "Exchange not found", content = @Content)
      })
  @GetMapping("/{exchangeId}")
  public ResponseEntity<BookExchangeDto> getExchangeById(
      @Parameter(description = "Exchange ID", required = true) @PathVariable String exchangeId) {

    log.info("Getting exchange by ID: {}", exchangeId);

    Optional<BookExchange> exchange = bookExchangeService.getExchangeById(exchangeId);
    if (exchange.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    BookExchangeDto responseDto = BookExchangeDtoMapper.INSTANCE.toDto(exchange.get());
    return ResponseEntity.ok(responseDto);
  }

  @Operation(
      summary = "Update exchange status",
      description = "Updates the status of an exchange (accept, reject, complete, cancel)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookExchangeDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "User not authorized for this action",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Exchange not found", content = @Content)
      })
  @PutMapping("/{exchangeId}/status")
  public ResponseEntity<BookExchangeDto> updateExchangeStatus(
      @Parameter(description = "Exchange ID", required = true) @PathVariable String exchangeId,
      @Parameter(description = "User ID performing the action", required = true) @RequestParam
          String userId,
      @Parameter(description = "Status update details", required = true) @Valid @RequestBody
          UpdateExchangeStatusDto updateDto) {

    log.info(
        "Updating exchange {} status to {} by user {}", exchangeId, updateDto.getStatus(), userId);

    Optional<BookExchange> exchange =
        bookExchangeService.updateExchangeStatus(exchangeId, userId, updateDto.getStatus());

    if (exchange.isEmpty()) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    BookExchangeDto responseDto = BookExchangeDtoMapper.INSTANCE.toDto(exchange.get());
    return ResponseEntity.ok(responseDto);
  }

  @Operation(
      summary = "Create counter offer",
      description = "Creates a counter offer for an existing exchange")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Counter offer created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookExchangeDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "User not authorized for this action",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Exchange not found", content = @Content)
      })
  @PutMapping("/{exchangeId}/counter-offer")
  public ResponseEntity<BookExchangeDto> createCounterOffer(
      @Parameter(description = "Exchange ID", required = true) @PathVariable String exchangeId,
      @Parameter(description = "User ID creating the counter offer", required = true) @RequestParam
          String userId,
      @Parameter(description = "Counter offer details", required = true) @Valid @RequestBody
          CounterOfferDto counterOfferDto) {

    log.info("Creating counter offer for exchange: {} by user: {}", exchangeId, userId);

    Optional<BookExchange> exchange =
        bookExchangeService.createCounterOffer(
            exchangeId,
            userId,
            counterOfferDto.getOwnerBookIds(),
            counterOfferDto.getRequesterBookIds());

    if (exchange.isEmpty()) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    BookExchangeDto responseDto = BookExchangeDtoMapper.INSTANCE.toDto(exchange.get());
    return ResponseEntity.ok(responseDto);
  }

  @Operation(
      summary = "Get pending exchanges count",
      description = "Gets the number of pending exchanges for a user")
  @GetMapping("/users/{userId}/pending-count")
  public ResponseEntity<Long> getPendingExchangesCount(
      @Parameter(description = "User ID", required = true) @PathVariable String userId) {

    log.info("Getting pending exchanges count for user: {}", userId);

    long count = bookExchangeService.getPendingExchangesCount(userId);
    return ResponseEntity.ok(count);
  }
}
