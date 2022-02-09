package com.artspace.feed.lifecycle;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
class ApplicationLifeCycle {

  private static final String APP_NAME = "FEED";

  void onStart(@Observes StartupEvent ev) {
    final StringBuilder appName = new StringBuilder("\n");
    appName
        .append(" _______  _______  _______  _______          ___      .______    __ ").append("\n")
        .append("|   ____||   ____||   ____||       \\        /   \\     |   _  \\  |  |").append("\n")
        .append("|  |__   |  |__   |  |__   |  .--.  |      /  ^  \\    |  |_)  | |  |").append("\n")
        .append("|   __|  |   __|  |   __|  |  |  |  |     /  /_\\  \\   |   ___/  |  |").append("\n")
        .append("|  |     |  |____ |  |____ |  '--'  |    /  _____  \\  |  |      |  |").append("\n")
        .append("|__|     |_______||_______||_______/    /__/     \\__\\ | _|      |__|");

    log.info(appName.toString());
    log.info("The application "+APP_NAME+" is starting with profile " + ProfileManager.getActiveProfile());
  }

  void onStop(@Observes ShutdownEvent ev) {
    log.info("The application "+APP_NAME+" is stopping...");
  }
}
