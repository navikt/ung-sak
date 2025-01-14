package no.nav.ung.sak.web.app.selftest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;
import no.nav.ung.sak.web.app.tjenester.ApplicationServiceStarter;
import no.nav.ung.sak.web.server.jetty.JettyServer;

@Path("/health")
@ApplicationScoped
public class HealthCheckRestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckRestService.class);

    private static final String RESPONSE_CACHE_KEY = "Cache-Control";
    private static final String RESPONSE_CACHE_VAL = "must-revalidate,no-cache,no-store";
    private static final String RESPONSE_OK = "OK";

    private static final Duration MELD_UT_ETTER_KRITISK_TJENESTE_FEILET_I = Duration.ofMinutes(15);

    private ApplicationServiceStarter starterService;
    private SelftestService selftestService;
    private LocalDateTime kritiskTjenesteFeiletVed;

    private static CacheControl cacheControl = noCache();


    public HealthCheckRestService() {
        // CDI
    }

    @Inject
    public HealthCheckRestService(ApplicationServiceStarter starterService, SelftestService selftestService) {
        this.starterService = starterService;
        this.selftestService = selftestService;
        this.kritiskTjenesteOk();
    }

    @GET
    @Path("isAlive")
    public Response isAlive() {
        Response.ResponseBuilder builder;
        if (JettyServer.KILL_APPLICATION.get()) {
            builder = Response.serverError();
        } else if (harKritiskTjenesteFeiletLengreEnn(MELD_UT_ETTER_KRITISK_TJENESTE_FEILET_I)) {
            LOGGER.warn("Melder ut node da kritiske tjenester har feilet lengre enn {} minutter", MELD_UT_ETTER_KRITISK_TJENESTE_FEILET_I.toMinutes());
            builder = Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .header(RESPONSE_CACHE_KEY, RESPONSE_CACHE_VAL);
        } else {
            builder = Response
                .ok(RESPONSE_OK)
                .header(RESPONSE_CACHE_KEY, RESPONSE_CACHE_VAL);
        }
        return builder.cacheControl(cacheControl).build();
    }

    @GET
    @Path("isReady")
    public Response isReady() {
        Response.ResponseBuilder builder;
        if (selftestService.kritiskTjenesteFeilet()) {
            kritiskTjenesteFeilet();
            builder = Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .header(RESPONSE_CACHE_KEY, RESPONSE_CACHE_VAL);
        } else {
            kritiskTjenesteOk();
            builder = Response.ok(RESPONSE_OK)
                .header(RESPONSE_CACHE_KEY, RESPONSE_CACHE_VAL);
        }
        builder.cacheControl(cacheControl);

        return builder.build();
    }

    @GET
    @Path("preStop")
    public Response preStop() {
        starterService.stopServices();
        return Response.ok(RESPONSE_OK).build();
    }

    private void kritiskTjenesteFeilet() {
        if (kritiskTjenesteFeiletVed == null) {
            LocalDateTime now = LocalDateTime.now();
            LOGGER.warn("Setter kritiskTjenesteFeiletVed={}", now);
            kritiskTjenesteFeiletVed = now;
        }
    }

    private void kritiskTjenesteOk() {
        kritiskTjenesteFeiletVed = null;
    }

    private Optional<Duration> kritiskTjenesteFeiletI() {
        LocalDateTime kritiskTjenesteFeiletVed = this.kritiskTjenesteFeiletVed;
        if (kritiskTjenesteFeiletVed == null) {
            return Optional.empty();
        } else {
            return Optional.of(
                Duration.between(kritiskTjenesteFeiletVed, LocalDateTime.now()).abs()
            );
        }
    }

    private boolean harKritiskTjenesteFeiletLengreEnn(Duration duration) {
        Optional<Duration> kritiskTjenerteFeiletI = kritiskTjenesteFeiletI();
        if (kritiskTjenerteFeiletI.isEmpty()) {
            return false;
        } else {
            return kritiskTjenerteFeiletI.get().toMillis() > duration.toMillis();
        }
    }

    private static CacheControl noCache() {
        CacheControl cc = new CacheControl();
        cc.setMustRevalidate(true);
        cc.setPrivate(true);
        cc.setNoCache(true);
        cc.setNoStore(true);
        return cc;
    }

}


