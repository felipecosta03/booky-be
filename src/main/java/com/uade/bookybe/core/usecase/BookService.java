package com.uade.bookybe.core.usecase;

import com.uade.bookybe.core.model.Book;
import com.uade.bookybe.core.model.UserBook;
import com.uade.bookybe.core.model.constant.BookStatus;
import java.util.List;
import java.util.Optional;

public interface BookService {

  /** Adds a book to user's library. If book doesn't exist, fetches from Google Books API */
  Optional<UserBook> addBookToUserLibrary(String userId, String isbn, BookStatus status);

  /** Searches books by query (searches in title, author, and categories) */
  List<Book> searchBooks(String query);

  /** Gets a book by ISBN. If not in DB, fetches from Google Books API and saves it */
  Optional<Book> getBookByIsbn(String isbn);

  /** Updates the status of a user's book */
  Optional<UserBook> updateBookStatus(String userId, String bookId, BookStatus status);

  /** Updates the exchange preference of a user's book */
  Optional<UserBook> updateBookExchangePreference(
      String userId, String bookId, boolean wantsToExchange);

  /** Marks a book as favorite or unfavorite */
  Optional<UserBook> toggleBookFavorite(String userId, String bookId);

  /** Gets all books in user's library */
  List<UserBook> getUserLibrary(String userId);

  /** Gets user's favorite books */
  List<UserBook> getUserFavoriteBooks(String userId);

  /** Gets user's library with optional filters for favorites, status and exchange preference */
  List<UserBook> getUserLibraryFiltered(String userId, Boolean favorites, BookStatus status, Boolean wantsToExchange);

  /** Gets books available for exchange */
  List<UserBook> getBooksForExchange();
}
