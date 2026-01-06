package com.uade.bookybe.core.usecase.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

import com.uade.bookybe.core.model.Book;
import com.uade.bookybe.core.model.UserBook;
import com.uade.bookybe.core.model.constant.BookStatus;
import com.uade.bookybe.core.port.GoogleBooksPort;
import com.uade.bookybe.core.usecase.GamificationService;
import com.uade.bookybe.infraestructure.entity.BookEntity;
import com.uade.bookybe.infraestructure.entity.UserBookEntity;
import com.uade.bookybe.infraestructure.repository.BookRepository;
import com.uade.bookybe.infraestructure.repository.UserBookRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock private BookRepository bookRepository;
    @Mock private UserBookRepository userBookRepository;
    @Mock private GoogleBooksPort googleBooksPort;
    @Mock private GamificationService gamificationService;

    @InjectMocks private BookServiceImpl sut;

    // ---------------- addBookToUserLibrary ----------------

    @Test
    void addBookToUserLibrary_deberiaRetornarEmpty_cuandoUsuarioYaTieneElLibro() {
        // given
        String userId = "u1";
        String isbn = "9780000000001";
        BookStatus status = BookStatus.READING;

        BookEntity existingBookEntity = BookEntity.builder()
                .id("book-1")
                .isbn(isbn)
                .title("T")
                .build();

        given(bookRepository.findByIsbn(isbn)).willReturn(Optional.of(existingBookEntity));
        given(userBookRepository.existsByUserIdAndBookId(userId, "book-1")).willReturn(true);

        // when
        Optional<UserBook> result = sut.addBookToUserLibrary(userId, isbn, status);

        // then
        assertTrue(result.isEmpty());
        then(userBookRepository).should().existsByUserIdAndBookId(userId, "book-1");
        then(userBookRepository).should(never()).save(any(UserBookEntity.class));
        then(gamificationService).shouldHaveNoInteractions();
    }

    @Test
    void addBookToUserLibrary_deberiaGuardarYRetornarUserBook_cuandoNoExisteEnLaBiblioteca() {
        // given
        String userId = "u1";
        String isbn = "9780000000001";
        BookStatus status = BookStatus.READING;

        BookEntity existingBookEntity = BookEntity.builder()
                .id("book-1")
                .isbn(isbn)
                .title("T")
                .build();

        given(bookRepository.findByIsbn(isbn)).willReturn(Optional.of(existingBookEntity));
        given(userBookRepository.existsByUserIdAndBookId(userId, "book-1")).willReturn(false);

        // capturamos el entity guardado para verificar campos importantes
        ArgumentCaptor<UserBookEntity> captor = ArgumentCaptor.forClass(UserBookEntity.class);

        // el save devuelve el mismo entity (típico en mocks)
        given(userBookRepository.save(any(UserBookEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, UserBookEntity.class));

        // simular "reload con book" (lo más importante para que devuelva Optional.of)
        UserBookEntity savedWithBook = UserBookEntity.builder()
                .id("user-book-aaaa1111")
                .userId(userId)
                .bookId("book-1")
                .status(status)
                .favorite(false)
                .wantsToExchange(false)
                // si tu entity tiene relación book, acá podrías setearla también
                .build();

        given(userBookRepository.findByUserIdAndBookIdWithBook(userId, "book-1"))
                .willReturn(Optional.of(savedWithBook));

        // when
        Optional<UserBook> result = sut.addBookToUserLibrary(userId, isbn, status);

        // then
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUserId());
        assertEquals("book-1", result.get().getBookId());
        assertEquals(status, result.get().getStatus());
        assertFalse(result.get().isFavorite());
        assertFalse(result.get().isWantsToExchange());

        then(userBookRepository).should().save(captor.capture());
        UserBookEntity toSave = captor.getValue();
        assertNotNull(toSave.getId());
        assertTrue(toSave.getId().startsWith("user-book-"));
        assertEquals(userId, toSave.getUserId());
        assertEquals("book-1", toSave.getBookId());
        assertEquals(status, toSave.getStatus());
        assertFalse(toSave.isFavorite());
        assertFalse(toSave.isWantsToExchange());

        then(userBookRepository).should().findByUserIdAndBookIdWithBook(userId, "book-1");
        then(gamificationService).should().processBookAdded(userId);
    }

    @Test
    void addBookToUserLibrary_deberiaRetornarEmpty_cuandoNoPuedeRecargarConBook() {
        // given
        String userId = "u1";
        String isbn = "9780000000001";
        BookStatus status = BookStatus.READING;

        BookEntity existingBookEntity = BookEntity.builder()
                .id("book-1")
                .isbn(isbn)
                .title("T")
                .build();

        given(bookRepository.findByIsbn(isbn)).willReturn(Optional.of(existingBookEntity));
        given(userBookRepository.existsByUserIdAndBookId(userId, "book-1")).willReturn(false);

        given(userBookRepository.save(any(UserBookEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, UserBookEntity.class));

        given(userBookRepository.findByUserIdAndBookIdWithBook(userId, "book-1"))
                .willReturn(Optional.empty());

        // when
        Optional<UserBook> result = sut.addBookToUserLibrary(userId, isbn, status);

        // then
        assertTrue(result.isEmpty());
        then(gamificationService).shouldHaveNoInteractions();
    }

    // ---------------- searchBooks ----------------

    @Test
    void searchBooks_deberiaMapearYRetornarLista() {
        // given
        String query = "harry";
        List<BookEntity> entities = List.of(
                BookEntity.builder().id("b1").isbn("1").title("A").build(),
                BookEntity.builder().id("b2").isbn("2").title("B").build()
        );

        given(bookRepository.searchBooks(query)).willReturn(entities);

        // when
        List<Book> result = sut.searchBooks(query);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("b1", result.get(0).getId());
        assertEquals("b2", result.get(1).getId());
        then(bookRepository).should().searchBooks(query);
    }

    // ---------------- getBookByIsbn ----------------

    @Test
    void getBookByIsbn_deberiaRetornarBook_cuandoExisteEnDB() {
        // given
        String isbn = "9780000000001";
        BookEntity entity = BookEntity.builder()
                .id("b1")
                .isbn(isbn)
                .title("DB Book")
                .build();

        given(bookRepository.findByIsbn(isbn)).willReturn(Optional.of(entity));

        // when
        Optional<Book> result = sut.getBookByIsbn(isbn);

        // then
        assertTrue(result.isPresent());
        assertEquals("b1", result.get().getId());
        assertEquals(isbn, result.get().getIsbn());
        then(googleBooksPort).shouldHaveNoInteractions();
        then(bookRepository).should(never()).save(any(BookEntity.class));
    }

    @Test
    void getBookByIsbn_deberiaRetornarEmpty_cuandoNoExisteEnDBNiEnGoogle() {
        // given
        String isbn = "9780000000001";
        given(bookRepository.findByIsbn(isbn)).willReturn(Optional.empty());
        given(googleBooksPort.getBookByIsbn(isbn)).willReturn(Optional.empty());

        // when
        Optional<Book> result = sut.getBookByIsbn(isbn);

        // then
        assertTrue(result.isEmpty());
        then(bookRepository).should().findByIsbn(isbn);
        then(googleBooksPort).should().getBookByIsbn(isbn);
        then(bookRepository).should(never()).save(any(BookEntity.class));
    }

    @Test
    void getBookByIsbn_deberiaRetornarExistentePorIsbnDeGoogle_cuandoGoogleDevuelveOtroIsbnQueYaExiste() {
        // given
        String isbnSolicitado = "9780000000001";
        String isbnGoogle = "9789999999999";

        given(bookRepository.findByIsbn(isbnSolicitado)).willReturn(Optional.empty());

        Book googleBook = Book.builder()
                .id("ignored")
                .isbn(isbnGoogle)
                .title("Google Book")
                .build();
        given(googleBooksPort.getBookByIsbn(isbnSolicitado)).willReturn(Optional.of(googleBook));

        BookEntity existingGoogleIsbnEntity = BookEntity.builder()
                .id("b-google")
                .isbn(isbnGoogle)
                .title("Existing")
                .build();
        given(bookRepository.findByIsbn(isbnGoogle)).willReturn(Optional.of(existingGoogleIsbnEntity));

        // when
        Optional<Book> result = sut.getBookByIsbn(isbnSolicitado);

        // then
        assertTrue(result.isPresent());
        assertEquals("b-google", result.get().getId());
        assertEquals(isbnGoogle, result.get().getIsbn());
        then(bookRepository).should(never()).save(any(BookEntity.class));
    }

    @Test
    void getBookByIsbn_deberiaGuardarYRetornarBook_cuandoVieneDeGoogleYNoHayConflictos() {
        // given
        String isbn = "9780000000001";
        given(bookRepository.findByIsbn(isbn)).willReturn(Optional.empty());

        Book googleBook = Book.builder()
                .id("x")
                .isbn(isbn)
                .title("Google Book")
                .build();
        given(googleBooksPort.getBookByIsbn(isbn)).willReturn(Optional.of(googleBook));

        // el save debe devolver un entity con id seteado por el service
        given(bookRepository.save(any(BookEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, BookEntity.class));

        // when
        Optional<Book> result = sut.getBookByIsbn(isbn);

        // then
        assertTrue(result.isPresent());
        assertNotNull(result.get().getId());
        assertTrue(result.get().getId().startsWith("book-"));
        assertEquals(isbn, result.get().getIsbn());

        ArgumentCaptor<BookEntity> captor = ArgumentCaptor.forClass(BookEntity.class);
        then(bookRepository).should().save(captor.capture());
        BookEntity saved = captor.getValue();
        assertNotNull(saved.getId());
        assertTrue(saved.getId().startsWith("book-"));
        assertEquals(isbn, saved.getIsbn());
    }

    @Test
    void getBookByIsbn_deberiaRetornarLibroPorIsbnGoogle_cuandoSaveDaConflictoDuplicateKey() {
        // given
        String isbnSolicitado = "9780000000001";
        String isbnGoogle = "9789999999999";

        given(bookRepository.findByIsbn(isbnSolicitado)).willReturn(Optional.empty());

        Book googleBook = Book.builder()
                .id("x")
                .isbn(isbnGoogle)
                .title("Google Book")
                .build();
        given(googleBooksPort.getBookByIsbn(isbnSolicitado)).willReturn(Optional.of(googleBook));

        BookEntity conflictEntity = BookEntity.builder()
                .id("b-conflict")
                .isbn(isbnGoogle)
                .title("Conflict")
                .build();
        given(bookRepository.findByIsbn(isbnGoogle)).willReturn(Optional.of(conflictEntity));

        // when
        Optional<Book> result = sut.getBookByIsbn(isbnSolicitado);

        // then
        assertTrue(result.isPresent());
        assertEquals("b-conflict", result.get().getId());
        assertEquals(isbnGoogle, result.get().getIsbn());
    }


    @Test
    void getBookByIsbn_deberiaRetornarEmpty_cuandoSaveDaErrorNoDuplicateKey() {
        // given
        String isbn = "9780000000001";
        given(bookRepository.findByIsbn(isbn)).willReturn(Optional.empty());

        Book googleBook = Book.builder()
                .id("x")
                .isbn(isbn)
                .title("Google Book")
                .build();
        given(googleBooksPort.getBookByIsbn(isbn)).willReturn(Optional.of(googleBook));

        given(bookRepository.save(any(BookEntity.class)))
                .willThrow(new RuntimeException("boom"));

        // when
        Optional<Book> result = sut.getBookByIsbn(isbn);

        // then
        assertTrue(result.isEmpty());
    }

    // ---------------- updateBookStatus ----------------

    @Test
    void updateBookStatus_deberiaRetornarEmpty_cuandoNoExisteUserBook() {
        // given
        given(userBookRepository.findByUserIdAndBookId("u1", "b1")).willReturn(Optional.empty());

        // when
        Optional<UserBook> result = sut.updateBookStatus("u1", "b1", BookStatus.READ);

        // then
        assertTrue(result.isEmpty());
        then(userBookRepository).should(never()).save(any(UserBookEntity.class));
        then(gamificationService).shouldHaveNoInteractions();
    }

    @Test
    void updateBookStatus_deberiaActualizarYDarPuntos_cuandoPasaARead_desdeOtroEstado() {
        // given
        String userId = "u1";
        String bookId = "b1";

        UserBookEntity entity = UserBookEntity.builder()
                .id("ub1")
                .userId(userId)
                .bookId(bookId)
                .status(BookStatus.READING)
                .favorite(false)
                .wantsToExchange(false)
                .build();

        given(userBookRepository.findByUserIdAndBookId(userId, bookId)).willReturn(Optional.of(entity));
        given(userBookRepository.save(any(UserBookEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, UserBookEntity.class));

        // when
        Optional<UserBook> result = sut.updateBookStatus(userId, bookId, BookStatus.READ);

        // then
        assertTrue(result.isPresent());
        assertEquals(BookStatus.READ, result.get().getStatus());
        then(gamificationService).should().processBookRead(userId);
    }

    @Test
    void updateBookStatus_noDeberiaDarPuntos_cuandoYaEstabaEnRead() {
        // given
        String userId = "u1";
        String bookId = "b1";

        UserBookEntity entity = UserBookEntity.builder()
                .id("ub1")
                .userId(userId)
                .bookId(bookId)
                .status(BookStatus.READ)
                .favorite(false)
                .wantsToExchange(false)
                .build();

        given(userBookRepository.findByUserIdAndBookId(userId, bookId)).willReturn(Optional.of(entity));
        given(userBookRepository.save(any(UserBookEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, UserBookEntity.class));

        // when
        Optional<UserBook> result = sut.updateBookStatus(userId, bookId, BookStatus.READ);

        // then
        assertTrue(result.isPresent());
        then(gamificationService).shouldHaveNoInteractions();
    }

    @Test
    void updateBookStatus_noDeberiaDarPuntos_cuandoNuevoEstadoNoEsRead() {
        // given
        String userId = "u1";
        String bookId = "b1";

        UserBookEntity entity = UserBookEntity.builder()
                .id("ub1")
                .userId(userId)
                .bookId(bookId)
                .status(BookStatus.TO_READ)
                .favorite(false)
                .wantsToExchange(false)
                .build();

        given(userBookRepository.findByUserIdAndBookId(userId, bookId)).willReturn(Optional.of(entity));
        given(userBookRepository.save(any(UserBookEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, UserBookEntity.class));

        // when
        Optional<UserBook> result = sut.updateBookStatus(userId, bookId, BookStatus.READING);

        // then
        assertTrue(result.isPresent());
        then(gamificationService).shouldHaveNoInteractions();
    }

    // ---------------- updateBookExchangePreference ----------------

    @Test
    void updateBookExchangePreference_deberiaRetornarEmpty_cuandoNoExisteUserBook() {
        // given
        given(userBookRepository.findByUserIdAndBookId("u1", "b1")).willReturn(Optional.empty());

        // when
        Optional<UserBook> result = sut.updateBookExchangePreference("u1", "b1", true);

        // then
        assertTrue(result.isEmpty());
        then(userBookRepository).should(never()).save(any(UserBookEntity.class));
        then(gamificationService).shouldHaveNoInteractions();
    }

    @Test
    void updateBookExchangePreference_deberiaDarPuntos_cuandoPasaAQuererIntercambiar_desdeFalse() {
        // given
        String userId = "u1";
        String bookId = "b1";

        UserBookEntity entity = UserBookEntity.builder()
                .id("ub1")
                .userId(userId)
                .bookId(bookId)
                .status(BookStatus.READING)
                .favorite(false)
                .wantsToExchange(false)
                .build();

        given(userBookRepository.findByUserIdAndBookId(userId, bookId)).willReturn(Optional.of(entity));
        given(userBookRepository.save(any(UserBookEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, UserBookEntity.class));

        // when
        Optional<UserBook> result = sut.updateBookExchangePreference(userId, bookId, true);

        // then
        assertTrue(result.isPresent());
        assertTrue(result.get().isWantsToExchange());
        then(gamificationService).should().processBookOfferedForExchange(userId);
    }

    @Test
    void updateBookExchangePreference_noDeberiaDarPuntos_cuandoPermaneceTrue() {
        // given
        String userId = "u1";
        String bookId = "b1";

        UserBookEntity entity = UserBookEntity.builder()
                .id("ub1")
                .userId(userId)
                .bookId(bookId)
                .status(BookStatus.READING)
                .favorite(false)
                .wantsToExchange(true)
                .build();

        given(userBookRepository.findByUserIdAndBookId(userId, bookId)).willReturn(Optional.of(entity));
        given(userBookRepository.save(any(UserBookEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, UserBookEntity.class));

        // when
        Optional<UserBook> result = sut.updateBookExchangePreference(userId, bookId, true);

        // then
        assertTrue(result.isPresent());
        then(gamificationService).shouldHaveNoInteractions();
    }

    // ---------------- toggleBookFavorite ----------------

    @Test
    void toggleBookFavorite_deberiaRetornarEmpty_cuandoNoExisteUserBook() {
        // given
        given(userBookRepository.findByUserIdAndBookId("u1", "b1")).willReturn(Optional.empty());

        // when
        Optional<UserBook> result = sut.toggleBookFavorite("u1", "b1");

        // then
        assertTrue(result.isEmpty());
        then(userBookRepository).should(never()).save(any(UserBookEntity.class));
        then(gamificationService).shouldHaveNoInteractions();
    }

    @Test
    void toggleBookFavorite_deberiaTogglearYDarPuntos_cuandoPasaATrue_desdeFalse() {
        // given
        String userId = "u1";
        String bookId = "b1";

        UserBookEntity entity = UserBookEntity.builder()
                .id("ub1")
                .userId(userId)
                .bookId(bookId)
                .status(BookStatus.READING)
                .favorite(false)
                .wantsToExchange(false)
                .build();

        given(userBookRepository.findByUserIdAndBookId(userId, bookId)).willReturn(Optional.of(entity));
        given(userBookRepository.save(any(UserBookEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, UserBookEntity.class));

        // when
        Optional<UserBook> result = sut.toggleBookFavorite(userId, bookId);

        // then
        assertTrue(result.isPresent());
        assertTrue(result.get().isFavorite());
        then(gamificationService).should().processBookFavorited(userId);
    }

    @Test
    void toggleBookFavorite_noDeberiaDarPuntos_cuandoPasaAFalse_desdeTrue() {
        // given
        String userId = "u1";
        String bookId = "b1";

        UserBookEntity entity = UserBookEntity.builder()
                .id("ub1")
                .userId(userId)
                .bookId(bookId)
                .status(BookStatus.READING)
                .favorite(true)
                .wantsToExchange(false)
                .build();

        given(userBookRepository.findByUserIdAndBookId(userId, bookId)).willReturn(Optional.of(entity));
        given(userBookRepository.save(any(UserBookEntity.class)))
                .willAnswer(inv -> inv.getArgument(0, UserBookEntity.class));

        // when
        Optional<UserBook> result = sut.toggleBookFavorite(userId, bookId);

        // then
        assertTrue(result.isPresent());
        assertFalse(result.get().isFavorite());
        then(gamificationService).shouldHaveNoInteractions();
    }

    // ---------------- getUserLibrary / favorites / filters / exchange ----------------

    @Test
    void getUserLibrary_deberiaMapearYRetornarLista() {
        // given
        String userId = "u1";
        given(userBookRepository.findByUserIdWithBook(userId))
                .willReturn(List.of(
                        UserBookEntity.builder().id("ub1").userId(userId).bookId("b1").status(BookStatus.TO_READ).build(),
                        UserBookEntity.builder().id("ub2").userId(userId).bookId("b2").status(BookStatus.READING).build()
                ));

        // when
        List<UserBook> result = sut.getUserLibrary(userId);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        then(userBookRepository).should().findByUserIdWithBook(userId);
    }

    @Test
    void getUserFavoriteBooks_deberiaMapearYRetornarLista() {
        // given
        String userId = "u1";
        given(userBookRepository.findByUserIdAndIsFavoriteTrueWithBook(userId))
                .willReturn(List.of(
                        UserBookEntity.builder().id("ub1").userId(userId).bookId("b1").status(BookStatus.TO_READ).favorite(true).build()
                ));

        // when
        List<UserBook> result = sut.getUserFavoriteBooks(userId);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        then(userBookRepository).should().findByUserIdAndIsFavoriteTrueWithBook(userId);
    }

    @Test
    void getUserLibraryFiltered_deberiaLlamarRepoConFiltrosYMapear() {
        // given
        String userId = "u1";
        Boolean favorites = true;
        BookStatus status = BookStatus.READING;
        Boolean wantsToExchange = false;

        given(userBookRepository.findByUserIdWithFilters(userId, favorites, status, wantsToExchange))
                .willReturn(List.of(
                        UserBookEntity.builder().id("ub1").userId(userId).bookId("b1").status(status).favorite(true).wantsToExchange(false).build()
                ));

        // when
        List<UserBook> result = sut.getUserLibraryFiltered(userId, favorites, status, wantsToExchange);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        then(userBookRepository).should().findByUserIdWithFilters(userId, favorites, status, wantsToExchange);
    }

    @Test
    void getBooksForExchange_deberiaMapearYRetornarLista() {
        // given
        given(userBookRepository.findByWantsToExchangeTrueWithBook())
                .willReturn(List.of(
                        UserBookEntity.builder().id("ub1").userId("u1").bookId("b1").status(BookStatus.READING).wantsToExchange(true).build()
                ));

        // when
        List<UserBook> result = sut.getBooksForExchange();

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        then(userBookRepository).should().findByWantsToExchangeTrueWithBook();
    }

    // ---------------- getBookById ----------------

    @Test
    void getBookById_deberiaMapearYRetornarOptional() {
        // given
        String bookId = "b1";
        given(bookRepository.findById(bookId))
                .willReturn(Optional.of(BookEntity.builder().id(bookId).isbn("1").title("T").build()));

        // when
        Optional<Book> result = sut.getBookById(bookId);

        // then
        assertTrue(result.isPresent());
        assertEquals(bookId, result.get().getId());
        then(bookRepository).should().findById(bookId);
    }

    @Test
    void getBookById_deberiaRetornarEmpty_cuandoNoExiste() {
        // given
        String bookId = "b1";
        given(bookRepository.findById(bookId)).willReturn(Optional.empty());

        // when
        Optional<Book> result = sut.getBookById(bookId);

        // then
        assertTrue(result.isEmpty());
    }
}
