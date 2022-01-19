package no.nav.k9.sak.web.server.metrics;


import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import io.prometheus.client.CollectorRegistry;
import io.swagger.v3.oas.annotations.Operation;

@Path("/metrics")
@ApplicationScoped
public class PrometheusRestService {

    @GET
    @Operation(hidden = true)
    @Path("/prometheus")
    public Response prometheus() {

        final Writer writer = new StringWriter();
        try {
            TextFormater.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
            return Response.ok().encoding("UTF-8").entity(writer.toString()).header("content-type", TextFormater.CONTENT_TYPE_004).build();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
