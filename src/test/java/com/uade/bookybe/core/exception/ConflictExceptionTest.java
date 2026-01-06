package com.uade.bookybe.core.exception;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConflictExceptionTest {
    @Test
    void testConstructorAndMessage() {
        ConflictException ex = new ConflictException("Conflicto");
        assertEquals("Conflicto", ex.getMessage());
    }
}

