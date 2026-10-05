package com.sdet.gorest.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The ONLY source of ids that cleanup may delete: ids returned by our own 201 responses in this
 * run. This guarantees we never update/delete users we did not create.
 */
public final class TestDataRegistry {

    private static final Set<Long> CREATED = ConcurrentHashMap.newKeySet();

    static {
        // Last line of defence if the JVM exits before JUnit's after-callbacks (e.g. Ctrl+C).
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!CREATED.isEmpty()) {
                UserCleaner.cleanUp("shutdown-hook");
            }
        }, "gorest-cleanup-hook"));
    }

    private TestDataRegistry() {
    }

    public static void register(long id) {
        CREATED.add(id);
    }

    public static void forget(long id) {
        CREATED.remove(id);
    }

    public static boolean isOwned(long id) {
        return CREATED.contains(id);
    }

    public static List<Long> snapshot() {
        return List.copyOf(CREATED);
    }
}
