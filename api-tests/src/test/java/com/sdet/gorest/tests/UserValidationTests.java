package com.sdet.gorest.tests;

import com.sdet.gorest.client.Auth;
import com.sdet.gorest.data.UserFactory;
import com.sdet.gorest.extensions.RequiresToken;
import com.sdet.gorest.model.User;
import com.sdet.gorest.model.ValidationError;
import io.restassured.response.Response;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import static com.sdet.gorest.assertions.ApiAssertions.assertClientError;
import static com.sdet.gorest.assertions.ApiAssertions.assertStatus;
import static com.sdet.gorest.assertions.ApiAssertions.assertUserResponse;
import static com.sdet.gorest.assertions.ApiAssertions.assertValidationError;
import static org.assertj.core.api.Assertions.assertThat;

@RequiresToken
@Tag("validation")
@DisplayName("Users - validation errors")
class UserValidationTests extends BaseApiTest {

    // ---------- create ----------
    static Stream<Arguments> missingRequiredField() {
        return Stream.of(
                Arguments.of("name", (UnaryOperator<User>) u -> u.withName(null)),
                Arguments.of("email", (UnaryOperator<User>) u -> u.withEmail(null)),
                Arguments.of("gender", (UnaryOperator<User>) u -> u.withGender((String) null)),
                Arguments.of("status", (UnaryOperator<User>) u -> u.withStatus((String) null)));
    }

    @ParameterizedTest(name = "missing ''{0}'' -> 422 can't be blank")
    @MethodSource("missingRequiredField")
    @DisplayName("POST without a required field -> 422 naming that field; nothing persisted")
    void post_missingRequiredField(String field, UnaryOperator<User> removeField) {
        User payload = removeField.apply(UserFactory.randomUser());
        assertValidationError(users.create(payload), field, "can't be blank");

        if (payload.getEmail() != null) {
            assertThat(userSteps.findByEmail(payload.getEmail())).as("no user persisted").isEmpty();
        }
    }

    @Test
    @DisplayName("POST {} -> 422 listing all four required fields at once")
    void empty_body_error_every_required_field() {
        Response response = users.createRaw("{}", "application/json", Auth.TOKEN);

        List<ValidationError> errors = assertValidationError(response, "email", "can't be blank");
        assertThat(errors).extracting(ValidationError::getField)
                .contains("name", "email", "gender", "status");
    }

    @ParameterizedTest(name = "email ''{0}'' -> 422 is invalid")
    @ValueSource(strings = {"plainaddress", "missing-at-sign.example.com", "user@", "@example.com", "two@@example.com"})
    @DisplayName("POST with malformed email -> 422 'is invalid'")
    void invalid_email_format(String badEmail) {
        assertValidationError(users.create(UserFactory.randomUser().withEmail(badEmail)), "email", "is invalid");
    }

    static Stream<Arguments> invalidEnumValue() {
        return Stream.of(
                Arguments.of("gender", (UnaryOperator<User>) u -> u.withGender("unknown")),
                Arguments.of("gender", (UnaryOperator<User>) u -> u.withGender("")),
                Arguments.of("status", (UnaryOperator<User>) u -> u.withStatus("deleted")),
                Arguments.of("status", (UnaryOperator<User>) u -> u.withStatus("")));
    }

    @ParameterizedTest(name = "[{index}] invalid ''{0}'' -> 422")
    @MethodSource("invalidEnumValue")
    @DisplayName("POST with value outside the gender/status enum -> 422 on that field")
    void create_user_with_invalidEnumValue(String field, UnaryOperator<User> mutate) {
        User payload = mutate.apply(UserFactory.randomUser());

        assertValidationError(users.create(payload), field, null);
        assertThat(userSteps.findByEmail(payload.getEmail())).as("no user persisted").isEmpty();
    }

