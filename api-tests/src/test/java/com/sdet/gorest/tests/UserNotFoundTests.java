package com.sdet.gorest.tests;

import com.sdet.gorest.data.UserFactory;
import com.sdet.gorest.extensions.RequiresToken;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static com.sdet.gorest.assertions.ApiAssertions.assertClientError;
import static com.sdet.gorest.assertions.ApiAssertions.assertErrorMessage;

@RequiresToken
@Tag("edge")
@DisplayName("Users - not found and malformed ids")
class UserNotFoundTests extends BaseApiTest {

    @ParameterizedTest(name = "{0} on a deleted id -> 404")
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
  //  @DisplayName("Operations on an id that no longer exists -> 404 with error body")
    void get_put_delete_on_deleted_user(String method) {
        long deletedId = userSteps.idOfDeletedUser(); // dynamic, never a hardcoded "999999"

        Response response = switch (method) {
            case "GET" -> users.get(deletedId);
            case "PUT" -> users.update(deletedId, UserFactory.randomUser());
            case "DELETE" -> users.delete(deletedId);
            default -> throw new IllegalArgumentException(method);
        };

        assertErrorMessage(response, 404);
    }

    @ParameterizedTest(name = "GET /users/{0} -> 4xx")
    @ValueSource(strings = {"0", "-1", "abc", "1.5", "99999999999999999999999"})
   // @DisplayName("Malformed / out-of-range ids are a client error, never a 5xx")
    void get_invalid_ids(String id) {
        assertClientError(users.get(id));
    }
}
