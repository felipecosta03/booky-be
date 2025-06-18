package com.uade.bookybe.util;

import java.util.Optional;

public final class EnvironmentUtil {

    private EnvironmentUtil() {
    }

    public static String getEnv(String key) {
        return System.getenv(key);
    }

    public static String getEnvOrDefault(String key, String defaultValue) {
        return Optional.ofNullable(getEnv(key)).orElse(defaultValue);
    }
}
