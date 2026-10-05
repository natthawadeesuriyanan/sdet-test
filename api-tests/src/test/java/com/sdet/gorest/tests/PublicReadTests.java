package com.sdet.gorest.tests;

import com.sdet.gorest.assertions.Schemas;
import com.sdet.gorest.client.Auth;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.sdet.gorest.assertions.ApiAssertions.assertJsonContentType;
import static com.sdet.gorest.assertions.ApiAssertions.assertResponseTime;
import static com.sdet.gorest.assertions.ApiAssertions.assertSchema;
import static com.sdet.gorest.assertions.ApiAssertions.assertStatus;

@Tag("smoke")
@DisplayName("Users - anonymous read access")
class PublicReadTests extends BaseApiTest {

    @Test
    @DisplayName("GET /users without a token -> 200, JSON array matching the user contract")
    void anonymous_list_is_allowed() {
        Response response = users.list(Map.of(), Auth.NONE);

        assertStatus(response, 200);
        assertJsonContentType(response);
        assertSchema(response, Schemas.USER_LIST);
        assertResponseTime(response);
    }
}
