package com.uade.bookybe.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LivekitPropsTest {

    @Test
    void testGettersAndSetters() {
        LivekitProps props = new LivekitProps();

        props.setApiKey("testKey");
        props.setApiSecret("testSecret");
        props.setWsUrl("ws://test.com");
        props.setTokenTtlSeconds(300);

        assertEquals("testKey", props.getApiKey());
        assertEquals("testSecret", props.getApiSecret());
        assertEquals("ws://test.com", props.getWsUrl());
        assertEquals(300, props.getTokenTtlSeconds());
    }

    @Test
    void testDefaultTokenTtl() {
        LivekitProps props = new LivekitProps();
        assertEquals(600, props.getTokenTtlSeconds());
    }
}

