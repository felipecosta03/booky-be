package com.uade.bookybe.infraestructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uade.bookybe.core.model.Book;
import com.uade.bookybe.core.port.GoogleBooksPort;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleBooksAdapter implements GoogleBooksPort {

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  
  private static final String GOOGLE_BOOKS_API_URL = "https://www.googleapis.com/books/v1/volumes";

  @Override
  public Optional<Book> getBookByIsbn(String isbn) {
    log.info("Fetching book from Google Books API with ISBN: {}", isbn);
    
    try {
      String url = GOOGLE_BOOKS_API_URL + "?q=isbn:" + isbn;
      String response = restTemplate.getForObject(url, String.class);
      
      if (response == null) {
        log.warn("No response from Google Books API for ISBN: {}", isbn);
        return Optional.empty();
      }
      
      JsonNode jsonResponse = objectMapper.readTree(response);
      JsonNode items = jsonResponse.get("items");
      
      if (items == null || items.size() == 0) {
        log.warn("No books found for ISBN: {}", isbn);
        return Optional.empty();
      }
      
      JsonNode bookItem = items.get(0);
      Book book = parseBookFromJson(bookItem);
      
      log.info("Successfully fetched book: {}", book.getTitle());
      return Optional.of(book);
      
    } catch (RestClientException e) {
      log.error("Error calling Google Books API for ISBN {}: {}", isbn, e.getMessage());
      return Optional.empty();
    } catch (Exception e) {
      log.error("Error parsing Google Books API response for ISBN {}: {}", isbn, e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<Book> searchBookByTitle(String title) {
    log.info("Searching book from Google Books API with title: {}", title);
    
    try {
      String url = GOOGLE_BOOKS_API_URL + "?q=intitle:" + title.replace(" ", "+");
      String response = restTemplate.getForObject(url, String.class);
      
      if (response == null) {
        log.warn("No response from Google Books API for title: {}", title);
        return Optional.empty();
      }
      
      JsonNode jsonResponse = objectMapper.readTree(response);
      JsonNode items = jsonResponse.get("items");
      
      if (items == null || items.size() == 0) {
        log.warn("No books found for title: {}", title);
        return Optional.empty();
      }
      
      JsonNode bookItem = items.get(0);
      Book book = parseBookFromJson(bookItem);
      
      log.info("Successfully found book: {}", book.getTitle());
      return Optional.of(book);
      
    } catch (RestClientException e) {
      log.error("Error calling Google Books API for title {}: {}", title, e.getMessage());
      return Optional.empty();
    } catch (Exception e) {
      log.error("Error parsing Google Books API response for title {}: {}", title, e.getMessage());
      return Optional.empty();
    }
  }

  private Book parseBookFromJson(JsonNode bookItem) throws Exception {
    JsonNode volumeInfo = bookItem.get("volumeInfo");
    
    if (volumeInfo == null) {
      throw new Exception("Invalid book format: missing volumeInfo");
    }

    Book.BookBuilder bookBuilder = Book.builder();
    
    // Title
    if (volumeInfo.has("title")) {
      bookBuilder.title(volumeInfo.get("title").asText());
    }
    
    // Authors
    if (volumeInfo.has("authors")) {
      JsonNode authors = volumeInfo.get("authors");
      if (authors.isArray() && authors.size() > 0) {
        bookBuilder.author(authors.get(0).asText());
      }
    }
    
    // ISBN
    if (volumeInfo.has("industryIdentifiers")) {
      JsonNode identifiers = volumeInfo.get("industryIdentifiers");
      for (JsonNode identifier : identifiers) {
        if ("ISBN_13".equals(identifier.get("type").asText()) ||
            "ISBN_10".equals(identifier.get("type").asText())) {
          bookBuilder.isbn(identifier.get("identifier").asText());
          break;
        }
      }
    }
    
    // Description
    if (volumeInfo.has("description")) {
      String description = volumeInfo.get("description").asText();
      bookBuilder.overview(description.length() > 500 ? description.substring(0, 500) : description);
      bookBuilder.synopsis(description);
    }
    
    // Pages
    if (volumeInfo.has("pageCount")) {
      bookBuilder.pages(volumeInfo.get("pageCount").asInt());
    }
    
    // Publisher
    if (volumeInfo.has("publisher")) {
      bookBuilder.publisher(volumeInfo.get("publisher").asText());
    }
    
    // Image
    if (volumeInfo.has("imageLinks")) {
      JsonNode imageLinks = volumeInfo.get("imageLinks");
      if (imageLinks.has("thumbnail")) {
        bookBuilder.image(imageLinks.get("thumbnail").asText());
      }else if (imageLinks.has("smallThumbnail")){
        bookBuilder.image(imageLinks.get("smallThumbnail").asText());
      }else {
        bookBuilder.image("https://books.google.com/googlebooks/images/no_cover_thumb.gif");
      }
    }
    
    // Categories
    if (volumeInfo.has("categories")) {
      JsonNode categories = volumeInfo.get("categories");
      if (categories.isArray()) {
        List<String> categoryList = Arrays.asList(objectMapper.convertValue(categories, String[].class));
        bookBuilder.categories(categoryList);
      }
    }
    
    // Published date as edition
    if (volumeInfo.has("publishedDate")) {
      bookBuilder.edition(volumeInfo.get("publishedDate").asText());
    }
    
    return bookBuilder.build();
  }
} 