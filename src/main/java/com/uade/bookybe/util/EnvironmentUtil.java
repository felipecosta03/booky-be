package com.uade.bookybe.util;

import java.util.Optional;

public final class EnvironmentUtil {

  public static final String SCOPE = "SCOPE";
  public static final String LOCAL = "local";

  private EnvironmentUtil() {}

  public static void setEnv(String key, String value) {
    System.setProperty(key, value);
  }

  public static void setScope() {
    String scope = getEnvOrDefault(SCOPE, LOCAL);
    setEnv(SCOPE, scope);
  }

  public static String getEnv(String key) {
    return System.getenv(key);
  }

  public static String getEnvOrDefault(String key, String defaultValue) {
    return Optional.ofNullable(getEnv(key)).orElse(defaultValue);
  }
}
