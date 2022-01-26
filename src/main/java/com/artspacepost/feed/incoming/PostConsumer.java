package com.artspacepost.feed.incoming;

import io.smallrye.mutiny.Uni;

/**
 * A Post data consumer from external sources/services
 * @param <T> The type of the message that is being emitted by the external data source
 */
public interface PostConsumer<T> {

  /**
   * Consume given message of type <T>
   * @param message T message
   * @return
   */
  Uni<Void> consume(final T message);

}
