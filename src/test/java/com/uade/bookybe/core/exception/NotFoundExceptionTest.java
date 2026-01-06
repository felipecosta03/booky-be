package com.uade.bookybe.core.exception;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotFoundExceptionTest {
    @Test
    void testConstructorAndMessage() {
        NotFoundException ex = new NotFoundException("No encontrado");
        assertEquals("No encontrado", ex.getMessage());
    }
}

