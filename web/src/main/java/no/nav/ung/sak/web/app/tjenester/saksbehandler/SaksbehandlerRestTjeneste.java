package no.nav.ung.sak.web.app.tjenester.saksbehandler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.integrasjon.microsoftgraph.MicrosoftGraphTjeneste;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.saksbehandler.SaksbehandlerDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

@Path("/saksbehandler")
@ApplicationScoped
@Transactional
public class SaksbehandlerRestTjeneste {
    public static final String SAKSBEHANDLER_PATH = "/saksbehandler";
    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(480, TimeUnit.MINUTES);

    private MicrosoftGraphTjeneste microsoftGraphTjeneste;

    private String systembruker;

    private HistorikkinnslagRepository historikkRepository;
    private BehandlingRepository behandlingRepository;

    public SaksbehandlerRestTjeneste() {
        //NOSONAR
    }

    @Inject
    public SaksbehandlerRestTjeneste(
        MicrosoftGraphTjeneste microsoftGraphTjeneste, @KonfigVerdi(value = "systembruker.username", required = false) String systembruker,
        HistorikkinnslagRepository historikkRepository,
        BehandlingRepository behandlingRepository) {
        this.microsoftGraphTjeneste = microsoftGraphTjeneste;
        this.systembruker = systembruker;
        this.historikkRepository = historikkRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        description = "Returnerer fullt navn for identer som har berørt en fagsak",
        tags = "nav-ansatt",
        summary = ("Identer hentes fra historikkinnslag og sykdomsvurderinger.")
    )
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK, auditlogg = false)
    public SaksbehandlerDto getSaksbehandlere(
        @QueryParam(BehandlingUuidDto.NAME)
        @Parameter(description = BehandlingUuidDto.DESC)
        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        BehandlingUuidDto behandlingUuid) {

        Behandling behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).orElseThrow();
        List<Historikkinnslag> historikkinnslag = historikkRepository.hent(behandling.getFagsak().getSaksnummer());

        Set<String> unikeIdenter = historikkinnslag.stream()
            .map(BaseEntitet::getOpprettetAv)
            .collect(Collectors.toSet());

        unikeIdenter.addAll(historikkinnslag.stream()
            .map(BaseEntitet::getEndretAv)
            .collect(Collectors.toSet()));

        unikeIdenter.remove(systembruker);

        Map<String, String> identTilNavn = microsoftGraphTjeneste.navnPåNavAnsatte(unikeIdenter);

        return new SaksbehandlerDto(identTilNavn);
    }

}
