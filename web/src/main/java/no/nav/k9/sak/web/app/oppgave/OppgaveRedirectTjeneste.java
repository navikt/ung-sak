package no.nav.k9.sak.web.app.oppgave;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.k9.abac.BeskyttetRessursKoder;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.produksjonsstyring.OppgaveIdDto;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.filter.DoNotCache;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.net.URI;

@Path("")
@DoNotCache
@ApplicationScoped
@OpenAPIDefinition(tags = {@Tag(name = "redirect", description = "Oppgave redirect")})
public class OppgaveRedirectTjeneste {

    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private FagsakRepository fagsakRepository;
    private RedirectFactory redirectFactory; //For å kunne endre til alternativ implementasjon på Jetty

    public OppgaveRedirectTjeneste() {
    }

    @Inject
    public OppgaveRedirectTjeneste(OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository, FagsakRepository fagsakRepository, RedirectFactory redirectFactory) {
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.fagsakRepository = fagsakRepository;
        this.redirectFactory = redirectFactory;
    }

    @GET
    @Operation(description = "redirect til oppgave", tags = "redirect")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = BeskyttetRessursKoder.FAGSAK)
    public Response doRedirect(@QueryParam("oppgaveId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) OppgaveIdDto oppgaveId,
                               @QueryParam("sakId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto) {
        OppgaveRedirectData data = OppgaveRedirectData.hent(oppgaveBehandlingKoblingRepository, fagsakRepository, oppgaveId, saksnummerDto);
        String url = redirectFactory.lagRedirect(data);
        Response.ResponseBuilder responser = Response.temporaryRedirect(URI.create(url));
        responser.encoding("UTF-8");
        return responser.build();
    }

}
