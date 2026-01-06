package com.uade.bookybe.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentUtilTest {

    @Test
    void setEnv_ShouldSetSystemProperty() {
        String key = "TEST_KEY";
        String value = "TEST_VALUE";

        EnvironmentUtil.setEnv(key, value);

        assertEquals(value, System.getProperty(key));
        System.clearProperty(key);
    }

    @Test
    void setScope_ShouldSetScopeProperty() {
        EnvironmentUtil.setScope();

        assertNotNull(System.getProperty(EnvironmentUtil.SCOPE));
        System.clearProperty(EnvironmentUtil.SCOPE);
    }

    @Test
    void getEnv_ShouldReturnEnvironmentVariable() {
        String result = EnvironmentUtil.getEnv("PATH");

        assertNotNull(result);
    }

    @Test
    void getEnvOrDefault_ShouldReturnDefaultWhenKeyNotFound() {
        String defaultValue = "default";

        String result = EnvironmentUtil.getEnvOrDefault("NONEXISTENT_KEY_XYZ", defaultValue);

        assertEquals(defaultValue, result);
    }

    @Test
    void getEnvOrDefault_ShouldReturnEnvValueWhenExists() {
        String result = EnvironmentUtil.getEnvOrDefault("PATH", "default");

        assertNotNull(result);
        assertNotEquals("default", result);
    }
}

