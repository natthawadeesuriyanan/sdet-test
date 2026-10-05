package com.sdet.gorest.client;

import com.sdet.gorest.data.TestDataRegistry;
import io.restassured.response.Response;

import java.util.Map;

/**
 * Thin, intention-revealing client for /users. Returns raw {@link Response}s so tests can assert
 * on failures too. Every 201 is registered for cleanup immediately - before any assertion can fail.
 */
public class UsersClient extends ApiClient {

    private static final String USERS = "/users";
    private static final String USER_BY_ID = "/users/{id}";

    // ---- create ----
    public Response create(Object body) {
        return create(body, Auth.TOKEN);
    }

    public Response create(Object body, Auth auth) {
        Response r = execute("POST", () -> request(auth).body(body).post(USERS));
        registerIfCreated(r);
        return r;
    }

    /** Sends a raw body with an arbitrary content type (malformed JSON, wrong media type...). */
    public Response createRaw(String rawBody, String contentType, Auth auth) {
        Response r = execute("POST", () -> request(auth).contentType(contentType).body(rawBody).post(USERS));
        registerIfCreated(r);
        return r;
    }

    // ---- read ----
    public Response get(Object id) {
        return get(id, Auth.TOKEN);
    }

    public Response get(Object id, Auth auth) {
        return execute("GET", () -> request(auth).pathParam("id", id).get(USER_BY_ID));
    }

    public Response list(Map<String, ?> query) {
        return list(query, Auth.TOKEN);
    }

    public Response list(Map<String, ?> query, Auth auth) {
        return execute("GET", () -> request(auth).queryParams(query).get(USERS));
    }

    // ---- update ----
    public Response update(Object id, Object body) {
        return update(id, body, Auth.TOKEN);
    }

    public Response update(Object id, Object body, Auth auth) {
        return execute("PUT", () -> request(auth).pathParam("id", id).body(body).put(USER_BY_ID));
    }

    // ---- delete ----
    public Response delete(Object id) {
        return delete(id, Auth.TOKEN);
    }

    public Response delete(Object id, Auth auth) {
        Response r = execute("DELETE", () -> request(auth).pathParam("id", id).delete(USER_BY_ID));
        if (auth == Auth.TOKEN && (r.statusCode() == 204 || r.statusCode() == 404) && id instanceof Number n) {
            TestDataRegistry.forget(n.longValue());
        }
        return r;
    }

    public Response deleteCollection() {
        return execute("DELETE", () -> request(Auth.TOKEN).delete(USERS));
    }

    private static void registerIfCreated(Response r) {
        if (r.statusCode() == 201) {
            Number id = r.path("id");
            if (id != null) {
                TestDataRegistry.register(id.longValue());
            }
        }
    }
}
