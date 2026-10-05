package com.sdet.gorest.tests;

import com.sdet.gorest.data.UserFactory;
import com.sdet.gorest.extensions.RequiresToken;
import com.sdet.gorest.model.User;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.sdet.gorest.assertions.ApiAssertions.assertErrorMessage;
import static com.sdet.gorest.assertions.ApiAssertions.assertStatus;
import static com.sdet.gorest.assertions.ApiAssertions.assertUserMatches;
import static com.sdet.gorest.assertions.ApiAssertions.assertUserResponse;
import static org.assertj.core.api.Assertions.assertThat;

@RequiresToken
@Tag("crud")
@DisplayName("Users - CRUD lifecycle")
class UserCrudTests extends BaseApiTest {

    @Test
    @DisplayName("POST/users -> 201, server-assigned id, payload echoed, schema valid")
    void post_user_returns_correct_response() {
        User payload = UserFactory.randomUser();
        Response response = users.create(payload);
        User created = assertUserResponse(response, 201);
        assertThat(created.getId()).as("server-assigned id").isNotNull().isPositive();
        assertUserMatches(created, payload);
    }

    @Test
    @DisplayName("GET/users/{id} returns exactly what was created (persistence, not just echo)")
    void get_user_returns_correct_record() {
        User created = userSteps.createRandomUser();
        User fetched = assertUserResponse(users.get(created.getId()), 200);
        assertThat(fetched).isEqualTo(created);
    }

    @Test
    @DisplayName("PUT/users/{id} replaces every mutable field; change is persisted and id is stable")
    void put_updates_all_existing_fields() {
        User original = userSteps.createRandomUser();
        User changes = UserFactory.differentFrom(original);
        User updated = assertUserResponse(users.update(original.getId(), changes), 200);
        assertThat(updated.getId()).as("id must not change on update").isEqualTo(original.getId());
        assertUserMatches(updated, changes);
        assertThat(userSteps.getUser(original.getId())).as("re-read after PUT").isEqualTo(updated);
    }

    @Test
    @DisplayName("DELETE/users/{id} -> 204 empty body; afterwards GET and DELETE are 404 (not idempotent-200)")
    void delete_user_then_get_deleted_again() {
        User created = userSteps.createRandomUser();
        Response response = users.delete(created.getId());
        assertStatus(response, 204);
        assertThat(response.asString()).as("204 must have no body").isEmpty();
        assertErrorMessage(users.get(created.getId()), 404);
        assertErrorMessage(users.delete(created.getId()), 404);
    }

    @Test
    @DisplayName("POST/users ignores a client-supplied id (no mass assignment / id hijack)")
    void create_user_ignores_provided_id() {
        User user1 = userSteps.createRandomUser();
        User payload = UserFactory.randomUser().withId(user1.getId());
        User user2 = assertUserResponse(users.create(payload), 201);
        assertThat(user2.getId()).as("server must assign its own id").isNotEqualTo(user1.getId());
        assertThat(userSteps.getUser(user1.getId())).as("existing user untouched").isEqualTo(user1);
    }

    @Test
    @DisplayName("PUT/users/{id} ignores an id in the body - the path id wins, the other user is untouched")
    void update_user_ignores_provided_id() {
        User user1 = userSteps.createRandomUser();
        User user2 = userSteps.createRandomUser();
        User changes = UserFactory.differentFrom(user1).withId(user2.getId());
        User updated = assertUserResponse(users.update(user1.getId(), changes), 200);
        assertThat(updated.getId()).isEqualTo(user1.getId());
        assertThat(userSteps.getUser(user2.getId())).as("user2 untouched").isEqualTo(user2);
    }

    @ParameterizedTest(name = "name round-trips unchanged: {0}")
    @ValueSource(strings = {"สมชาย ใจดี", "Rosé Müller-Øyster", "Pitér O'Connor-Łukasz"})
    @DisplayName("Unicode / special characters in name survive create + read (UTF-8 end to end)")
    void unicode_names_round_trip(String baseName) {
        User payload = UserFactory.randomUser().withName(baseName + " " + UserFactory.uniqueTag());
        User created = assertUserResponse(users.create(payload), 201);
        assertThat(created.getName()).isEqualTo(payload.getName());
        assertThat(userSteps.getUser(created.getId()).getName()).isEqualTo(payload.getName());
    }
}
