package no.nav.k9.sak.web.app;

import java.net.URI;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@Path("/login")
@RequestScoped
public class FrontendLoginResource {

    @GET
    @Path("")
    public Response login(@QueryParam("redirectTo") @DefaultValue("/k9/web/") String redirectTo) {
        var relativePath = URI.create(redirectTo).getPath();
        if (!relativePath.startsWith("/")) {
            relativePath = "/" + relativePath;
        }
        //  når vi har kommet hit, er brukeren innlogget og har fått ID-token. Kan da gjøre redirect til hovedsiden for VL
        return Response.status(307).header(HttpHeaders.LOCATION, relativePath).build();
    }
}
