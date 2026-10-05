package com.saucedemo.helper;

import java.util.concurrent.TimeUnit;

public final class TestMetrics {
    private TestMetrics() {
    }

    public static long elapsedMillis(long startNanos) {
        long elapsedNanos = System.nanoTime() - startNanos;
        return TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
    }

    public static void log(String scenario, long elapsedMillis, long limitMillis) {
    String status = (elapsedMillis <= limitMillis) ? "Passed" : "Failed (Outlier)";
    System.out.printf("[PERF] Scenario: %s | Time: %d ms | Limit: %d ms | Status: %s%n", 
    scenario, elapsedMillis, limitMillis, status);    }
}