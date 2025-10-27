package no.nav.ung.sak.web.app.tjenester.saksbehandler;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.ung.sak.web.server.abac.NavAnsatttRestKlient;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

@Path("/nav-ansatt")
@ApplicationScoped
@Transactional
public class NavAnsattRestTjeneste {
    public static final String NAV_ANSATT_PATH = "/nav-ansatt";

    private NavAnsatttRestKlient navAnsattRestKlient;

    public NavAnsattRestTjeneste() {
        //NOSONAR
    }

    @Inject
    public NavAnsattRestTjeneste(NavAnsatttRestKlient navAnsattRestKlient) {
        this.navAnsattRestKlient = navAnsattRestKlient;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        description = "Returnerer fullt navn for ident",
        tags = "nav-ansatt",
        summary = ("Ident hentes fra sikkerhetskonteksten som er tilgjengelig etter innlogging.")
    )
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.APPLIKASJON, auditlogg = false)
    public no.nav.sif.abac.kontrakt.abac.InnloggetAnsattDto innloggetBruker() {
        return navAnsattRestKlient.tilangerForInnloggetBruker();
    }

}
