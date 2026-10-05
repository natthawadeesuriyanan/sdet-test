package com.sdet.gorest.http;

/** One recorded request/response pair. {@code dump} is a human-readable, secret-masked transcript. */
public record HttpExchange(String method, String uri, int status, long durationMs, String dump) {

    public String summary() {
        return method + " " + uri + " -> " + status + " (" + durationMs + " ms)";
    }
}
