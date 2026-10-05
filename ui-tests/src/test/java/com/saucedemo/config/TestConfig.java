package com.saucedemo.config;

import java.math.BigDecimal;

public final class TestConfig {
    public static final String PASSWORD = "secret_sauce";
    public static final BigDecimal TAX_RATE = new BigDecimal("0.08");

    private TestConfig() {
    }

    public static String baseUrl() {
        return setting("saucedemo.baseUrl", "SAUCEDEMO_BASE_URL", "https://www.saucedemo.com");
    }

    public static boolean headless() {
        return Boolean.parseBoolean(setting("saucedemo.headless", "SAUCEDEMO_HEADLESS", "false"));
    }

    public static int timeoutMs() {
        return integerSetting("saucedemo.timeoutMs", "SAUCEDEMO_TIMEOUT_MS", 10_000);
    }

    public static int performanceGlitchLoginMaxMs() {
        return integerSetting("saucedemo.performanceGlitchLoginMaxMs", "SAUCEDEMO_GLITCH_LOGIN_MAX_MS", 8_000);
    }

    public static int standardLoginMaxMs() {
        return integerSetting("saucedemo.standardLoginMaxMs", "SAUCEDEMO_STANDARD_LOGIN_MAX_MS", 2_000);
    }

    public static int performanceGlitchCheckoutMaxMs() {
        return integerSetting("saucedemo.performanceGlitchCheckoutMaxMs", "SAUCEDEMO_GLITCH_CHECKOUT_MAX_MS", 45_000);
    }

    public static boolean performanceAssertionsEnabled() {
        return Boolean.parseBoolean(setting("saucedemo.performanceAssertions", "SAUCEDEMO_PERFORMANCE_ASSERTIONS", "true"));
    }

    private static int integerSetting(String property, String environmentVariable, int fallback) {
        return Integer.parseInt(setting(property, environmentVariable, Integer.toString(fallback)));
    }

    private static String setting(String property, String environmentVariable, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariable);
        }
        return value == null || value.isBlank() ? fallback : value;
    }
}