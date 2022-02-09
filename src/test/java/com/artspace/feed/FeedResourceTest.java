package com.artspace.feed;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;

import com.artspace.feed.archive.ArchiveService;
import com.artspace.feed.cache.FeedCacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(WireMockPostExtensions.class)
class FeedResourceTest {

  private static final Duration FIVE_SECONDS = Duration.ofSeconds(5);

  @Inject
  ArchiveService archiveService;

  @Inject
  FeedCacheService feedCacheService;

  @InjectWiredPost
  WiredPostService wiredPostService;

  @Inject
  ObjectMapper objectMapper;

  @BeforeEach
  void before() {

  }

  @Test
  @DisplayName("An OPEN Api resource should be available")
  void shouldPingOpenAPI() {
    given()
        .header(ACCEPT, APPLICATION_JSON)
        .when()
        .get("/q/openapi")
        .then()
        .statusCode(OK.getStatusCode());
  }

  @Test
  @DisplayName("Should search Cache for newer contents")
  void shouldSearchNewerContentsAtCache() throws JsonProcessingException {

    final var cachePosts = new ArrayList<>(wiredPostService.getPage(0));
    cachePosts.forEach(post -> this.feedCacheService.append(post).await().atMost(FIVE_SECONDS));

    final var expectedPosts = new ArrayList<>(cachePosts);
    Collections.reverse(expectedPosts);

    var response = given()
        .header(ACCEPT, APPLICATION_JSON)
        .pathParam("index", 0)
        .header(FeedResource.CORRELATION_HEADER, UUID.randomUUID().toString())
        .when()
        .get("/api/feed?index={index}")
        .then()
        .statusCode(OK.getStatusCode())
        .extract();

    final var responseData = response.body().asString();
    final var responsePosts = objectMapper.readerForListOf(Post.class).readValue(responseData);

    assertThat(responsePosts, Is.is(expectedPosts));
  }

  @Test
  @DisplayName("Should search Post Service for old contents")
  void shouldSearchOlderContentsFromPostService() throws JsonProcessingException {

    final var posts = new ArrayList<>(wiredPostService.getPage(0));
    posts.addAll(wiredPostService.getPage(1));
    posts.forEach(post -> this.archiveService.archivePost(post).await().atMost(FIVE_SECONDS));

    final var expectedPosts = new ArrayList<>(wiredPostService.getPage(1)).stream()
        .sorted((o1, o2) -> o2.getCreationTime().compareTo(o1.getCreationTime()))
        .collect(Collectors.toList());

    var response = given()
        .header(ACCEPT, APPLICATION_JSON)
        .pathParam("index", 1)
        .header(FeedResource.CORRELATION_HEADER, UUID.randomUUID().toString())
        .when()
        .get("/api/feed?index={index}")
        .then()
        .statusCode(OK.getStatusCode())
        .extract();

    final var responseData = response.body().asString();
    final var responsePosts = objectMapper.readerForListOf(Post.class).readValue(responseData);

    assertThat(responsePosts, Is.is(expectedPosts));
  }
}