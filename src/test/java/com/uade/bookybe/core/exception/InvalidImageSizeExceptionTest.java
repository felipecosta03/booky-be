package com.uade.bookybe.core.exception;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InvalidImageSizeExceptionTest {
    @Test
    void testConstructorAndMessage() {
        InvalidImageSizeException ex = new InvalidImageSizeException("Tamaño inválido");
        assertEquals("Tamaño inválido", ex.getMessage());
    }
}

