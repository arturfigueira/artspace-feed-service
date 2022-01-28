package com.artspace.feed.cache;

import java.util.Optional;
import lombok.Getter;
import lombok.ToString;

/**
 * Identify cached items that belong to feed
 */
@Getter
@ToString
class FeedCacheId implements CacheId {

  private static final String PREFIX = "fdd-";

  private final String value;

  public FeedCacheId(String id) {
    this.value =Optional.ofNullable(id).filter(s -> !s.isBlank()).map(s -> PREFIX + s)
        .orElseThrow(() -> new IllegalArgumentException("Id must not be blank"));
  }
}
