package com.artspacepost.feed.incoming;

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

  /**
   * Consumes records from Kafka with {@link PostDTO}.
   * <p>
   * Messages are required to have a {@link  PostKafkaConsumer#correlationKey} header to identify
   * the transit id that originated this message. Failing to ship a message with this header will
   * force the consumer to ignore the message. No failure will happen, and the record will be
   * acknowledged.
   * <p>
   * Also invalid DTOs will be ignored as well and won't return as a failure, acknowledging the
   * received record.
   * <p>
   * Records won't be acknowledged only if a persistence error occurs. This will be considered a
   * failure and will dirty this consumer.
   *
   * @param message A record containing a {@code PostDTO}
   */
  @Incoming("post-in")
  @Override
  public void consume(final ConsumerRecord<String, PostDTO> message) {
    final var headers = message.headers();
    final var correlationHeader = headers.headers(correlationKey);
    if (!correlationHeader.iterator().hasNext()) {
      logger.errorf("Required headers not found. Ignoring Message %s", message);
      return;
    }

    var correlation = Optional.ofNullable(correlationHeader.iterator().next())
        .map(Header::value)
        .map(String::new)
        .map(String::trim)
        .filter(s -> !s.isBlank());

    if (correlation.isEmpty()) {
      logger.errorf("CorrelationId header not found. Ignoring Message %s", message);
      return;
    }

    final var correlationId = correlation.get();
    final var post = message.value();
    logger.infof("[%s] New incoming Post message to process. %s", correlationId, post);

    //TODO Process given user
  }

}
