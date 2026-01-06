package com.uade.bookybe.core.exception;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpenAIServiceExceptionTest {
    @Test
    void testConstructorAndMessage() {
        OpenAIServiceException ex = new OpenAIServiceException("Error OpenAI");
        assertEquals("Error OpenAI", ex.getMessage());
    }
}

