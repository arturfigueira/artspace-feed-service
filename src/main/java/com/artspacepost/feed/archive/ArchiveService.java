package com.artspacepost.feed.archive;

import com.artspacepost.feed.Post;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import javax.validation.Valid;
import javax.validation.ValidationException;
import lombok.AllArgsConstructor;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

/**
 * Archives posts for future listing at the user feed. This will simplify the pagination of older
 * posts, giving access to the chronological history of posts that were created the application.
 * <p>
 * Archived won't be archiving the entire post, just the necessary data to retrieve it from its
 * original service.
 */
@ApplicationScoped
@AllArgsConstructor
@ReactiveTransactional
public class ArchiveService {

  final ArchiveMapper postMapper;
  final ArchiveRepository archiveRepository;
  final Logger logger;

  /**
   * Persist given {@link Post} into the archive repository.
   *
   * @param post Non-null, valid, Post to be archived.
   * @return a void {@link Uni}
   * @throws javax.validation.ValidationException if given post is invalid
   */
  @Timeout
  @Retry(maxRetries = 5, delay = 200, abortOn = ValidationException.class)
  @FibonacciBackoff
  public Uni<Void> archivePost(@Valid final Post post) {
    final var archive = postMapper.toArchive(post);
    return archiveRepository.persist(archive).replaceWithVoid();
  }
}
