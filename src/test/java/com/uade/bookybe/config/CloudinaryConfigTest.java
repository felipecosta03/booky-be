package com.uade.bookybe.config;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class CloudinaryConfigTest {

    @Test
    void cloudinary_WithoutCredentials_ReturnsDefaultInstance() {
        CloudinaryConfig config = new CloudinaryConfig();
        
        Cloudinary cloudinary = config.cloudinary();
        
        assertNotNull(cloudinary);
    }

    @Test
    void cloudinary_WithCredentials_ReturnsConfiguredInstance() {
        CloudinaryConfig config = new CloudinaryConfig();
        ReflectionTestUtils.setField(config, "cloudName", "test-cloud");
        ReflectionTestUtils.setField(config, "apiKey", "test-key");
        ReflectionTestUtils.setField(config, "apiSecret", "test-secret");
        
        Cloudinary cloudinary = config.cloudinary();
        
        assertNotNull(cloudinary);
    }
}


