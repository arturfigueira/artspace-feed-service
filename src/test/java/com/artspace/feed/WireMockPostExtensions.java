package com.artspace.feed;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import java.util.Optional;

public class WireMockPostExtensions implements QuarkusTestResourceLifecycleManager {

  private WireMockServer wireMockServer;

  private final WiredPostService wiredPostService = new WiredPostService();

  private static final ObjectMapper MAPPER;

  static {
    MAPPER = new ObjectMapper();
    MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    MAPPER.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
    MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    MAPPER.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES);
    MAPPER.findAndRegisterModules();
  }

  @Override
  public Map<String, String> start() {
    wireMockServer = new WireMockServer();
    wireMockServer.start();

    stubFor(get(urlMatching(
        "\\/api\\/posts\\?ids=([a-zA-Z0-9\\-]+(\\%2C)?)+\\&index=1\\&size=" + WiredPostService.PAGE_SIZE))
        .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
                getBodyForPage(1)
            )));

    return Map.of(
        "quarkus.rest-client.post-api.url", wireMockServer.baseUrl(),
        "feed.items.per.page", Long.toString(WiredPostService.PAGE_SIZE)
    );
  }

  private String getBodyForPage(final int page) {
    try {
      return MAPPER.writeValueAsString(this.wiredPostService.getPage(page));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public void inject(final TestInjector testInjector) {
    testInjector.injectIntoFields(wiredPostService,
        new TestInjector.AnnotatedAndMatchesType(InjectWiredPost.class, WiredPostService.class));
  }

  @Override
  public void stop() {
    Optional.ofNullable(wireMockServer).ifPresent(WireMockServer::stop);
  }
}
