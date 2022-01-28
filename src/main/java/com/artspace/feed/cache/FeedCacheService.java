package com.artspace.feed.cache;

import com.artspace.feed.Post;
import io.smallrye.mutiny.Uni;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * {@link FeedCacheService} offers meanings to cache post data for quick and easy access, instead of a
 * gathering this information from a regular database or external service.
 */
public interface FeedCacheService {

  /**
   * Append given {@link Post} into a cached list of posts
   *
   * @param post non-null, valid post data to be cached
   */
  Uni<Boolean> append(@NotNull @Valid final Post post);
}
