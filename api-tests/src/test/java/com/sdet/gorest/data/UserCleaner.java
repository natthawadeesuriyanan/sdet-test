package com.sdet.gorest.data;

import com.sdet.gorest.client.UsersClient;
import com.sdet.gorest.config.Config;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/** Deletes every user registered by this run. Tolerates 404 (already gone / daily reset). */
public final class UserCleaner {

    private static final Logger LOG = LoggerFactory.getLogger(UserCleaner.class);

    private UserCleaner() {
    }

    public static synchronized List<Long> cleanUp(String phase) {
        List<Long> leftovers = new ArrayList<>();
        List<Long> ids = TestDataRegistry.snapshot();
        if (ids.isEmpty() || !Config.hasToken()) {
            return leftovers;
        }
        UsersClient client = new UsersClient();
        for (Long id : ids) {
            try {
                Response r = client.delete(id);
                if (r.statusCode() == 204 || r.statusCode() == 404) {
                    LOG.debug("[{}] cleaned up user {} (HTTP {})", phase, id, r.statusCode());
                } else {
                    leftovers.add(id);
                    LOG.warn("[{}] could not delete user {}: HTTP {} {}", phase, id, r.statusCode(), r.asString());
                }
            } catch (RuntimeException e) {
                leftovers.add(id);
                LOG.warn("[{}] could not delete user {}: {}", phase, id, e.toString());
            }
        }
        return leftovers;
    }
}
