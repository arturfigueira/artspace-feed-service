package com.artspace.feed.client;

import com.artspace.feed.Post;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.smallrye.mutiny.Uni;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/posts")
@Produces(MediaType.APPLICATION_JSON)
@RegisterProvider(OidcClientRequestReactiveFilter.class)
@RegisterRestClient(configKey = "post-api")
public interface PostResourceProxy {

  @GET
  Uni<List<Post>> queryPosts(
      @QueryParam("ids") String ids,
      @QueryParam("author") String username,
      @QueryParam("status") String postStatus,
      @QueryParam("index") int pageIndex,
      @QueryParam("size") int pageSize,
      @HeaderParam("X-Request-ID") String correlationId);
}
