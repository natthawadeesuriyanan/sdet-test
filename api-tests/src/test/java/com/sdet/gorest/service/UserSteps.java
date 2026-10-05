package com.sdet.gorest.service;

import com.sdet.gorest.client.UsersClient;
import com.sdet.gorest.data.UserFactory;
import com.sdet.gorest.model.User;
import io.restassured.response.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.sdet.gorest.assertions.ApiAssertions.assertStatus;

/**
 * Business-level steps used for test setup and verification ("arrange" and "re-read" steps).
 * They assert the happy path so that setup failures are reported as such, not as confusing
 * downstream failures.
 */
public class UserSteps {

    private final UsersClient client;

    public UserSteps(UsersClient client) {
        this.client = client;
    }

    public User createUser(User payload) {
        return assertStatus(client.create(payload), 201).as(User.class);
    }

    public User createRandomUser() {
        return createUser(UserFactory.randomUser());
    }

    public User getUser(long id) {
        return assertStatus(client.get(id), 200).as(User.class);
    }

    /** Produces an id that is guaranteed not to exist for this token - no hardcoded "999999". */
    public long idOfDeletedUser() {
        User user = createRandomUser();
        assertStatus(client.delete(user.getId()), 204);
        return user.getId();
    }

    /** Exact-match lookup; the API filter is a partial match, so we narrow it client side. */
    public List<User> findByEmail(String email) {
        Response response = assertStatus(client.list(Map.of("email", email)), 200);
        return Arrays.stream(response.as(User[].class))
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .toList();
    }
}
