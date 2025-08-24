package com.uade.bookybe.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class OpenApiConfig {

  @Value("${app.openapi.dev-url:http://localhost:8080}")
  private String devUrl;

  @Value("${app.openapi.prod-url:http://52.15.181.167:8080}")
  private String prodUrl;

  private final Environment environment;

  public OpenApiConfig(Environment environment) {
    this.environment = environment;
  }

  @Bean
  public OpenAPI bookiOpenAPI() {
    // Determinar qué servidor mostrar basado en el perfil activo
    boolean isProduction = List.of(environment.getActiveProfiles()).contains("prod");

    Server currentServer = new Server();
    if (isProduction) {
      currentServer.setUrl(prodUrl);
      currentServer.setDescription("Production Server");
    } else {
      currentServer.setUrl(devUrl);
      currentServer.setDescription("Development Server");
    }

    Contact contact = new Contact();
    contact.setEmail("support@booky.com");
    contact.setName("Booky Team");
    contact.setUrl("https://booky.com");

    License mitLicense =
        new License().name("MIT License").url("https://choosealicense.com/licenses/mit/");

    Info info =
        new Info()
            .title("Booky API")
            .version("1.0")
            .contact(contact)
            .description("API for Booky - Book management and social reading platform")
            .license(mitLicense);

    SecurityScheme securityScheme =
        new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .in(SecurityScheme.In.HEADER)
            .name("Authorization");

    SecurityRequirement securityRequirement =
        new SecurityRequirement().addList("Bearer Authentication");

    return new OpenAPI()
        .info(info)
        .servers(List.of(currentServer))
        .addSecurityItem(securityRequirement)
        .schemaRequirement("Bearer Authentication", securityScheme);
  }

  @Bean
  public ModelResolver modelResolver(ObjectMapper objectMapper) {
    return new ModelResolver(objectMapper);
  }
}
