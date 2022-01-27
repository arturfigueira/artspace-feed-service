package com.artspacepost.feed.incoming;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artspacepost.feed.Post;
import com.artspacepost.feed.archive.ArchiveService;
import com.artspacepost.feed.cache.FeedCacheService;
import com.github.javafaker.Faker;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostKafkaConsumerTest {

  static Faker FAKER;
  static Duration ONE_SEC = Duration.ofSeconds(1L);

  @Mock
  ArchiveService archiveService;

  @Mock
  FeedCacheService feedCacheService;

  Logger logger = Logger.getLogger(PostKafkaConsumerTest.class);

  PostKafkaConsumer consumer;

  @Captor
  ArgumentCaptor<Post> argumentCaptor;

  @BeforeAll
  public static void setupBefore() {
    FAKER = new Faker(Locale.ENGLISH);
  }

  @BeforeEach
  void setup() {
    consumer = new PostKafkaConsumer();
    consumer.correlationKey = "correlationId";
    consumer.archiveService = archiveService;
    consumer.logger = logger;
    consumer.feedCacheService = feedCacheService;
  }

  @Test
  void consumeShouldIgnoreMessagesWithoutHeaders() {
    //given
    var message = sampleIncomingMessage(null, null);

    //when
    this.consumer.consume(message).await().atMost(ONE_SEC);

    //then
    verify(archiveService, never()).archivePost(any(Post.class));
    verify(feedCacheService, never()).append(any(Post.class));
  }

  @ParameterizedTest
  @EmptySource
  @ValueSource(strings = "    ")
  void consumeShouldIgnoreMessagesWithoutCorrelationId(String correlationId) {
    //given
    var message = sampleIncomingMessage(correlationId, null);

    //when
    this.consumer.consume(message).await().atMost(ONE_SEC);

    //then
    verify(archiveService, never()).archivePost(any(Post.class));
    verify(feedCacheService, never()).append(any(Post.class));
  }

  @Test
  void consumeShouldIgnoreMessagesWithoutPost() {
    //given
    var message = sampleIncomingMessage(UUID.randomUUID().toString(), null);

    //when
    this.consumer.consume(message).await().atMost(ONE_SEC);

    //then
    verify(archiveService, never()).archivePost(any(Post.class));
    verify(feedCacheService, never()).append(any(Post.class));
  }

  @Test
  void consumeShouldArchiveMessageData() {
    //given
    var post = samplePost();
    var message = sampleIncomingMessage(post);
    when(archiveService.archivePost(any(Post.class))).thenReturn(Uni.createFrom().voidItem());
    when(feedCacheService.append(any(Post.class))).thenReturn(Uni.createFrom().item(true));

    //when
    this.consumer.consume(message).await().atMost(ONE_SEC);

    //then
    verify(archiveService).archivePost(argumentCaptor.capture());
    final var data = argumentCaptor.getValue();
    assertThat(data.getMessage(), is(post.getMessage()));
    assertThat(data.getAuthor(), is(post.getAuthorUsername()));
    assertThat(data.getCreationTime(), notNullValue());
    assertTrue(post.isEnabled());
  }

  @Test
  void consumeShouldCacheMessageData() {
    //given
    var post = samplePost();
    var message = sampleIncomingMessage(post);
    when(archiveService.archivePost(any(Post.class))).thenReturn(Uni.createFrom().voidItem());
    when(feedCacheService.append(any(Post.class))).thenReturn(Uni.createFrom().item(true));

    //when
    this.consumer.consume(message).await().atMost(ONE_SEC);

    //then
    verify(feedCacheService).append(argumentCaptor.capture());
    final var data = argumentCaptor.getValue();
    assertThat(data.getMessage(), is(post.getMessage()));
    assertThat(data.getAuthor(), is(post.getAuthorUsername()));
    assertThat(data.getCreationTime(), notNullValue());
    assertTrue(post.isEnabled());
  }


  private static PostDTO samplePost() {
    final var postDto = new PostDTO();
    postDto.setId(UUID.randomUUID().toString());
    postDto.setAuthorUsername(FAKER.name().username());
    postDto.setMessage(FAKER.lorem().sentence());
    postDto.setEnabled(true);
    postDto.setCreationTime(Instant.now());
    postDto.setAction("CREATED");
    return postDto;
  }

  private static ConsumerRecord<String, PostDTO> sampleIncomingMessage(final PostDTO postDTO){
    return sampleIncomingMessage(UUID.randomUUID().toString(), postDTO);
  }

  private static ConsumerRecord<String, PostDTO> sampleIncomingMessage(
      String invalidCorrelationId,
      final PostDTO post) {
    var record = new ConsumerRecord<String, PostDTO>("mock", 1, 1L, null, post);
    record.headers().add("correlationId", Optional.ofNullable(invalidCorrelationId)
        .map(String::getBytes).orElse(null));

    return record;
  }
}