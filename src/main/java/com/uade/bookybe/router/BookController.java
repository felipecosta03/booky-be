package com.uade.bookybe.router;

import com.uade.bookybe.core.model.Book;
import com.uade.bookybe.core.model.UserBook;
import com.uade.bookybe.core.usecase.BookService;
import com.uade.bookybe.router.dto.book.*;
import com.uade.bookybe.router.mapper.BookDtoMapper;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Book Management", description = "APIs for book and library management")
@RequestMapping("/books")
public class BookController {

  private final BookService bookService;

  @Operation(
      summary = "Add book to user library",
      description = "Adds a book to user's library. If book doesn't exist, fetches from Google Books API")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Book added successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserBookDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
        @ApiResponse(responseCode = "404", description = "Book not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Book already in library", content = @Content)
      })
  @PostMapping("/users/{userId}/library")
  public ResponseEntity<UserBookDto> addBookToUserLibrary(
      @Parameter(description = "User ID", required = true) @PathVariable String userId,
      @Parameter(description = "Book data", required = true) @Valid @RequestBody AddBookToLibraryDto dto) {
    
    log.info("Adding book to library for user: {}", userId);
    
    return bookService.addBookToUserLibrary(userId, dto.getIsbn(), dto.getStatus())
        .map(BookDtoMapper.INSTANCE::toUserBookDto)
        .map(userBook -> {
          log.info("Book added successfully to user library: {}", userBook.getBook().getTitle());
          return ResponseEntity.ok(userBook);
        })
        .orElseGet(() -> {
          log.warn("Failed to add book to user library");
          return ResponseEntity.status(HttpStatus.CONFLICT).build();
        });
  }

  @Operation(
      summary = "Get user library",
      description = "Retrieves all books in user's library")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Library retrieved successfully",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/users/{userId}/library")
  public ResponseEntity<List<UserBookDto>> getUserLibrary(
      @Parameter(description = "User ID", required = true) @PathVariable String userId) {
    
    log.info("Getting library for user: {}", userId);
    
    List<UserBook> userBooks = bookService.getUserLibrary(userId);
    List<UserBookDto> userBookDtos = userBooks.stream()
        .map(BookDtoMapper.INSTANCE::toUserBookDto)
        .collect(Collectors.toList());
    
    return ResponseEntity.ok(userBookDtos);
  }

  @Operation(
      summary = "Get user favorite books",
      description = "Retrieves all favorite books of a user")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Favorite books retrieved successfully",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/users/{userId}/favorites")
  public ResponseEntity<List<UserBookDto>> getUserFavoriteBooks(
      @Parameter(description = "User ID", required = true) @PathVariable String userId) {
    
    log.info("Getting favorite books for user: {}", userId);
    
    List<UserBook> userBooks = bookService.getUserFavoriteBooks(userId);
    List<UserBookDto> userBookDtos = userBooks.stream()
        .map(BookDtoMapper.INSTANCE::toUserBookDto)
        .collect(Collectors.toList());
    
    return ResponseEntity.ok(userBookDtos);
  }

  @Operation(
      summary = "Get books for exchange",
      description = "Retrieves all books available for exchange")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Exchange books retrieved successfully",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/exchange")
  public ResponseEntity<List<UserBookDto>> getBooksForExchange() {
    
    log.info("Getting books available for exchange");
    
    List<UserBook> userBooks = bookService.getBooksForExchange();
    List<UserBookDto> userBookDtos = userBooks.stream()
        .map(BookDtoMapper.INSTANCE::toUserBookDto)
        .collect(Collectors.toList());
    
    return ResponseEntity.ok(userBookDtos);
  }

  @Operation(
      summary = "Update book status",
      description = "Updates the reading status of a user's book")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Book status updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserBookDto.class))),
        @ApiResponse(responseCode = "404", description = "Book not found in user library", content = @Content)
      })
  @PutMapping("/users/{userId}/books/{bookId}/status")
  public ResponseEntity<UserBookDto> updateBookStatus(
      @Parameter(description = "User ID", required = true) @PathVariable String userId,
      @Parameter(description = "Book ID", required = true) @PathVariable Long bookId,
      @Parameter(description = "Status update data", required = true) @Valid @RequestBody UpdateStatusDto dto) {
    
    log.info("Updating book status for user: {}, book: {}", userId, bookId);
    
    return bookService.updateBookStatus(userId, bookId, dto.getStatus())
        .map(BookDtoMapper.INSTANCE::toUserBookDto)
        .map(userBook -> {
          log.info("Book status updated successfully: {}", userBook.getStatus());
          return ResponseEntity.ok(userBook);
        })
        .orElseGet(() -> {
          log.warn("Book not found in user library");
          return ResponseEntity.notFound().build();
        });
  }

  @Operation(
      summary = "Update exchange preference",
      description = "Updates whether user wants to exchange a book")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Exchange preference updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserBookDto.class))),
        @ApiResponse(responseCode = "404", description = "Book not found in user library", content = @Content)
      })
  @PutMapping("/users/{userId}/books/{bookId}/exchange")
  public ResponseEntity<UserBookDto> updateExchangePreference(
      @Parameter(description = "User ID", required = true) @PathVariable String userId,
      @Parameter(description = "Book ID", required = true) @PathVariable Long bookId,
      @Parameter(description = "Exchange preference data", required = true) @Valid @RequestBody UpdateExchangePreferenceDto dto) {
    
    log.info("Updating exchange preference for user: {}, book: {}", userId, bookId);
    
    return bookService.updateBookExchangePreference(userId, bookId, dto.getWantsToExchange())
        .map(BookDtoMapper.INSTANCE::toUserBookDto)
        .map(userBook -> {
          log.info("Exchange preference updated successfully");
          return ResponseEntity.ok(userBook);
        })
        .orElseGet(() -> {
          log.warn("Book not found in user library");
          return ResponseEntity.notFound().build();
        });
  }

  @Operation(
      summary = "Toggle book favorite",
      description = "Toggles the favorite status of a user's book")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Favorite status toggled successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserBookDto.class))),
        @ApiResponse(responseCode = "404", description = "Book not found in user library", content = @Content)
      })
  @PutMapping("/users/{userId}/books/{bookId}/favorite")
  public ResponseEntity<UserBookDto> toggleBookFavorite(
      @Parameter(description = "User ID", required = true) @PathVariable String userId,
      @Parameter(description = "Book ID", required = true) @PathVariable Long bookId) {
    
    log.info("Toggling favorite for user: {}, book: {}", userId, bookId);
    
    return bookService.toggleBookFavorite(userId, bookId)
        .map(BookDtoMapper.INSTANCE::toUserBookDto)
        .map(userBook -> {
          log.info("Favorite status toggled successfully: {}", userBook.isFavorite());
          return ResponseEntity.ok(userBook);
        })
        .orElseGet(() -> {
          log.warn("Book not found in user library");
          return ResponseEntity.notFound().build();
        });
  }

  @Operation(
      summary = "Search books",
      description = "Searches books by query (searches in title, author, and categories)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Books found successfully",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/search")
  public ResponseEntity<List<BookDto>> searchBooks(
      @Parameter(description = "Search query", required = true) @RequestParam String q) {
    
    log.info("Searching books with query: {}", q);
    
    List<Book> books = bookService.searchBooks(q);
    List<BookDto> bookDtos = books.stream()
        .map(BookDtoMapper.INSTANCE::toDto)
        .collect(Collectors.toList());
    
    return ResponseEntity.ok(bookDtos);
  }

  @Operation(
      summary = "Get book by ISBN",
      description = "Gets a book by ISBN. If not in database, fetches from Google Books API and saves it")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Book found successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BookDto.class))),
        @ApiResponse(responseCode = "404", description = "Book not found", content = @Content)
      })
  @GetMapping("/isbn/{isbn}")
  public ResponseEntity<BookDto> getBookByIsbn(
      @Parameter(description = "ISBN", required = true) @PathVariable String isbn) {
    
    log.info("Getting book by ISBN: {}", isbn);
    
    return bookService.getBookByIsbn(isbn)
        .map(BookDtoMapper.INSTANCE::toDto)
        .map(book -> {
          log.info("Book found: {}", book.getTitle());
          return ResponseEntity.ok(book);
        })
        .orElseGet(() -> {
          log.warn("Book not found with ISBN: {}", isbn);
          return ResponseEntity.notFound().build();
        });
  }
} 