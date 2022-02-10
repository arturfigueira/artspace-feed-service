package com.artspace.feed.client;

import io.quarkus.oidc.SecurityEvent;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.Logger;

@ApplicationScoped
@RequiredArgsConstructor
public class ClientSecurityEventListener {

  final Logger logger;

  public void event(@Observes SecurityEvent event) {
    final var tenantId = event.getSecurityIdentity().getAttribute("tenant-id");
    logger.debugf("event:%s,tenantId:%s", event.getEventType().name(), tenantId);
  }
}
