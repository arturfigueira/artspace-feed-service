package com.artspacepost.feed.incoming;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

/**
 * Default post data deserializer
 */
class PostDeserializer extends ObjectMapperDeserializer<PostDTO> {

  public PostDeserializer() {
    super(PostDTO.class);
  }
}
