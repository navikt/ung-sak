package no.nav.k9.sak.web.app;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;

@Path("/dummy/{var:.+}")
public class DummyRestTjenesteForTest {

    public DummyRestTjenesteForTest() {

    }

    @GET
    @Operation(hidden = true)
    public Response get() {
        return Response.ok().build();
    }

    @POST
    @Operation(hidden = true)
    public Response post() {
        return Response.ok().build();
    }

    @OPTIONS
    @Operation(hidden = true)
    public Response options() {
        return Response.ok().build();
    }
}
