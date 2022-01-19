package no.nav.k9.sak.web.app;

import java.net.URI;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

@Path("/login")
@RequestScoped
public class FrontendLoginResource {

    @GET
    public Response login(@QueryParam("redirectTo") @DefaultValue("/k9/web/") String redirectTo) {
        var uri = URI.create(redirectTo);
        var relativePath = "";
        if (uri.getPath() != null) {
            relativePath += uri.getPath();
        }
        if (uri.getQuery() != null) {
            relativePath += '?' + uri.getQuery();
        }
        if (uri.getFragment() != null) {
            relativePath += '#' + uri.getFragment();
        }
        if (!relativePath.startsWith("/")) {
            relativePath = "/" + relativePath;
        }
        //  når vi har kommet hit, er brukeren innlogget og har fått ID-token. Kan da gjøre redirect til hovedsiden for VL
        return Response.status(307).header(HttpHeaders.LOCATION, relativePath).build();
    }
}
