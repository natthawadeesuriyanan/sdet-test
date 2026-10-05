package com.sdet.gorest.tests;

import com.sdet.gorest.client.Auth;
import com.sdet.gorest.data.UserFactory;
import com.sdet.gorest.extensions.RequiresToken;
import com.sdet.gorest.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

import static com.sdet.gorest.assertions.ApiAssertions.assertErrorMessage;
import static org.assertj.core.api.Assertions.assertThat;

/** Writes must be authenticated, and a rejected write must have no side effect. */
@RequiresToken
@Tag("auth")
@DisplayName("Users - authentication and data isolation")
class UserAuthTests extends BaseApiTest {

    @ParameterizedTest(name = "POST with auth={0} -> 401")
    @EnumSource(value = Auth.class, names = {"NONE", "INVALID"})
   // @DisplayName("POST /users without a valid token -> 401 and no user is created")
    void post_user_without_token(Auth auth) {
        User payload = UserFactory.randomUser();
        assertErrorMessage(users.create(payload, auth), 401);
        assertThat(userSteps.findByEmail(payload.getEmail())).as("no side effect").isEmpty();
    }

     static Stream<Arguments> unauthorisedWrite() {
        return Stream.of(
                Arguments.of(Auth.NONE, 404),
                Arguments.of(Auth.INVALID, 401));
    }

    @ParameterizedTest(name = "PUT with auth={0} -> {1}")
    @MethodSource("unauthorisedWrite")
   // @DisplayName("PUT /users/{id} without a valid token is rejected (401 / 404) and the user is unchanged")
    void put_without_token(Auth auth, int expectedStatus) {
        User original = userSteps.createRandomUser();
        assertErrorMessage(users.update(original.getId(), UserFactory.differentFrom(original), auth), expectedStatus);
        assertThat(userSteps.getUser(original.getId())).isEqualTo(original);
    }

    @ParameterizedTest(name = "DELETE with auth={0} -> {1}")
    @MethodSource("unauthorisedWrite")
    //@DisplayName("DELETE /users/{id} without a valid token is rejected (401 / 404) and the user still exists")
    void delete_without_token(Auth auth, int expectedStatus) {
        User original = userSteps.createRandomUser();

        assertErrorMessage(users.delete(original.getId(), auth), expectedStatus);
        assertThat(userSteps.getUser(original.getId())).isEqualTo(original);
    }

    @Test
  //  @DisplayName("POST /users A user created with our token is invisible to anonymous readers (GoREST per-token isolation)")
    void other_can_not_see_mycreated_user() {
        User created = userSteps.createRandomUser();

        assertErrorMessage(users.get(created.getId(), Auth.NONE), 404);
    }
}
