package com.artspace.feed.archive;

import com.artspace.feed.Post;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.Sort.Direction;
import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.Min;
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
  @Transactional(TxType.REQUIRED)
  public Uni<Void> archivePost(@Valid final Post post) {
    final var archive = postMapper.toArchive(post);
    return archiveRepository.persist(archive).replaceWithVoid();
  }

  @Transactional(TxType.SUPPORTS)
  public Uni<List<String>> getArchivedPostIds(@Min(0) int pageIndex, @Min(1) int pageSize) {
    return this.archiveRepository
        .findAll(Sort.by("creationTime", Direction.Descending))
        .page(pageIndex, pageSize)
        .list()
        .map(this::getPostIds);
  }

  private List<String> getPostIds(final List<Archive> archives) {
    return archives.stream()
        .map(Archive::getPostId)
        .map(Object::toString)
        .collect(Collectors.toList());
  }
}
