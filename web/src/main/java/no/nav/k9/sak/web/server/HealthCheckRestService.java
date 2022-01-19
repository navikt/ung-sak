package no.nav.k9.sak.web.server;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;

@Path("/health")
@ApplicationScoped
public class HealthCheckRestService {

    private static CacheControl cacheControl = noCache();

    private static CacheControl noCache() {
        CacheControl cc = new CacheControl();
        cc.setMustRevalidate(true);
        cc.setPrivate(true);
        cc.setNoCache(true);
        cc.setNoStore(true);
        return cc;
    }

    @GET
    @Operation(hidden = true)
    @Path("/isReady")
    public Response isReady() {
        Response.ResponseBuilder builder;
        builder = Response.ok("OK", MediaType.TEXT_PLAIN_TYPE);
        builder.cacheControl(cacheControl);
        return builder.build();
    }

    /**
     * Så lenge jetty klarer å svare 200 OK, og at applikasjonen er startet opp regner vi med att allt er i orden
     *
     * @return 200 OK if app is started
     */
    @GET
    @Operation(hidden = true)
    @Path("/isAlive")
    public Response isAlive() {
        return isReady();
    }
}
