package com.uade.bookybe.infraestructure.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uade.bookybe.core.model.Book;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class GoogleBooksAdapterTest {

  @Mock private RestTemplate restTemplate;

  private ObjectMapper objectMapper;
  private GoogleBooksAdapter sut;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    sut = new GoogleBooksAdapter(restTemplate, objectMapper);
  }

  // ---------------- getBookByIsbn ----------------

  @Test
  void getBookByIsbn_deberiaRetornarEmpty_cuandoResponseEsNull() {
    // given
    String isbn = "9781234567890";
    String expectedUrl = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;

    given(restTemplate.getForObject(eq(expectedUrl), eq(String.class))).willReturn(null);

    // when
    Optional<Book> result = sut.getBookByIsbn(isbn);

    // then
    assertTrue(result.isEmpty());
    then(restTemplate).should().getForObject(eq(expectedUrl), eq(String.class));
  }

  @Test
  void getBookByIsbn_deberiaRetornarEmpty_cuandoNoHayItems() {
    // given
    String isbn = "9781234567890";
    String expectedUrl = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;

    given(restTemplate.getForObject(eq(expectedUrl), eq(String.class)))
        .willReturn(
            """
          {"kind":"books#volumes"}
        """);

    // when
    Optional<Book> result = sut.getBookByIsbn(isbn);

    // then
    assertTrue(result.isEmpty());
    then(restTemplate).should().getForObject(eq(expectedUrl), eq(String.class));
  }

  @Test
  void getBookByIsbn_deberiaRetornarEmpty_cuandoItemsVacio() {
    // given
    String isbn = "9781234567890";
    String expectedUrl = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;

    given(restTemplate.getForObject(eq(expectedUrl), eq(String.class)))
        .willReturn(
            """
          {"items":[]}
        """);

    // when
    Optional<Book> result = sut.getBookByIsbn(isbn);

    // then
    assertTrue(result.isEmpty());
    then(restTemplate).should().getForObject(eq(expectedUrl), eq(String.class));
  }

  @Test
  void getBookByIsbn_deberiaRetornarBook_parseandoCamposPrincipales() {
    // given
    String isbn = "9781234567890";
    String expectedUrl = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;

    String longDesc = "a".repeat(600);

    given(restTemplate.getForObject(eq(expectedUrl), eq(String.class)))
        .willReturn(
            """
          {
            "items": [
              {
                "volumeInfo": {
                  "title": "Mi Libro",
                  "authors": ["Autor Uno", "Autor Dos"],
                  "industryIdentifiers": [
                    { "type": "ISBN_13", "identifier": "9781234567890" },
                    { "type": "ISBN_10", "identifier": "1234567890" }
                  ],
                  "description": "%s",
                  "pageCount": 321,
                  "publisher": "Editorial X",
                  "imageLinks": { "thumbnail": "http://example.com/img.jpg" },
                  "categories": ["Fiction", "Fantasy"],
                  "publishedDate": "2001-01-01"
                }
              }
            ]
          }
        """
                .formatted(longDesc));

    // when
    Optional<Book> result = sut.getBookByIsbn(isbn);

    // then
    assertTrue(result.isPresent());
    Book book = result.get();

    assertEquals("Mi Libro", book.getTitle());
    assertEquals("Autor Uno", book.getAuthor(), "Debe tomar el primer autor del array");
    assertEquals("9781234567890", book.getIsbn(), "Debe priorizar ISBN_13 si existe");
    assertEquals(321, book.getPages());
    assertEquals("Editorial X", book.getPublisher());
    assertEquals("2001-01-01", book.getEdition());

    // description => overview truncado a 500, synopsis completo
    assertNotNull(book.getOverview());
    assertEquals(500, book.getOverview().length());
    assertNotNull(book.getSynopsis());
    assertEquals(600, book.getSynopsis().length());

    // categories
    assertNotNull(book.getCategories());
    assertEquals(List.of("Fiction", "Fantasy"), book.getCategories());

    // image (en tu adapter actual, solo se setea si hay imageLinks)
    assertEquals("http://example.com/img.jpg", book.getImage());

    then(restTemplate).should().getForObject(eq(expectedUrl), eq(String.class));
  }

  @Test
  void getBookByIsbn_deberiaUsarSmallThumbnail_siNoHayThumbnail() {
    // given
    String isbn = "9781234567890";
    String expectedUrl = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;

    given(restTemplate.getForObject(eq(expectedUrl), eq(String.class)))
        .willReturn(
            """
          {
            "items": [
              {
                "volumeInfo": {
                  "title": "Libro",
                  "imageLinks": { "smallThumbnail": "http://example.com/small.jpg" }
                }
              }
            ]
          }
        """);

    // when
    Optional<Book> result = sut.getBookByIsbn(isbn);

    // then
    assertTrue(result.isPresent());
    assertEquals("http://example.com/small.jpg", result.get().getImage());
  }

  @Test
  void getBookByIsbn_deberiaRetornarEmpty_cuandoRestTemplateLanzaRestClientException() {
    // given
    String isbn = "9781234567890";
    String expectedUrl = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;

    given(restTemplate.getForObject(eq(expectedUrl), eq(String.class)))
        .willThrow(new RestClientException("boom"));

    // when
    Optional<Book> result = sut.getBookByIsbn(isbn);

    // then
    assertTrue(result.isEmpty());
  }

  @Test
  void getBookByIsbn_deberiaRetornarEmpty_cuandoJsonInvalido() {
    // given
    String isbn = "9781234567890";
    String expectedUrl = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;

    given(restTemplate.getForObject(eq(expectedUrl), eq(String.class)))
        .willReturn("{ invalid json");

    // when
    Optional<Book> result = sut.getBookByIsbn(isbn);

    // then
    assertTrue(result.isEmpty());
  }

  // ---------------- searchBookByTitle ----------------

  @Test
  void searchBookByTitle_deberiaReemplazarEspaciosPorMas_enLaUrl() {
    // given
    String title = "Harry Potter";
    String expectedUrl = "https://www.googleapis.com/books/v1/volumes?q=intitle:" + "Harry+Potter";

    given(restTemplate.getForObject(eq(expectedUrl), eq(String.class)))
        .willReturn(
            """
          { "items": [ { "volumeInfo": { "title": "HP", "authors": ["J.K."] } } ] }
        """);

    // when
    Optional<Book> result = sut.searchBookByTitle(title);

    // then
    assertTrue(result.isPresent());
    assertEquals("HP", result.get().getTitle());
    assertEquals("J.K.", result.get().getAuthor());

    then(restTemplate).should().getForObject(eq(expectedUrl), eq(String.class));
  }

  @Test
  void searchBookByTitle_deberiaRetornarEmpty_cuandoItemsVacio() {
    // given
    String title = "Nada";
    String expectedUrl = "https://www.googleapis.com/books/v1/volumes?q=intitle:" + "Nada";

    given(restTemplate.getForObject(eq(expectedUrl), eq(String.class)))
        .willReturn(
            """
          { "items": [] }
        """);

    // when
    Optional<Book> result = sut.searchBookByTitle(title);

    // then
    assertTrue(result.isEmpty());
  }

  @Test
  void searchBookByTitle_deberiaRetornarEmpty_cuandoResponseNull() {
    // given
    String title = "Algo";
    String expectedUrl = "https://www.googleapis.com/books/v1/volumes?q=intitle:" + "Algo";

    given(restTemplate.getForObject(eq(expectedUrl), eq(String.class))).willReturn(null);

    // when
    Optional<Book> result = sut.searchBookByTitle(title);

    // then
    assertTrue(result.isEmpty());
  }

  @Test
  void searchBookByTitle_deberiaRetornarEmpty_cuandoRestTemplateLanzaRestClientException() {
    // given
    String title = "Boom";
    String expectedUrl = "https://www.googleapis.com/books/v1/volumes?q=intitle:" + "Boom";

    given(restTemplate.getForObject(eq(expectedUrl), eq(String.class)))
        .willThrow(new RestClientException("down"));

    // when
    Optional<Book> result = sut.searchBookByTitle(title);

    // then
    assertTrue(result.isEmpty());
  }
}
