package com.artspacepost.feed.incoming;

import com.artspacepost.feed.Post;
import com.artspacepost.feed.archive.ArchiveService;
import io.smallrye.mutiny.Uni;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * A concrete implementation of {@link PostConsumer} for Kafka message broker
 */
@ApplicationScoped
public class PostKafkaConsumer implements PostConsumer<ConsumerRecord<String, PostDTO>> {

  @Getter(AccessLevel.PROTECTED)
  @ConfigProperty(name = "incoming.correlation.key", defaultValue = "correlationId")
  String correlationKey;

  @Inject
  Logger logger;

  @Inject
  ArchiveService archiveService;

  /**
   * Consumes {@link PostDTO} records sent from a kafka broker instance.
   * <p>
   * Consumed posts will be archived
   * <p>
   * Messages are required to have a {@link  PostKafkaConsumer#correlationKey} header to identify
   * the transit id that originated this message. Failing to ship a message with this header will
   * force the consumer to ignore the message. No failure will happen, and the record will be
   * acknowledged.
   * <p>
   * Also invalid DTOs will be ignored as well and won't return as a failure, acknowledging the
   * received record.
   * <p>
   * Records won't be acknowledged if a persistence error occurs. This will be considered a failure
   * and will dirty this consumer.
   *
   * @param message A record containing a {@code PostDTO}
   * @return a void {@link Uni}
   */
  @Incoming("post-in")
  @Override
  public Uni<Void> consume(final ConsumerRecord<String, PostDTO> message) {
    final var headers = message.headers();
    final var correlationHeader = headers.headers(correlationKey);

    var result = Uni.createFrom().voidItem();

    if (!correlationHeader.iterator().hasNext()) {
      logger.errorf("Required headers not found. Ignoring Message %s", message);
      return result;
    }

    var correlation = Optional.ofNullable(correlationHeader.iterator().next())
        .map(Header::value)
        .map(String::new)
        .map(String::trim)
        .filter(s -> !s.isBlank());

    if (correlation.isEmpty()) {
      logger.errorf("CorrelationId header not found. Ignoring Message %s", message);
      return result;
    }

    final var correlationId = correlation.get();
    final var value = Optional.ofNullable(message.value());

    if (value.isEmpty()) {
      logger.errorf("[%s] Post not found at received message. Ignoring Message %s", correlationId,
          message);
      return result;
    }

    var dto = value.get();
    logger.infof("[%s] New post message to process. %s", correlationId, dto);

    final var post = Post.builder()
        .author(dto.getAuthorUsername())
        .message(dto.getMessage())
        .creationTime(dto.getCreationTime())
        .isEnabled(dto.isEnabled())
        .id(dto.getId())
        .build();

    result = archiveService.archivePost(post)
        .invoke(a -> logger.infof("[%s] Post %s has been archived", correlationId, post))
        .onFailure()
        .invoke(throwable -> logger.errorf("[%s] Post %s could not be archived. %s", correlationId,
            post, throwable))
        .replaceWithVoid();

    return result;
  }

}
