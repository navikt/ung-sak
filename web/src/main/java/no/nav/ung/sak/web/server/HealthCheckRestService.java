package no.nav.ung.sak.web.server;

import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.ung.sak.web.server.jetty.JettyServer;
import no.nav.k9.sikkerhet.oidc.config.OpenIDConfig;
import no.nav.k9.sikkerhet.oidc.config.OpenIDConfigProvider;

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
        var configs = OpenIDConfigProvider.instance().getConfigs().stream().map(OpenIDConfig::getProvider).collect(Collectors.toSet());
        Response.ResponseBuilder builder;

        //STS er fjernet for VTP, men fortsatt aktiv ellers.
        if (Environment.current().isLocal() && !configs.isEmpty() || configs.size() > 1) {
            builder = Response.ok("OK", MediaType.TEXT_PLAIN_TYPE);
        } else {
            builder = Response.serverError();
        }
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
        Response.ResponseBuilder builder;
        if (JettyServer.KILL_APPLICATION.get()) {
            builder = Response.serverError();
        } else {
            builder = Response.ok("OK", MediaType.TEXT_PLAIN_TYPE);
        }
        builder.cacheControl(cacheControl);
        return builder.build();
    }
}
