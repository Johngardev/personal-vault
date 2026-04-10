package io.sovereign;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.awt.*;
import java.util.Map;

@RegisterRestClient(baseUri = "https://api.github.com")
public interface GitHubClient {

  @GET
  @Path("/users/{username}")
  @Produces(MediaType.APPLICATION_JSON)
  Map<String, Object> getUserData(@PathParam("username") String username);
}
