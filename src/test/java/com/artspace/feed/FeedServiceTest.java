package com.artspace.feed;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artspace.feed.FeedService.PageRequest;
import com.artspace.feed.archive.ArchiveService;
import com.artspace.feed.cache.FeedCacheService;
import com.artspace.feed.client.PostResourceProxy;
import com.github.javafaker.Faker;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hamcrest.core.Is;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

  static Faker FAKER;

  static Duration ONE_SEC = Duration.ofSeconds(1L);

  FeedService feedService;

  @Mock
  FeedCacheService cacheService;

  @Mock
  ArchiveService archiveService;

  @Mock
  PostResourceProxy postResourceProxy;

  @BeforeAll
  public static void setupBefore() {
    FAKER = new Faker(Locale.ENGLISH);
  }

  @BeforeEach
  void setup() {
    this.feedService = new FeedService(cacheService, archiveService,
        Logger.getLogger(FeedService.class));
    this.feedService.postResourceProxy = postResourceProxy;
  }

  private static Stream<Arguments> pageInvalidArguments() {
    return Stream.of(
        Arguments.of(-1, 10),
        Arguments.of(1, 0),
        Arguments.of(1, -5),
        Arguments.of(-1, 0)
    );
  }

  @ParameterizedTest
  @MethodSource("pageInvalidArguments")
  void getFeedShouldThrowWithInvalidArgs(int page, int size) {
    var pageRequest = new PageRequest(page, size, "");
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> this.feedService.getFeed(pageRequest));
  }

  @Test
  void getFeedShouldSearchFirstPageFromCacheFirst() {
    //given
    var posts = new ArrayList<Post>();
    for (var index = 0; index < 5; index++) {
      posts.add(samplePost());
    }

    when(this.cacheService.list(5L)).thenReturn(Uni.createFrom().item(posts));

    var pageRequest = new PageRequest(0, 5, "");

    //when
    final var result = this.feedService.getFeed(pageRequest).await().atMost(ONE_SEC);

    //then
    assertThat(result, Is.is(posts));
  }


  @Test
  void getFeedShouldAppealToArchiveIfNoCacheFound() {
    //given
    var posts = new ArrayList<Post>();
    final var pageSize = 5;
    for (var index = 0; index < pageSize; index++) {
      posts.add(samplePost());
    }

    final var postIds = posts.stream().map(Post::getId).collect(Collectors.toList());

    when(this.cacheService.list(5L)).thenReturn(Uni.createFrom().item(Collections.emptyList()));
    when(this.cacheService.append(any(Post.class))).thenReturn(Uni.createFrom().item(true));
    when(this.archiveService.getArchivedPostIds(0, pageSize))
        .thenReturn(
            Uni.createFrom().item(postIds));
    when(this.postResourceProxy.queryPosts(eq(String.join(",", postIds)), isNull(), isNull(), anyInt(), anyInt(),
        anyString())).thenReturn(Uni.createFrom().item(posts));

    var pageRequest = new PageRequest(0, pageSize, "");

    final var sortedPosts = posts.stream().sorted((o1, o2) -> o2.getCreationTime().compareTo(o1.getCreationTime())).collect(
        Collectors.toList());

    //when
    final var result = this.feedService.getFeed(pageRequest).await().atMost(ONE_SEC);

    //then
    assertThat(result, Is.is(sortedPosts));
  }

  @Test
  void getFeedShouldShouldCacheAppealedPosts() {
    //given
    var posts = new ArrayList<Post>();
    final var pageSize = 5;
    for (var index = 0; index < pageSize; index++) {
      posts.add(samplePost());
    }

    final var postIds = posts.stream().map(Post::getId).collect(Collectors.toList());

    when(this.cacheService.list(5L)).thenReturn(Uni.createFrom().item(Collections.emptyList()));
    when(this.cacheService.append(any(Post.class))).thenReturn(Uni.createFrom().item(true));
    when(this.archiveService.getArchivedPostIds(0, pageSize))
        .thenReturn(
            Uni.createFrom().item(postIds));
    when(this.postResourceProxy.queryPosts(eq(String.join(",", postIds)), isNull(), isNull(), anyInt(), anyInt(),
        anyString())).thenReturn(Uni.createFrom().item(posts));

    var pageRequest = new PageRequest(0, pageSize, "");

    //when
    this.feedService.getFeed(pageRequest).await().atMost(ONE_SEC);

    //then
    verify(this.cacheService, times(pageSize)).append(any(Post.class));
  }

  @Test
  void getFeedShouldSearchTheArchiveForOlderPosts() {
    //given
    final var pageSize = 5;
    var posts = new ArrayList<Post>();
    for (var index = 0; index < pageSize; index++) {
      posts.add(samplePost());
    }

    final var orderedPosts = posts.stream().sorted((o1, o2) -> o2.getCreationTime().compareTo(o1.getCreationTime())).collect(
        Collectors.toList());

    final var postIds = orderedPosts.stream().map(Post::getId).collect(Collectors.toList());
    when(this.archiveService.getArchivedPostIds(1, pageSize))
        .thenReturn(
            Uni.createFrom().item(postIds));

    when(this.postResourceProxy.queryPosts(eq(String.join(",", postIds)), isNull(), isNull(), anyInt(), anyInt(),
        anyString())).thenReturn(Uni.createFrom().item(posts));

    var pageRequest = new PageRequest(1, pageSize, "");

    //when
    final var result = this.feedService.getFeed(pageRequest).await().atMost(ONE_SEC);

    //then
    assertThat(result, Is.is(orderedPosts));
  }

  private static Post samplePost() {
    return Post.builder()
        .id(UUID.randomUUID().toString())
        .author(FAKER.name().username())
        .message(FAKER.lorem().sentence())
        .isEnabled(true)
        .creationTime(Instant.now()).build();
  }

}