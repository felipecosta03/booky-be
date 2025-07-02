package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.model.Book;
import com.uade.bookybe.core.model.UserBook;
import com.uade.bookybe.core.model.constant.BookStatus;
import com.uade.bookybe.core.port.GoogleBooksPort;
import com.uade.bookybe.core.usecase.BookService;
import com.uade.bookybe.infraestructure.entity.BookEntity;
import com.uade.bookybe.infraestructure.entity.UserBookEntity;
import com.uade.bookybe.infraestructure.mapper.BookEntityMapper;
import com.uade.bookybe.infraestructure.mapper.UserBookEntityMapper;
import com.uade.bookybe.infraestructure.repository.BookRepository;
import com.uade.bookybe.infraestructure.repository.UserBookRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookServiceImpl implements BookService {

  private final BookRepository bookRepository;
  private final UserBookRepository userBookRepository;
  private final GoogleBooksPort googleBooksPort;

  @Override
  public Optional<UserBook> addBookToUserLibrary(String userId, String isbn, BookStatus status) {
    log.info("Adding book to user library. UserId: {}, ISBN: {}, Status: {}", userId, isbn, status);

    // Get or create the book using existing method
    Book book = getBookByIsbn(isbn).orElseThrow(() -> new NotFoundException("Book not found with ISBN: " + isbn));

    String bookId = book.getId();

    // Check if user already has this book in their library
    if (userBookRepository.existsByUserIdAndBookId(userId, bookId)) {
      log.warn("User {} already has book with ISBN {}", userId, isbn);
      return Optional.empty();
    }

    // Generate unique ID for user_book
    String userBookId = "user-book-" + java.util.UUID.randomUUID().toString().substring(0, 8);

    // Add book to user's library
    UserBookEntity userBookEntity = UserBookEntity.builder()
        .id(userBookId)
        .userId(userId)
        .bookId(bookId)
        .status(status)
        .favorite(false)
        .wantsToExchange(false)
        .build();

    userBookEntity = userBookRepository.save(userBookEntity);

    // Reload with book information
    Optional<UserBookEntity> savedWithBook = userBookRepository.findByUserIdAndBookIdWithBook(userId, bookId);
    if (savedWithBook.isEmpty()) {
      log.error("Failed to reload saved user book with book information");
      return Optional.empty();
    }

    UserBook userBook = UserBookEntityMapper.INSTANCE.toModel(savedWithBook.get());
    log.info("Successfully added book to user library: {}", userBook.getId());
    return Optional.of(userBook);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Book> searchBooks(String query) {
    log.info("Searching books with query: {}", query);

    List<BookEntity> bookEntities = bookRepository.searchBooks(query);

    return bookEntities.stream()
        .map(BookEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Book> getBookByIsbn(String isbn) {
    log.info("Getting book by ISBN: {}", isbn);

    // First try to find in database
    Optional<BookEntity> existingBook = bookRepository.findByIsbn(isbn);
    if (existingBook.isPresent()) {
      log.info("Book found in database: {}", existingBook.get().getTitle());
      return Optional.of(BookEntityMapper.INSTANCE.toModel(existingBook.get()));
    }

    // If not found, fetch from Google Books API
    log.info("Book not found in database, fetching from Google Books API");
    Optional<Book> googleBook = googleBooksPort.getBookByIsbn(isbn);

    if (googleBook.isEmpty()) {
      log.warn("Book with ISBN {} not found in Google Books API", isbn);
      return Optional.empty();
    }

    // Before saving, check if book with Google Books ISBN already exists
    String googleBookIsbn = googleBook.get().getIsbn();
    if (googleBookIsbn != null && !googleBookIsbn.equals(isbn)) {
      log.info("Checking if book with Google Books ISBN {} already exists", googleBookIsbn);
      Optional<BookEntity> existingGoogleBook = bookRepository.findByIsbn(googleBookIsbn);
      if (existingGoogleBook.isPresent()) {
        log.info("Found existing book with Google Books ISBN: {}", existingGoogleBook.get().getTitle());
        return Optional.of(BookEntityMapper.INSTANCE.toModel(existingGoogleBook.get()));
      }
    }

    try {
      // Save book to database
      BookEntity bookEntity = BookEntityMapper.INSTANCE.toEntity(googleBook.get());
      
      // Generate unique ID for the book
      String bookId = "book-" + java.util.UUID.randomUUID().toString().substring(0, 8);
      bookEntity.setId(bookId);
      
      bookEntity = bookRepository.save(bookEntity);

      Book savedBook = BookEntityMapper.INSTANCE.toModel(bookEntity);
      log.info("Book fetched from Google Books API and saved: {}", savedBook.getTitle());
      return Optional.of(savedBook);
      
    } catch (Exception e) {
      // Handle ISBN conflict - try to find by the Google Books returned ISBN
      if (e.getMessage() != null && e.getMessage().contains("duplicate key value violates unique constraint")) {
        log.warn("ISBN conflict detected, searching for existing book with Google Books ISBN: {}", googleBook.get().getIsbn());
        
        // Try to find with Google Books ISBN
        Optional<BookEntity> conflictBook = bookRepository.findByIsbn(googleBook.get().getIsbn());
        if (conflictBook.isPresent()) {
          log.info("Found existing book with conflicting ISBN: {}", conflictBook.get().getTitle());
          return Optional.of(BookEntityMapper.INSTANCE.toModel(conflictBook.get()));
        }
        
        // Also try with original ISBN in case of format differences
        Optional<BookEntity> originalBook = bookRepository.findByIsbn(isbn);
        if (originalBook.isPresent()) {
          log.info("Found existing book with original ISBN: {}", originalBook.get().getTitle());
          return Optional.of(BookEntityMapper.INSTANCE.toModel(originalBook.get()));
        }
        
        // If still not found, log and return empty instead of throwing
        log.error("Book exists (ISBN conflict) but could not be retrieved: {}", e.getMessage());
        return Optional.empty();
      }
      log.error("Error saving book from Google Books API: {}", e.getMessage());
      return Optional.empty(); // Changed from throw e; to prevent 500 errors
    }
  }

  @Override
  public Optional<UserBook> updateBookStatus(String userId, String bookId, BookStatus status) {
    log.info("Updating book status. UserId: {}, BookId: {}, Status: {}", userId, bookId, status);

    Optional<UserBookEntity> userBookEntity = userBookRepository.findByUserIdAndBookId(userId, bookId);

    if (userBookEntity.isEmpty()) {
      log.warn("User book not found. UserId: {}, BookId: {}", userId, bookId);
      return Optional.empty();
    }

    UserBookEntity entity = userBookEntity.get();
    entity.setStatus(status);
    entity = userBookRepository.save(entity);

    UserBook userBook = UserBookEntityMapper.INSTANCE.toModel(entity);
    log.info("Successfully updated book status: {}", userBook.getId());
    return Optional.of(userBook);
  }

  @Override
  public Optional<UserBook> updateBookExchangePreference(String userId, String bookId, boolean wantsToExchange) {
    log.info("Updating book exchange preference. UserId: {}, BookId: {}, WantsToExchange: {}", userId, bookId, wantsToExchange);

    Optional<UserBookEntity> userBookEntity = userBookRepository.findByUserIdAndBookId(userId, bookId);

    if (userBookEntity.isEmpty()) {
      log.warn("User book not found. UserId: {}, BookId: {}", userId, bookId);
      return Optional.empty();
    }

    UserBookEntity entity = userBookEntity.get();
    entity.setWantsToExchange(wantsToExchange);
    entity = userBookRepository.save(entity);

    UserBook userBook = UserBookEntityMapper.INSTANCE.toModel(entity);
    log.info("Successfully updated book exchange preference: {}", userBook.getId());
    return Optional.of(userBook);
  }

  @Override
  public Optional<UserBook> toggleBookFavorite(String userId, String bookId) {
    log.info("Toggling book favorite. UserId: {}, BookId: {}", userId, bookId);

    Optional<UserBookEntity> userBookEntity = userBookRepository.findByUserIdAndBookId(userId, bookId);

    if (userBookEntity.isEmpty()) {
      log.warn("User book not found. UserId: {}, BookId: {}", userId, bookId);
      return Optional.empty();
    }

    UserBookEntity entity = userBookEntity.get();
    entity.setFavorite(!entity.isFavorite());
    entity = userBookRepository.save(entity);

    UserBook userBook = UserBookEntityMapper.INSTANCE.toModel(entity);
    log.info("Successfully toggled book favorite. New state: {}", userBook.isFavorite());
    return Optional.of(userBook);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserBook> getUserLibrary(String userId) {
    log.info("Getting user library for userId: {}", userId);

    List<UserBookEntity> userBookEntities = userBookRepository.findByUserIdWithBook(userId);

    return userBookEntities.stream()
        .map(UserBookEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserBook> getUserFavoriteBooks(String userId) {
    log.info("Getting user favorite books for userId: {}", userId);

    List<UserBookEntity> userBookEntities = userBookRepository.findByUserIdAndIsFavoriteTrueWithBook(userId);

    return userBookEntities.stream()
        .map(UserBookEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserBook> getBooksForExchange() {
    log.info("Getting books available for exchange");

    List<UserBookEntity> userBookEntities = userBookRepository.findByWantsToExchangeTrueWithBook();

    return userBookEntities.stream()
        .map(UserBookEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }


}