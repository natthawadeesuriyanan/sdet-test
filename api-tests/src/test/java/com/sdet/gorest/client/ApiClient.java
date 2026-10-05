package com.sdet.gorest.client;

import com.sdet.gorest.config.Config;
import com.sdet.gorest.http.HttpExchangeRecorder;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Transport layer shared by every resource client:
 * base spec, authentication modes, exchange recording, and rate-limit aware retries.
 */
public abstract class ApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(ApiClient.class);
    private static final Set<String> IDEMPOTENT = Set.of("GET", "PUT", "DELETE");
    private static final long MAX_WAIT_MS = 65_000;
    private static final RequestSpecification BASE_SPEC = buildBaseSpec();

    private static RequestSpecification buildBaseSpec() {
        URI uri = URI.create(Config.baseUrl());
        RestAssuredConfig config = RestAssuredConfig.config()
                .encoderConfig(EncoderConfig.encoderConfig()
                        .defaultCharsetForContentType(StandardCharsets.UTF_8.name(), ContentType.JSON))
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", 15_000)
                        .setParam("http.socket.timeout", 30_000));
        return new RequestSpecBuilder()
                .setBaseUri(uri.getScheme() + "://" + uri.getAuthority())
                .setBasePath(uri.getPath())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(config)
                .addFilter(new HttpExchangeRecorder())
                .build();
    }

    protected RequestSpecification request(Auth auth) {
        RequestSpecification spec = RestAssured.given().spec(BASE_SPEC);
        return switch (auth) {
            case TOKEN -> spec.header("Authorization", "Bearer " + Config.requireToken());
            case INVALID -> spec.header("Authorization", "Bearer invalid-" + UUID.randomUUID());
            case NONE -> spec;
        };
    }

    /**
     * Executes a call, retrying on 429 (any method - the request was not processed) and on
     * 502/503/504 for idempotent methods only. Waits for Retry-After / X-RateLimit-Reset when
     * present, otherwise exponential back-off with jitter. Every attempt is recorded.
     */
    protected Response execute(String method, Supplier<Response> call) {
        int maxRetries = Config.maxRetries();
        for (int attempt = 0; ; attempt++) {
            Response response = call.get();
            int status = response.statusCode();
            boolean retryable = status == 429 || (IDEMPOTENT.contains(method) && status >= 502 && status <= 504);

            if (!retryable) {
                pauseIfBudgetExhausted(response);
                return response;
            }
            if (attempt >= maxRetries) {
                LOG.warn("{} still returned HTTP {} after {} retries - giving up", method, status, maxRetries);
                return response;
            }
            Duration wait = waitFromHeaders(response)
                    .orElse(Duration.ofMillis((long) (1_000 * Math.pow(2, attempt))));
            wait = wait.plusMillis(ThreadLocalRandom.current().nextLong(100, 500));
            LOG.warn("HTTP {} on {} - retry {}/{} in {} ms", status, method, attempt + 1, maxRetries, wait.toMillis());
            sleep(wait);
        }
    }

    /** Proactive throttle: if the per-minute budget is nearly gone, wait instead of hitting 429. */
    private static void pauseIfBudgetExhausted(Response response) {
        Long remaining = parseLong(response.header("X-RateLimit-Remaining"));
        if (remaining != null && remaining <= 1) {
            Duration wait = waitFromHeaders(response).orElse(Duration.ofSeconds(5));
            LOG.info("Rate-limit budget almost exhausted (remaining={}), pausing {} ms", remaining, wait.toMillis());
            sleep(wait);
        }
    }

    private static Optional<Duration> waitFromHeaders(Response response) {
        Long seconds = parseLong(response.header("Retry-After"));
        if (seconds == null) {
            seconds = parseLong(response.header("X-RateLimit-Reset"));
        }
        if (seconds == null || seconds < 0) {
            return Optional.empty();
        }
        // Defensive: treat a huge value as an epoch timestamp rather than a delta.
        if (seconds > 1_000_000_000L) {
            seconds = Math.max(0, seconds - System.currentTimeMillis() / 1000);
        }
        return Optional.of(Duration.ofMillis(Math.min(seconds * 1000, MAX_WAIT_MS)));
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while backing off", e);
        }
    }
}
