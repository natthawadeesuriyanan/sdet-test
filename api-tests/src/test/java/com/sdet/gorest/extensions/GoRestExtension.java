package com.sdet.gorest.extensions;

import com.sdet.gorest.config.Config;
import com.sdet.gorest.config.RunContext;
import com.sdet.gorest.data.UserCleaner;
import com.sdet.gorest.http.HttpExchange;
import com.sdet.gorest.http.HttpExchangeRecorder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Test lifecycle glue:
 * - beforeAll : fail fast and loudly if a @RequiresToken class runs without a token
 * - beforeEach: reset the HTTP recorder
 * - afterEach : snapshot the test's exchanges, then delete every user the test created
 *               (runs even when the test failed)
 * - afterAll  : second cleanup pass for anything that failed to delete earlier
 * - testFailed: print the full request/response transcript of the failed test
 */
public class GoRestExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback,
        AfterAllCallback, TestWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(GoRestExtension.class);
    private static final Map<String, List<HttpExchange>> EXCHANGES_BY_TEST = new ConcurrentHashMap<>();
    private static volatile boolean bannerPrinted;

    @Override
    public void beforeAll(ExtensionContext ctx) {
        if (!bannerPrinted) {
            bannerPrinted = true;
            LOG.info("GoREST run id={} baseUrl={} token={}", RunContext.runId(), Config.baseUrl(),
                    Config.hasToken() ? "present" : "MISSING");
        }
        if (AnnotationSupport.isAnnotated(ctx.getTestClass(), RequiresToken.class)) {
            Config.requireToken(); // throws MissingTokenException -> every test in the class FAILS
        }
    }

    @Override
    public void beforeEach(ExtensionContext ctx) {
        HttpExchangeRecorder.clear();
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        EXCHANGES_BY_TEST.put(ctx.getUniqueId(), HttpExchangeRecorder.snapshot());
        List<Long> leftovers = UserCleaner.cleanUp("afterEach: " + ctx.getDisplayName());
        if (!leftovers.isEmpty()) {
            LOG.warn("Users not yet cleaned up (will retry in afterAll): {}", leftovers);
        }
    }

    @Override
    public void afterAll(ExtensionContext ctx) {
        List<Long> leftovers = UserCleaner.cleanUp("afterAll: " + ctx.getDisplayName());
        if (!leftovers.isEmpty()) {
            LOG.error("LEAKED test users (GoREST daily reset will purge them): {}", leftovers);
        }
    }

    @Override
    public void testFailed(ExtensionContext ctx, Throwable cause) {
        List<HttpExchange> exchanges = EXCHANGES_BY_TEST.remove(ctx.getUniqueId());
        if (exchanges == null) {
            exchanges = HttpExchangeRecorder.snapshot();
        }
        String transcript = exchanges.isEmpty()
                ? "  <no HTTP exchanges>"
                : exchanges.stream().map(HttpExchange::dump).collect(Collectors.joining("\n"));
        LOG.error("""

                ==================== FAILED: {} ====================
                Cause: {}
                {} HTTP exchange(s) in this test:
                {}
                ===================================================================""",
                ctx.getDisplayName(), cause.toString(), exchanges.size(), transcript);
    }

    @Override
    public void testSuccessful(ExtensionContext ctx) {
        EXCHANGES_BY_TEST.remove(ctx.getUniqueId());
    }

    @Override
    public void testAborted(ExtensionContext ctx, Throwable cause) {
        EXCHANGES_BY_TEST.remove(ctx.getUniqueId());
    }
}
