package com.uade.bookybe.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SceneImageConfigTest {

    @Test
    void testDefaultValues() {
        SceneImageConfig config = new SceneImageConfig();

        assertEquals("1024x1024", config.getDefaultSize());
        assertEquals(2000, config.getMaxTextLength());
        assertEquals(15, config.getMinTextLength());
        assertNotNull(config.getRateLimit());
        assertEquals(10, config.getRateLimit().getRequestsPerMinute());
    }

    @Test
    void testSettersAndGetters() {
        SceneImageConfig config = new SceneImageConfig();
        
        config.setDefaultSize("512x512");
        config.setMaxTextLength(1000);
        config.setMinTextLength(10);
        
        SceneImageConfig.RateLimit rateLimit = new SceneImageConfig.RateLimit();
        rateLimit.setRequestsPerMinute(5);
        config.setRateLimit(rateLimit);

        assertEquals("512x512", config.getDefaultSize());
        assertEquals(1000, config.getMaxTextLength());
        assertEquals(10, config.getMinTextLength());
        assertEquals(5, config.getRateLimit().getRequestsPerMinute());
    }

    @Test
    void testRateLimitDefaults() {
        SceneImageConfig.RateLimit rateLimit = new SceneImageConfig.RateLimit();
        assertEquals(10, rateLimit.getRequestsPerMinute());
    }
}

