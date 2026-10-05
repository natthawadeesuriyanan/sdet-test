package com.sdet.gorest.tests;

import com.sdet.gorest.assertions.Schemas;
import com.sdet.gorest.data.UserFactory;
import com.sdet.gorest.extensions.RequiresToken;
import com.sdet.gorest.model.User;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static com.sdet.gorest.assertions.ApiAssertions.assertClientError;
import static com.sdet.gorest.assertions.ApiAssertions.assertJsonContentType;
import static com.sdet.gorest.assertions.ApiAssertions.assertSchema;
import static com.sdet.gorest.assertions.ApiAssertions.assertStatus;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * List/search. Live shared data => never assert ordering, totals or specific
 * foreign records;
 * only assert properties that must hold for whatever the page contains.
 */
@RequiresToken
@Tag("query")
@DisplayName("Users - list, filter, pagination")
class UserQueryTests extends BaseApiTest {

    @Test
    @DisplayName("GET /users?email= finds our user, and every result honours the filter")
    void get_filter_by_email() {
        User myUser = userSteps.createRandomUser();
        String searchEmail = myUser.getEmail();

        // Act: search users by that email
        Response response = users.list(Map.of("email", searchEmail));
        List<User> results = okList(response);

        // Assert 1: the user we created is in the results
        assertThat(results).as("search results should include the user we just created")
                .contains(myUser);

        // Assert 2: every result matches the search (no unrelated users)
        for (User user : results) {
            assertThat(user.getEmail()).as("every result must match the email filter")
                    .containsIgnoringCase(searchEmail);
        }
    }

    @Test
    @DisplayName("GET /users?name= finds our user, and every result honours the filter")
    void get_filter_by_name() {
        User myUser = userSteps.createRandomUser();
        String searchName = myUser.getName();
        Response response = users.list(Map.of("name", searchName));
        List<User> results = okList(response);

        assertThat(results)
                .as("search results should include the user we just created")
                .contains(myUser);

        // Assert 2: every result matches the search (no unrelated users)
        for (User user : results) {
            assertThat(user.getName())
                    .as("every result must match the name filter")
                    .containsIgnoringCase(searchName);
        }
    }

    @Test
    @DisplayName("GET /users?email= with no matches -> 200 and an empty array")
    void email_filter_with_no_matches_returns_empty_list() {
        String absentEmail = UserFactory.uniqueEmail();
        List<User> results = okList(users.list(Map.of("email", absentEmail)));

        assertThat(results).as("no user should match a unique email").isEmpty();
    }

    /**
     * GoREST docs promise 405 for an unsupported verb on an existing path, but the
     * live service
     * answers 404 with an HTML page (no route at all). The invariant that matters -
     * the request is
     * rejected as a client error and nothing is deleted - is asserted strictly; the
     * deviation is
     * documented in README as a finding.
     */
    @Test
    @DisplayName("DELETE /users without an id is rejected (docs: 405, live: 404 HTML), never 5xx")
    void delete_users_collection_is_not_allowed() {
        // Arrange: a user that must survive the request
        User myUser = userSteps.createRandomUser();

        // Act: DELETE the whole collection
        Response response = users.deleteCollection();

        // Assert: rejected as a client error, never a server error
        assertClientError(response);
        assertThat(response.statusCode())
                .as("documented 405, observed 404")
                .isIn(404, 405);

        // Assert: nothing was deleted
        assertThat(userSteps.getUser(myUser.getId()))
                .as("user must still exist after DELETE /users")
                .isEqualTo(myUser);
    }

    @Test
    @DisplayName("GET /users?page=1&per_page=5 returns at most 5 users and matching X-Pagination headers")
    void verify_paging() {
        // Act: ask for page 1 with 5 users per page
        Response response = users.list(Map.of("page", 1, "per_page", 5));
        List<User> results = okList(response);

        // Assert: page size is respected
        assertThat(results)
                .as("page should not contain more users than per_page")
                .isNotEmpty()
                .hasSizeLessThanOrEqualTo(5);

        // Assert: pagination headers are present and correct (documented by GoREST)
        assertThat(response.header("X-Pagination-Limit"))
                .as("X-Pagination-Limit header")
                .isEqualTo("5");
        assertThat(response.header("X-Pagination-Page"))
                .as("X-Pagination-Page header")
                .isEqualTo("1");
    }

    @Test
     @DisplayName("per_page above the documented max (100) is capped, not an error")
    void per_page_is_capped_at_100() {
        assertThat(okList(users.list(Map.of("per_page", 500)))).hasSizeLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("A page beyond the last one -> 200 with an empty array (not 404 / 5xx)")
    void page_beyond_last_is_empty() {
        // Arrange: find out how many pages exist right now
        Response firstPage = users.list(Map.of("per_page", 100));
        assertStatus(firstPage, 200);
        long totalPages = Long.parseLong(firstPage.header("X-Pagination-Pages"));

        // +1000 (not +1) because other people add data to this shared service while we
        // run
        long pageBeyondLast = totalPages + 1_000;

        // Act: request a page that cannot exist
        Response response = users.list(Map.of("page", pageBeyondLast, "per_page", 100));
        List<User> results = okList(response); // checks 200 + JSON + schema

        // Assert: success with an empty list, not an error
        assertThat(results)
                .as("page %d should be empty", pageBeyondLast)
                .isEmpty();
    }

    private static List<User> okList(Response r) {
        assertStatus(r, 200);
        assertJsonContentType(r);
        assertSchema(r, Schemas.USER_LIST);
        return Arrays.asList(r.as(User[].class));
    }
}