    @Test
    @DisplayName("POST with whitespace-only name -> 422 (blank is not a value)")
    void create_user_with_whitespace_name_is_blank() {
        assertValidationError(users.create(UserFactory.randomUser().withName("   ")), "name", "can't be blank");
    }

    @Test
    @DisplayName("POST with an email already in use -> 422 'has already been taken'; still exactly one user")
    void duplicate_email_rejected() {
        User existing = userSteps.createRandomUser();

        Response r = users.create(UserFactory.randomUser().withEmail(existing.getEmail()));

        assertValidationError(r, "email", "has already been taken");
        assertThat(userSteps.findByEmail(existing.getEmail())).hasSize(1);
    }

    @Disabled("KNOWN DEFECT: case-variant duplicate email is accepted (201). See README > Findings.")
    @Test
    @DisplayName("POST with an email differing only by case -> 422 duplicate email")
    void duplicate_email_ignores_case() {
        User existing = userSteps.createRandomUser();
        String uppercaseEmail = existing.getEmail().toUpperCase(Locale.ROOT);
        Response response = users.create(UserFactory.randomUser().withEmail(uppercaseEmail));

        assertValidationError(response, "email", "has already been taken");
        assertThat(userSteps.findByEmail(existing.getEmail()))
                .as("case-variant duplicate must not create another user")
                .hasSize(1);
    }

    @ParameterizedTest(name = "name length {0} -> success or client validation error, never 5xx")
    @ValueSource(ints = {255, 256, 10_000})
    @DisplayName("POST names at and beyond likely length boundaries never cause a server error")
    void name_length_boundaries_never_return_server_error(int length) {
        User payload = UserFactory.randomUser().withName("N".repeat(length));

        Response response = users.create(payload);

        if (response.statusCode() == 201) {
            User created = assertUserResponse(response, 201);
            assertThat(created.getName()).hasSize(length).isEqualTo(payload.getName());
            assertThat(userSteps.getUser(created.getId()).getName()).isEqualTo(payload.getName());
        } else {
            assertClientError(response);
            assertThat(userSteps.findByEmail(payload.getEmail()))
                    .as("a rejected long name must not be persisted")
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("POST with malformed JSON -> 400, never 5xx")
    void malformed_json_is_400() {
        Response r = users.createRaw("{\"name\": \"broken\", \"email\": ", "application/json", Auth.TOKEN);

        assertClientError(r);
        assertStatus(r, 400);
    }

    @Test
    @DisplayName("POST with unsupported Content-Type (text/plain) -> rejected (docs: 415, live: 422); nothing persisted")
    void unsupported_media_type_is_rejected() {
        String email = UserFactory.uniqueEmail();
        Response r = users.createRaw("name=x&email=" + email + "&gender=male&status=active", "text/plain", Auth.TOKEN);

        assertClientError(r);
        assertThat(r.statusCode()).as("documented 415, observed 422").isIn(415, 422);
        assertThat(userSteps.findByEmail(email)).as("no user persisted").isEmpty();
    }

    // ---------- update ----------

    @Test
    @DisplayName("PUT with invalid email -> 422 and the stored user is unchanged (no partial write)")
    void update_with_invalid_email_does_not_persist() {
        User original = userSteps.createRandomUser();
        User bad = UserFactory.differentFrom(original).withEmail("not-an-email");

        assertValidationError(users.update(original.getId(), bad), "email", "is invalid");

        assertThat(userSteps.getUser(original.getId())).as("record untouched").isEqualTo(original);
    }

    @Test
    @DisplayName("PUT changing email to another user's email -> 422 taken; both users unchanged")
    void update_to_duplicate_email_rejected() {
        User first = userSteps.createRandomUser();
        User second = userSteps.createRandomUser();

        Response r = users.update(second.getId(), second.withEmail(first.getEmail()).withId(null));

        assertValidationError(r, "email", "has already been taken");
        assertThat(userSteps.getUser(first.getId())).isEqualTo(first);
        assertThat(userSteps.getUser(second.getId())).isEqualTo(second);
    }
}
