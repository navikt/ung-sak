package no.nav.ung.sak.web.app.oppgave;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.kontrakt.produksjonsstyring.OppgaveIdDto;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

@Path("")
@ApplicationScoped
@OpenAPIDefinition(tags = {@Tag(name = "redirect", description = "Oppgave redirect")})
public class OppgaveRedirectTjeneste {
    private static final Logger log = LoggerFactory.getLogger(OppgaveRedirectTjeneste.class);

    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private FagsakRepository fagsakRepository;
    private RedirectFactory redirectFactory = new RedirectFactory();

    public OppgaveRedirectTjeneste() {
    }

    @Inject
    public OppgaveRedirectTjeneste(OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository, FagsakRepository fagsakRepository) {
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.fagsakRepository = fagsakRepository;
    }

    @GET
    @Operation(description = "redirect til oppgave", tags = "redirect")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.FAGSAK)
    public Response doRedirect(@QueryParam("oppgaveId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) OppgaveIdDto oppgaveId,
                               @QueryParam("sakId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto) {
        OppgaveRedirectData data = OppgaveRedirectData.hent(oppgaveBehandlingKoblingRepository, fagsakRepository, oppgaveId, saksnummerDto);
        String url = redirectFactory.lagRedirect(data);
        Response.ResponseBuilder responser = Response.temporaryRedirect(URI.create(url));
        log.warn("Mottok oppgaveredirect for : {}", url);
        responser.encoding("UTF-8");
        return responser.build();
    }

}
