package com.sdet.gorest.config;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** A unique id per JVM run, embedded in every test record so data never collides across reruns. */
public final class RunContext {

    private static final String RUN_ID = DateTimeFormatter.ofPattern("ddMMyyHHmmss")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())
            + UUID.randomUUID().toString().substring(0, 4);

    private RunContext() {
    }

    public static String runId() {
        return RUN_ID;
    }
}
