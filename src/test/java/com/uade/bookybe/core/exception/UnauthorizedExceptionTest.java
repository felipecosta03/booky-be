package com.uade.bookybe.core.exception;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnauthorizedExceptionTest {
    @Test
    void testConstructorAndMessage() {
        UnauthorizedException ex = new UnauthorizedException("No autorizado");
        assertEquals("No autorizado", ex.getMessage());
    }
}

