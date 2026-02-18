package no.nav.ung.sak.web.app.selftest;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_HTML;

@Path("/selftest")
@RequestScoped
public class SelftestRestTjeneste {

    private SelftestService selftestService;

    public SelftestRestTjeneste() {
        // CDI
    }

    @Inject
    public SelftestRestTjeneste(SelftestService selftestService) {
        this.selftestService = selftestService;
    }

    @GET
    @Produces({TEXT_HTML, APPLICATION_JSON})
    public Response doSelftest(@HeaderParam("Content-Type") String contentType, @QueryParam("json") boolean writeJsonAsHtml) {
        return selftestService.doSelftest(contentType, writeJsonAsHtml);
    }

}
