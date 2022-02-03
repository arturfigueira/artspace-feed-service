package com.artspace.feed.client;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

@Provider
public class GenericResponseExceptionMapper implements ResponseExceptionMapper<RuntimeException> {

  @Override
  public RuntimeException toThrowable(Response response) {
    if(response == null){
      return null;
    }

    if (response.getStatus() == 500) {
      throw new PostServiceRuntimeException("Post service responded with HTTP 500");
    }
    if (response.getStatus() == 400) {
      throw new IllegalArgumentException("Post service responded with HTTP 400");
    }

    return null;
  }
}
