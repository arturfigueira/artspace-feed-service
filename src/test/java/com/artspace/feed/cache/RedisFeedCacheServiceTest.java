package com.artspace.feed.cache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.wildfly.common.Assert.assertTrue;

import com.artspace.feed.Post;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.hamcrest.core.Is;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RedisFeedCacheServiceTest {

  static Faker FAKER;
  static Duration ONE_SEC = Duration.ofSeconds(1L);

  @Mock
  ReactiveRedisClient redisClient;

  @Mock
  ObjectMapper objectMapper;

  Logger logger = Logger.getLogger(RedisFeedCacheServiceTest.class);

  RedisFeedCacheService redisFeedCacheService;

  @Captor
  ArgumentCaptor<List<String>> argumentCaptor;

  @BeforeAll
  public static void setupBefore() {
    FAKER = new Faker(Locale.ENGLISH);
  }

  @BeforeEach
  public void setup() {
    redisFeedCacheService = new RedisFeedCacheService();
    redisFeedCacheService.redisClient = redisClient;
    redisFeedCacheService.cacheEnabled = true;
    redisFeedCacheService.maxCachedEntries = 5;
    redisFeedCacheService.logger = logger;
    redisFeedCacheService.objectMapper = objectMapper;
  }

  @Test
  @DisplayName("Append should do nothing if cache is disabled")
  void appendWontCacheIfCacheIsDisabled() {
    //given
    redisFeedCacheService.cacheEnabled = false;

    //when
    var result = redisFeedCacheService.append(samplePost()).await().atMost(ONE_SEC);

    //then
    assertTrue(result);
    verify(redisClient, never()).lpush(ArgumentMatchers.any());
  }

  @Test
  @DisplayName("Append should append to correct index")
  void appendWillAppendToCorrectIndex() throws JsonProcessingException {
    //given
    final var response = mock(io.vertx.redis.client.Response.class);
    when(response.toString()).thenReturn("1");
    when(objectMapper.writeValueAsString(any(Post.class))).thenReturn("");

    final var item = new Response(response);
    when(redisClient.lpush(any())).thenReturn(Uni.createFrom().item(item));

    //when
    var result = redisFeedCacheService.append(samplePost()).await().atMost(ONE_SEC);

    //then
    verify(redisClient, times(1)).lpush(ArgumentMatchers.any());
    verify(redisClient).lpush(argumentCaptor.capture());

    final var value = argumentCaptor.getValue();
    assertThat(value.get(0), Is.is("fdd-all"));
  }

  @Test
  @DisplayName("Append should append to the correct data")
  void appendWillAppendToDataCorrectly() throws JsonProcessingException {
    //given
    final var response = mock(io.vertx.redis.client.Response.class);
    when(response.toString()).thenReturn("1");
    when(objectMapper.writeValueAsString(any(Post.class))).thenReturn("{post-data}");

    final var item = new Response(response);
    when(redisClient.lpush(any())).thenReturn(Uni.createFrom().item(item));

    //when
    var result = redisFeedCacheService.append(samplePost()).await().atMost(ONE_SEC);

    //then
    assertTrue(result);
    verify(redisClient).lpush(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().get(1), Is.is("{post-data}"));
  }

  @Test
  @DisplayName("Append should not let index list grow indefinitely")
  void appendWillKeepListSizeControlled() throws JsonProcessingException {
    //given
    final var response = mock(io.vertx.redis.client.Response.class);
    when(response.toString()).thenReturn("11");
    when(objectMapper.writeValueAsString(any(Post.class))).thenReturn("");

    final var item = new Response(response);
    when(redisClient.lpush(any())).thenReturn(Uni.createFrom().item(item));
    when(redisClient.ltrim(anyString(), eq("0"), anyString())).thenReturn(
        Uni.createFrom().item(item));

    //when
    redisFeedCacheService.append(samplePost()).await().atMost(ONE_SEC);

    //then
    verify(redisClient, times(1))
        .ltrim(eq("fdd-all"), eq("0"), eq(Long.toString(redisFeedCacheService.maxCachedEntries-1)));
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