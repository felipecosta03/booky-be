package com.uade.bookybe.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {

    @Test
    void parseDatabaseUrl_ValidUrl() throws Exception {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "databaseUrl", 
            "postgres://user:pass@localhost:5432/testdb");

        // Test through parseDatabaseUrl reflection
        var method = DatabaseConfig.class.getDeclaredMethod("parseDatabaseUrl", String.class);
        method.setAccessible(true);
        
        var result = method.invoke(config, "postgres://user:pass@localhost:5432/testdb");
        assertNotNull(result);
    }

    @Test
    void parseDatabaseUrl_WithoutPort() throws Exception {
        DatabaseConfig config = new DatabaseConfig();
        
        var method = DatabaseConfig.class.getDeclaredMethod("parseDatabaseUrl", String.class);
        method.setAccessible(true);
        
        var result = method.invoke(config, "postgres://user:pass@localhost/testdb");
        assertNotNull(result);
    }

    @Test
    void prodDataSource_EmptyUrl() {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "databaseUrl", "");

        assertThrows(IllegalStateException.class, () -> config.prodDataSource());
    }

    @Test
    void prodDataSource_NullUrl() {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "databaseUrl", null);

        assertThrows(IllegalStateException.class, () -> config.prodDataSource());
    }
}


