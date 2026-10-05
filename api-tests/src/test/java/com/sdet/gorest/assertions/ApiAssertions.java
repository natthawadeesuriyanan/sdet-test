package com.sdet.gorest.assertions;

import com.sdet.gorest.config.Config;
import com.sdet.gorest.http.HttpExchange;
import com.sdet.gorest.http.HttpExchangeRecorder;
import com.sdet.gorest.model.ApiError;
import com.sdet.gorest.model.User;
import com.sdet.gorest.model.ValidationError;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;

import java.util.Arrays;
import java.util.List;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared, intention-revealing assertions. Status assertions embed the full HTTP exchange in the
 * failure message, so a red test is diagnosable from the report alone.
 */
public final class ApiAssertions {

    private ApiAssertions() {
    }

    public static Response assertStatus(Response r, int expected) {
        if (r.statusCode() != expected) {
            throw new AssertionError(String.format("Expected HTTP %d but got %d%n%s",
                    expected, r.statusCode(), lastExchangeDump()));
        }
        return r;
    }

    public static void assertClientError(Response r) {
        assertThat(r.statusCode())
                .as("Expected a 4xx client error (never a 5xx) %n%s", lastExchangeDump())
                .isBetween(400, 499);
    }

    public static void assertJsonContentType(Response r) {
        assertThat(r.contentType()).as("Content-Type").startsWith("application/json");
    }

    public static void assertSchema(Response r, String schemaClasspath) {
        r.then().assertThat().body(matchesJsonSchemaInClasspath(schemaClasspath));
    }

    public static void assertResponseTime(Response r) {
        assertThat(r.time())
                .as("Response time (ms) for %s", HttpExchangeRecorder.last().map(HttpExchange::summary).orElse("?"))
                .isLessThanOrEqualTo(Config.maxResponseTimeMs());
    }

    /** Full contract check of a single-user 2xx response: status, media type, schema, latency. */
    public static User assertUserResponse(Response r, int expectedStatus) {
        assertStatus(r, expectedStatus);
        assertJsonContentType(r);
        assertSchema(r, Schemas.USER);
        assertResponseTime(r);
        return r.as(User.class);
    }

    /** Field-by-field comparison of mutable fields, reported all at once (soft). */
    public static void assertUserMatches(User actual, User expected) {
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(actual.getName()).as("name").isEqualTo(expected.getName());
            s.assertThat(actual.getEmail()).as("email").isEqualToIgnoringCase(expected.getEmail());
            s.assertThat(actual.getGender()).as("gender").isEqualTo(expected.getGender());
            s.assertThat(actual.getStatus()).as("status").isEqualTo(expected.getStatus());
        });
    }

    /**
     * Asserts a 422 with the documented [{field, message}] shape that contains an error for
     * {@code field}. When {@code messageFragment} is null, any non-blank message is accepted.
     */
    public static List<ValidationError> assertValidationError(Response r, String field, String messageFragment) {
        assertStatus(r, 422);
        assertJsonContentType(r);
        assertSchema(r, Schemas.VALIDATION_ERRORS);
        List<ValidationError> errors = Arrays.asList(r.as(ValidationError[].class));
        assertThat(errors)
                .as("422 errors %s should contain field '%s' (message ~ '%s')", errors, field, messageFragment)
                .anySatisfy(e -> {
                    assertThat(e.getField()).isEqualTo(field);
                    assertThat(e.getMessage()).isNotBlank();
                    if (messageFragment != null) {
                        assertThat(e.getMessage()).containsIgnoringCase(messageFragment);
                    }
                });
        return errors;
    }

    public static ApiError assertErrorMessage(Response r, int expectedStatus) {
        assertStatus(r, expectedStatus);
        assertJsonContentType(r);
        assertSchema(r, Schemas.ERROR);
        ApiError error = r.as(ApiError.class);
        assertThat(error.getMessage()).as("error message").isNotBlank();
        return error;
    }

    private static String lastExchangeDump() {
        return HttpExchangeRecorder.last().map(HttpExchange::dump).orElse("<no HTTP exchange recorded>");
    }
}
