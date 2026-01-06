package com.uade.bookybe.router;

import com.uade.bookybe.core.model.UserRate;
import com.uade.bookybe.core.usecase.UserRateService;
import com.uade.bookybe.router.dto.rate.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRateControllerTest {

    @Mock
    private UserRateService userRateService;

    @InjectMocks
    private UserRateController controller;

    @Test
    void createRating_WithoutAuth_ReturnsUnauthorized() {
        CreateUserRateDto dto = new CreateUserRateDto();
        dto.setRating(5);
        dto.setComment("Great");

        // El controller internamente usa SecurityContextHolder que estará vacío
        // por lo que devolverá 401, esto es esperado en un test unitario
        ResponseEntity<UserRateDto> response = controller.createRating("ex1", dto);

        // En un test unitario sin Spring Security configurado, esperamos 401
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}

