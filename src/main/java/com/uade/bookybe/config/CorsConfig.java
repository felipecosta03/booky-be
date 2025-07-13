package com.uade.bookybe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Permitir orígenes específicos
        configuration.addAllowedOriginPattern("*"); // En producción, especificar orígenes exactos
        
        // Permitir todos los métodos HTTP
        configuration.addAllowedMethod("*");
        
        // Permitir todos los headers
        configuration.addAllowedHeader("*");
        
        // Permitir credentials
        configuration.setAllowCredentials(true);
        
        // Configurar headers expuestos
        configuration.addExposedHeader("Authorization");
        configuration.addExposedHeader("Content-Type");
        
        // Aplicar a todas las rutas
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
} 