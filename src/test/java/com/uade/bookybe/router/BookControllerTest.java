package com.uade.bookybe.router;

import com.uade.bookybe.core.model.Book;
import com.uade.bookybe.core.model.UserBook;
import com.uade.bookybe.core.model.constant.BookStatus;
import com.uade.bookybe.core.usecase.BookService;
import com.uade.bookybe.router.dto.book.*;
import com.uade.bookybe.router.mapper.BookDtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    @Mock
    private BookService bookService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BookController bookController;

    private Book testBook;
    private UserBook testUserBook;

    @BeforeEach
    void setUp() {
        testBook = Book.builder()
                .id("book123")
                .isbn("9781234567890")
                .title("Test Book")
                .author("Test Author")
                .build();

        testUserBook = UserBook.builder()
                .id("userBook123")
                .userId("user123")
                .book(testBook)
                .status(BookStatus.READING)
                .favorite(false)
                .wantsToExchange(false)
                .build();
    }

    @Test
    void addBookToUserLibrary_Success() {
        // Arrange
        AddBookToLibraryDto dto = new AddBookToLibraryDto();
        dto.setIsbn("9781234567890");
        dto.setStatus(BookStatus.READING);

        when(authentication.getName()).thenReturn("user123");
        when(bookService.addBookToUserLibrary(anyString(), anyString(), any(BookStatus.class)))
                .thenReturn(Optional.of(testUserBook));

        // Act
        ResponseEntity<UserBookDto> response = bookController.addBookToUserLibrary(dto, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(bookService).addBookToUserLibrary("user123", "9781234567890", BookStatus.READING);
    }

    @Test
    void addBookToUserLibrary_Conflict() {
        // Arrange
        AddBookToLibraryDto dto = new AddBookToLibraryDto();
        dto.setIsbn("9781234567890");
        dto.setStatus(BookStatus.READING);

        when(authentication.getName()).thenReturn("user123");
        when(bookService.addBookToUserLibrary(anyString(), anyString(), any(BookStatus.class)))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<UserBookDto> response = bookController.addBookToUserLibrary(dto, authentication);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getUserLibrary_Success() {
        // Arrange
        List<UserBook> userBooks = Arrays.asList(testUserBook);
        when(bookService.getUserLibraryFiltered(anyString(), any(), any(), any()))
                .thenReturn(userBooks);

        // Act
        ResponseEntity<List<UserBookDto>> response = bookController.getUserLibrary(
                "user123", null, null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(bookService).getUserLibraryFiltered("user123", null, null, null);
    }

    @Test
    void getUserLibrary_WithFilters() {
        // Arrange
        when(bookService.getUserLibraryFiltered("user123", true, BookStatus.READING, true))
                .thenReturn(Arrays.asList(testUserBook));

        // Act
        ResponseEntity<List<UserBookDto>> response = bookController.getUserLibrary(
                "user123", true, BookStatus.READING, true);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(bookService).getUserLibraryFiltered("user123", true, BookStatus.READING, true);
    }

    @Test
    void getBooksForExchange_Success() {
        // Arrange
        List<UserBook> exchangeBooks = Arrays.asList(testUserBook);
        when(bookService.getBooksForExchange()).thenReturn(exchangeBooks);

        // Act
        ResponseEntity<List<UserBookDto>> response = bookController.getBooksForExchange();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(bookService).getBooksForExchange();
    }

    @Test
    void updateBookStatus_Success() {
        // Arrange
        UpdateStatusDto dto = new UpdateStatusDto();
        dto.setStatus(BookStatus.READ);

        when(authentication.getName()).thenReturn("user123");
        when(bookService.updateBookStatus(anyString(), anyString(), any(BookStatus.class)))
                .thenReturn(Optional.of(testUserBook));

        // Act
        ResponseEntity<UserBookDto> response = bookController.updateBookStatus("book123", dto, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(bookService).updateBookStatus("user123", "book123", BookStatus.READ);
    }

    @Test
    void updateBookStatus_NotFound() {
        // Arrange
        UpdateStatusDto dto = new UpdateStatusDto();
        dto.setStatus(BookStatus.READ);

        when(authentication.getName()).thenReturn("user123");
        when(bookService.updateBookStatus(anyString(), anyString(), any(BookStatus.class)))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<UserBookDto> response = bookController.updateBookStatus("book123", dto, authentication);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void updateExchangePreference_Success() {
        // Arrange
        UpdateExchangePreferenceDto dto = new UpdateExchangePreferenceDto();
        dto.setWantsToExchange(true);

        when(authentication.getName()).thenReturn("user123");
        when(bookService.updateBookExchangePreference(anyString(), anyString(), anyBoolean()))
                .thenReturn(Optional.of(testUserBook));

        // Act
        ResponseEntity<UserBookDto> response = bookController.updateExchangePreference("book123", dto, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(bookService).updateBookExchangePreference("user123", "book123", true);
    }

    @Test
    void updateExchangePreference_NotFound() {
        // Arrange
        UpdateExchangePreferenceDto dto = new UpdateExchangePreferenceDto();
        dto.setWantsToExchange(true);

        when(authentication.getName()).thenReturn("user123");
        when(bookService.updateBookExchangePreference(anyString(), anyString(), anyBoolean()))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<UserBookDto> response = bookController.updateExchangePreference("book123", dto, authentication);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void toggleBookFavorite_Success() {
        // Arrange
        when(authentication.getName()).thenReturn("user123");
        when(bookService.toggleBookFavorite(anyString(), anyString()))
                .thenReturn(Optional.of(testUserBook));

        // Act
        ResponseEntity<UserBookDto> response = bookController.toggleBookFavorite("book123", authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(bookService).toggleBookFavorite("user123", "book123");
    }

    @Test
    void toggleBookFavorite_NotFound() {
        // Arrange
        when(authentication.getName()).thenReturn("user123");
        when(bookService.toggleBookFavorite(anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<UserBookDto> response = bookController.toggleBookFavorite("book123", authentication);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void searchBooks_Success() {
        // Arrange
        List<Book> books = Arrays.asList(testBook);
        when(bookService.searchBooks(anyString())).thenReturn(books);

        // Act
        ResponseEntity<List<BookDto>> response = bookController.searchBooks("test");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(bookService).searchBooks("test");
    }

    @Test
    void getBookByIsbn_Success() {
        // Arrange
        when(bookService.getBookByIsbn(anyString())).thenReturn(Optional.of(testBook));

        // Act
        ResponseEntity<BookDto> response = bookController.getBookByIsbn("9781234567890");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(bookService).getBookByIsbn("9781234567890");
    }

    @Test
    void getBookByIsbn_NotFound() {
        // Arrange
        when(bookService.getBookByIsbn(anyString())).thenReturn(Optional.empty());

        // Act
        ResponseEntity<BookDto> response = bookController.getBookByIsbn("9781234567890");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }
}

