package com.artspace.feed;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.artspace.feed.FeedService.PageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import java.util.List;
import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

@Path("/api/feed")
@Tag(name = "feed")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FeedResource {

  protected static final String CORRELATION_HEADER = "X-Request-ID";

  @Inject
  FeedService feedService;

  @Inject
  Logger logger;

  @ConfigProperty(name = "feed.items.per.page", defaultValue = "10")
  int itemsPerPage;

  @Operation(summary = "Get Feed pages")
  @GET
  @APIResponse(
      responseCode = "200",
      content =
      @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Post.class)))
  @APIResponse(responseCode = "400", description = "Query params contains invalid data")
  public Uni<Response> getFeedPage(
      @DefaultValue("0") @PositiveOrZero @QueryParam("index") int index,
      @NotBlank @HeaderParam(CORRELATION_HEADER) String correlationId
  ) {
    logger.infof("[%s] Requested feed page %s", correlationId, index);

    final var pageRequest = new PageRequest(index, itemsPerPage, correlationId);
    var feedPage = feedService.getFeed(pageRequest)
        .invoke(posts -> handleSuccess(index, correlationId, posts))
        .onFailure()
        .invoke(throwable -> handleFailure(index, correlationId, throwable));

    return feedPage.map(entities -> Response.ok(entities).build());
  }

  private void handleFailure(int index, String correlationId, Throwable throwable) {
    logger.errorf("[%s] An error occurred while retrieving feed page %s. %s",
        correlationId, index, throwable);
  }

  private void handleSuccess(int index, String correlationId, List<Post> posts) {
    logger.infof("[%s] Feed page %s returned %s posts", correlationId, index,
        posts.size());
  }
}