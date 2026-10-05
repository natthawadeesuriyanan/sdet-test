package com.sdet.gorest.http;

import com.sdet.gorest.config.Config;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * REST Assured filter that records every HTTP exchange of the current test (thread).
 * Nothing is printed for passing tests; when a test fails, {@code GoRestExtension}
 * dumps the full request + response transcript so the failure can be reproduced.
 * The Authorization header is always masked.
 */
public final class HttpExchangeRecorder implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(HttpExchangeRecorder.class);
    private static final int MAX_EXCHANGES_PER_TEST = 50;
    private static final int MAX_BODY_CHARS = 4_000;
    private static final ThreadLocal<Deque<HttpExchange>> EXCHANGES = ThreadLocal.withInitial(ArrayDeque::new);

    @Override
    public Response filter(FilterableRequestSpecification req,
                           FilterableResponseSpecification resSpec,
                           FilterContext ctx) {
        long start = System.nanoTime();
        Response res = ctx.next(req, resSpec);
        long ms = (System.nanoTime() - start) / 1_000_000;

        HttpExchange exchange = new HttpExchange(req.getMethod(), req.getURI(), res.statusCode(), ms, format(req, res, ms));
        Deque<HttpExchange> deque = EXCHANGES.get();
        if (deque.size() >= MAX_EXCHANGES_PER_TEST) {
            deque.removeFirst();
        }
        deque.addLast(exchange);

        if (Config.logAllExchanges()) {
            LOG.info("\n{}", exchange.dump());
        } else {
            LOG.debug(exchange.summary());
        }
        return res;
    }

    public static List<HttpExchange> snapshot() {
        return List.copyOf(EXCHANGES.get());
    }

    public static Optional<HttpExchange> last() {
        return Optional.ofNullable(EXCHANGES.get().peekLast());
    }

    public static void clear() {
        EXCHANGES.get().clear();
    }

    private static String format(FilterableRequestSpecification req, Response res, long ms) {
        StringBuilder sb = new StringBuilder();
        sb.append(">>> REQUEST  ").append(req.getMethod()).append(' ').append(req.getURI()).append('\n');
        for (Header h : req.getHeaders()) {
            sb.append("    ").append(h.getName()).append(": ").append(mask(h)).append('\n');
        }
        Object body = req.getBody();
        if (body != null) {
            sb.append("    Body: ").append(truncate(String.valueOf(body))).append('\n');
        }
        sb.append("<<< RESPONSE ").append(res.getStatusLine()).append("  [").append(ms).append(" ms]\n");
        for (Header h : res.getHeaders()) {
            sb.append("    ").append(h.getName()).append(": ").append(h.getValue()).append('\n');
        }
        String responseBody = prettyBody(res);
        sb.append("    Body: ").append(responseBody.isBlank() ? "<empty>" : truncate(responseBody)).append('\n');
        return sb.toString();
    }

    private static String prettyBody(Response res) {
        try {
            return res.getBody().asPrettyString();
        } catch (Exception e) {
            return res.getBody().asString();
        }
    }

    private static String mask(Header h) {
        if (!"Authorization".equalsIgnoreCase(h.getName())) {
            return h.getValue();
        }
        String v = h.getValue();
        return v.length() <= 12 ? "****" : v.substring(0, 7) + "****" + v.substring(v.length() - 4);
    }

    private static String truncate(String s) {
        return s.length() <= MAX_BODY_CHARS ? s : s.substring(0, MAX_BODY_CHARS) + "... <truncated " + (s.length() - MAX_BODY_CHARS) + " chars>";
    }
}
