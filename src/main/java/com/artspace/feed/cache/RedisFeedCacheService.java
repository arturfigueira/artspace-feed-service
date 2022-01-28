package com.artspace.feed.cache;

import com.artspace.feed.Post;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.providers.i18n.ProviderLogging;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

/**
 * Singleton implementation of a {@link FeedCacheService} using redis.
 *
 * <p>Fault Tolerance, such as timeout and retry are applied over all available methods, to
 * guarantee that the service continue to operate without breaking the core features of the service
 *
 * <p>Cache can be disabled via configuration property {@code cache.service.enabled}, set through
 * application.properties
 *
 * <p>How many posts can be cached per cache key can be configured via configuration property
 * {@code cache.service.max.entries.per.key}, set through application.properties
 */
@Singleton
public class RedisFeedCacheService implements FeedCacheService {

  private static final String PUSH_SUCCESS = "OK";

  @Inject
  ReactiveRedisClient redisClient;

  @Inject
  ObjectMapper objectMapper;

  @Inject
  Logger logger;

  @ConfigProperty(name = "cache.service.enabled", defaultValue = "true")
  boolean cacheEnabled;

  @ConfigProperty(name = "cache.service.max.entries.per.key", defaultValue = "10")
  long maxCachedEntries;


  /**
   * {@inheritDoc}
   * <p>
   * If cache is disabled it will return a {@link Uni} with {@code true} After the append occurs the
   * method will perform a trim operation to guarantee that the list size doesn't grow indefinitely
   * <p>
   * In case of fault tolerance errors a a {@link Uni} with {@code false} will be returned as fall
   * back
   *
   * @param post non-null, valid post data to be cached
   * @return a {@link Uni} that will resolve into the current size of the list where the post was
   * added, if post was appended, {@code 0} otherwise
   */
  @Override
  public Uni<Boolean> append(final @NotNull @Valid Post post) {
    var result = Uni.createFrom().item(true);
    if (cacheEnabled) {
      final var cacheId = new FeedCacheId("all");
      result = doAppend(cacheId, post)
          .invoke(length -> trim(cacheId, length))
          .map(listLength -> listLength > 0);
    } else {
      logger.warn("Caching is disabled. Ignoring cache request and returning default value true");
    }
    return result;
  }

  @Timeout()
  @CircuitBreakerName("feed-cache-append")
  @CircuitBreaker(
      requestVolumeThreshold = 10,
      delay = 500L)
  @Bulkhead()
  @Fallback(fallbackMethod = "doAppendFallback")
  /**
   * Protected method to allow fault tolerance to work upon it
   */
  protected Uni<Long> doAppend(final CacheId id, final Post post) {
    try {
      final var valueAsString = objectMapper.writeValueAsString(post);

      return this.redisClient
          .lpush(List.of(id.getValue(), valueAsString))
          .map(Optional::ofNullable)
          .map(response -> response.map(res -> Long.parseLong(res.toString())).orElse(0L));

    } catch (JsonProcessingException e) {
      logger.errorf("Unable to convert %s to string and cache it", post, e);
      return emptyUni();
    }
  }

  protected Uni<Long> doAppendFallback(final CacheId id, final Post post) {
    logger.warnf("Fallback active while appending data %s with cacheId %s", post, id.getValue());
    return emptyUni();
  }

  private static Uni<Long> emptyUni() {
    return Uni.createFrom().item(0L);
  }

  private void trim(final CacheId id, final Long listSize) {
    if (listSize > this.maxCachedEntries) {
      this.redisClient
          .ltrim(id.getValue(), "0", Long.toString(this.maxCachedEntries-1))
          .map(Optional::ofNullable)
          .map(result -> result.map(response -> response.toString().contains(PUSH_SUCCESS))
              .orElse(false))
          .invoke(
              result -> logger.debugf("Trim of cache %s finished with response %s", id.getValue(),
                  result))
          .onFailure()
          .invoke(
              throwable -> logger.errorf("Trim of cache %s failed %s", id.getValue(), throwable))
          .subscribe()
          .with(x -> {
          }, ProviderLogging.log::failureEmittingMessage);
    }
  }

}
