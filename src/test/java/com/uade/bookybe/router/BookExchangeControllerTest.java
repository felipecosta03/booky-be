package com.uade.bookybe.router;

import com.uade.bookybe.core.model.BookExchange;
import com.uade.bookybe.core.usecase.BookExchangeService;
import com.uade.bookybe.core.usecase.UserRateService;
import com.uade.bookybe.core.usecase.UserService;
import com.uade.bookybe.router.dto.exchange.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookExchangeControllerTest {

    @Mock
    private BookExchangeService bookExchangeService;
    @Mock
    private UserRateService userRateService;
    @Mock
    private UserService userService;

    @InjectMocks
    private BookExchangeController controller;

    @Test
    void createExchange_Success() {
        CreateBookExchangeDto dto = new CreateBookExchangeDto();
        dto.setRequesterId("user1");
        dto.setOwnerId("user2");
        dto.setOwnerBookIds(Arrays.asList("book1"));
        dto.setRequesterBookIds(Arrays.asList("book2"));

        BookExchange exchange = BookExchange.builder()
                .id("ex1")
                .requesterId("user1")
                .ownerId("user2")
                .build();
        
        when(bookExchangeService.createExchange("user1", "user2", 
                Arrays.asList("book1"), Arrays.asList("book2")))
                .thenReturn(Optional.of(exchange));

        ResponseEntity<BookExchangeDto> response = controller.createExchange(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void createExchange_Conflict() {
        CreateBookExchangeDto dto = new CreateBookExchangeDto();
        dto.setRequesterId("user1");
        dto.setOwnerId("user2");
        dto.setOwnerBookIds(Arrays.asList("book1"));
        dto.setRequesterBookIds(Arrays.asList("book2"));

        when(bookExchangeService.createExchange(anyString(), anyString(), anyList(), anyList()))
                .thenReturn(Optional.empty());

        ResponseEntity<BookExchangeDto> response = controller.createExchange(dto);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void getUserExchanges_Success() {
        when(bookExchangeService.getUserExchanges("user1")).thenReturn(Arrays.asList());

        ResponseEntity response = controller.getUserExchanges("user1", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}

