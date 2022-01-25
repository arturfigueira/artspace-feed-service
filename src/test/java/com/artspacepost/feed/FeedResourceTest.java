package com.artspacepost.feed;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class FeedResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/api/feed")
          .then()
             .statusCode(200)
             .body(is("Hello RESTEasy Reactive"));
    }

}