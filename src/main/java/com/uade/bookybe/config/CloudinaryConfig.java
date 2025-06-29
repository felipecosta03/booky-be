package com.uade.bookybe.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name:#{null}}")
    private String cloudName;

    @Value("${cloudinary.api-key:#{null}}")
    private String apiKey;

    @Value("${cloudinary.api-secret:#{null}}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        if (cloudName == null || apiKey == null || apiSecret == null) {
            // Si no hay configuración, crear una instancia vacía para evitar errores
            return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "demo",
                "api_key", "123456789012345",
                "api_secret", "abcdefghijklmnopqrstuvwxyz123456"
            ));
        }

        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret,
            "secure", true
        ));
    }
} 