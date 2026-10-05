package com.sdet.gorest.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Optional;

/**
 * Central configuration. Lookup order for every key:
 * 1. JVM system property (-DGOREST_API_TOKEN=...)
 * 2. OS environment variable (used by CI)
 * 3. Local .env file in the project root (git-ignored, for local runs)
 */
public final class Config {

    public static final String TOKEN_KEY = "GOREST_API_TOKEN";

    private static final Dotenv DOTENV = Dotenv.configure()
            .ignoreIfMissing()
            .ignoreIfMalformed()
            .load();

    private Config() {
    }

    public static String baseUrl() {
        return value("GOREST_BASE_URL").orElse("https://gorest.co.in/public/v2");
    }

    public static Optional<String> token() {
        return value(TOKEN_KEY);
    }

    public static boolean hasToken() {
        return token().isPresent();
    }

    /** Returns the token or fails loudly. Never returns null / empty. */
    public static String requireToken() {
        return token().orElseThrow(MissingTokenException::new);
    }

    public static int maxRetries() {
        return Integer.parseInt(value("GOREST_MAX_RETRIES").orElse("4"));
    }

    public static long maxResponseTimeMs() {
        return Long.parseLong(value("GOREST_MAX_RESPONSE_TIME_MS").orElse("10000"));
    }

    public static boolean logAllExchanges() {
        return Boolean.parseBoolean(value("GOREST_LOG_ALL").orElse("false"));
    }

    static Optional<String> value(String key) {
        String v = System.getProperty(key);
        if (isBlank(v)) {
            v = System.getenv(key);
        }
        if (isBlank(v)) {
            v = DOTENV.get(key, null);
        }
        return isBlank(v) ? Optional.empty() : Optional.of(v.trim());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
