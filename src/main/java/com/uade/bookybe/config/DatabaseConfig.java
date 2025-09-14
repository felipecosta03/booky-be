package com.uade.bookybe.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final String JDBC_POSTGRESQL_PREFIX = "jdbc:postgresql://";
    private static final int DEFAULT_POSTGRESQL_PORT = 5432;

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "prod")
    public DataSource prodDataSource() {
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            throw new IllegalStateException("DATABASE_URL environment variable is required in production");
        }

        try {
            DatabaseUrlComponents components = parseDatabaseUrl(databaseUrl);

            logger.info("Connecting to database: {}:{}/{}", components.host, components.port, components.database);
            logger.debug("Username: {}", components.username);

            return DataSourceBuilder.create()
                    .url(components.jdbcUrl)
                    .username(components.username)
                    .password(components.password)
                    .driverClassName("org.postgresql.Driver")
                    .build();

        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid DATABASE_URL format: " + databaseUrl, e);
        }
    }

    private DatabaseUrlComponents parseDatabaseUrl(String url) throws URISyntaxException {
        URI dbUri = new URI(url);

        String username = "";
        String password = "";

        if (dbUri.getUserInfo() != null) {
            String[] userInfo = dbUri.getUserInfo().split(":");
            username = userInfo[0];
            if (userInfo.length > 1) {
                password = userInfo[1];
            }
        }

        String host = dbUri.getHost();
        int port = dbUri.getPort() != -1 ? dbUri.getPort() : DEFAULT_POSTGRESQL_PORT;
        String database = dbUri.getPath().substring(1); // Remove leading slash

        String jdbcUrl = String.format("%s%s:%d/%s", JDBC_POSTGRESQL_PREFIX, host, port, database);

        return new DatabaseUrlComponents(jdbcUrl, username, password, host, port, database);
    }

    private static class DatabaseUrlComponents {
        final String jdbcUrl;
        final String username;
        final String password;
        final String host;
        final int port;
        final String database;

        DatabaseUrlComponents(String jdbcUrl, String username, String password, String host, int port, String database) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
            this.host = host;
            this.port = port;
            this.database = database;
        }
    }
}
