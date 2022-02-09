package com.artspace.feed;

import com.artspace.feed.archive.ArchiveService;
import com.artspace.feed.cache.FeedCacheService;
import com.artspace.feed.client.PostResourceProxy;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.providers.i18n.ProviderLogging;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.With;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@ApplicationScoped
class FeedService {

  final FeedCacheService cacheService;
  final ArchiveService archiveService;
  final Logger logger;

  @RestClient
  PostResourceProxy postResourceProxy;


  @RequiredArgsConstructor
  @Getter(AccessLevel.PROTECTED)
  @ToString
  static class PageRequest {

    static final int FIRST_PAGE = 0;

    @With
    private final int index;
    private final int size;
    private final String correlationId;

    boolean isForFirstPage() {
      return FIRST_PAGE == index;
    }
  }

  public Uni<List<Post>> getFeed(final PageRequest request) {
    final var pageRequest = Optional.ofNullable(request)
        .filter(pgReq -> pgReq.index >= 0 && pgReq.size > 0)
        .orElseThrow(() -> new IllegalArgumentException(
            "Page should be positive and size greater than zero"));

    return pageRequest.isForFirstPage() ? fromCache(pageRequest) : fromArchive(pageRequest);
  }

  private Uni<List<Post>> fromCache(final PageRequest request) {
    return cacheService.list(request.size)
        .chain(posts -> validateCacheOrGetFromArchive(posts, request));
  }

  private Uni<List<Post>> validateCacheOrGetFromArchive(final List<Post> posts,
      final PageRequest request) {
    return !posts.isEmpty() ? Uni.createFrom().item(posts) : appealToArchive(request);
  }

  private Uni<List<Post>> appealToArchive(final PageRequest request) {
    return this.fromArchive(request.withIndex(PageRequest.FIRST_PAGE))
        .invoke(this::cacheArchivedPosts);
  }

  private void cacheArchivedPosts(List<Post> archives) {
    List<Uni<Boolean>> results = new ArrayList<>();
    for (var archivedPost : archives) {
      results.add(this.cacheService.append(archivedPost));
    }
    Uni.join().all(results).andCollectFailures().subscribe().with(v -> {
    }, ProviderLogging.log::failureEmittingMessage);
  }

  @Timeout()
  @Retry(abortOn = {IllegalArgumentException.class})
  @CircuitBreakerName("feed-from-post-service")
  @CircuitBreaker(
      requestVolumeThreshold = 10,
      delay = 100L,
      skipOn = {IllegalArgumentException.class})
  @Fallback(fallbackMethod = "fromArchiveFallback")
  protected Uni<List<Post>> fromArchive(final PageRequest request) {
    return archiveService.getArchivedPostIds(request.index, request.size)
        .map(ids -> String.join(",", ids))
        .chain(ids -> this.getPostsFromService(ids, request));
  }

  protected Uni<List<Post>> fromArchiveFallback(final PageRequest request) {
    return Uni.createFrom().item(Collections.emptyList());
  }

  private Uni<List<Post>> getPostsFromService(String ids, final PageRequest request) {
    return this.postResourceProxy
        .queryPosts(ids, null, null, request.index, request.size, request.correlationId)
        .map(posts -> posts.stream()
            .sorted(byDateDescending()).collect(
                Collectors.toList()))
        .onFailure()
        .invoke(throwable -> logger.errorf("[%s] Unable to request %s from post service. %s",
            request.correlationId, ids, throwable.getMessage()));
  }

  private static Comparator<Post> byDateDescending() {
    return (o1, o2) -> o2.getCreationTime().compareTo(o1.getCreationTime());
  }
}
