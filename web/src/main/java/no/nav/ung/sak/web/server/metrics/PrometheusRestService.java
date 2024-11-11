package no.nav.ung.sak.web.server.metrics;


import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import no.nav.k9.felles.log.metrics.MetricsUtil;

@Path("/metrics")
@Produces(TEXT_PLAIN)
@ApplicationScoped
public class PrometheusRestService {

    @GET
    @Operation(hidden = true)
    @Path("/prometheus")
    public String prometheus()  {
        return MetricsUtil.REGISTRY.scrape();
    }
}
