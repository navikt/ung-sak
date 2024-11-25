package no.nav.ung.sak.web.app.tjenester.microsoftgraph;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.util.Optional;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.ung.abac.BeskyttetRessursKoder.APPLIKASJON;


@Path("/forvaltning/saksbehandler")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class ForvaltningTestRestTjeneste {

    private MicrosoftGraphTjeneste microsoftGraphTjeneste;

    public ForvaltningTestRestTjeneste() {
    }

    @Inject
    public ForvaltningTestRestTjeneste(MicrosoftGraphTjeneste microsoftGraphTjeneste) {
        this.microsoftGraphTjeneste = microsoftGraphTjeneste;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/saksbehandlernavn")
    @BeskyttetRessurs(action = READ, resource = APPLIKASJON)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response finnNavnPåSakbehandler(@Parameter(description = "saksbehandleIdent") @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) @Valid String saksbehandler) {
        if (Environment.current().isProd()) {
            throw new IllegalArgumentException("Kun tiltenkt brukt i test");
        }
        Optional<String> resultat = microsoftGraphTjeneste.navnPåNavAnsatt(saksbehandler);
        if (resultat.isPresent()) {
            return Response.ok(resultat.get()).build();
        }
        return Response.noContent().build();
    }
}
