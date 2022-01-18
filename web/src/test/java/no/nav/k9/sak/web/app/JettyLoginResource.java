package no.nav.k9.sak.web.app;

import java.net.URI;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;


/**
 * Innlogging ved kjøring lokalt.
 * <p>
 * Se utviklerhåndbok for hvordan dette fungerer.
 */
@Path("/login")
@RequestScoped
public class JettyLoginResource {

    @GET
    @Path("")
    public Response login() {
        //  når vi har kommet hit, er brukeren innlogget og har fått ID-token. Kan da gjøre redirect til hovedsiden for VL
        return Response.temporaryRedirect(URI.create("http://localhost:9000/")).build();
    }
}
