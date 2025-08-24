package com.uade.bookybe.core.port;

import com.uade.bookybe.core.model.Book;
import java.util.Optional;

public interface GoogleBooksPort {

  /** Fetches book information from Google Books API by ISBN */
  Optional<Book> getBookByIsbn(String isbn);

  /** Searches books in Google Books API by title */
  Optional<Book> searchBookByTitle(String title);
}
