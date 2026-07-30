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
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.saksbehandler.SaksbehandlerDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

@Path("/saksbehandler")
@ApplicationScoped
@Transactional
public class SaksbehandlerRestTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(SaksbehandlerRestTjeneste.class);

    private MicrosoftGraphTjeneste microsoftGraphTjeneste;

    private String systembruker;

    private String appName;
    private HistorikkinnslagRepository historikkRepository;
    private BehandlingRepository behandlingRepository;

    public SaksbehandlerRestTjeneste() {
        //NOSONAR
    }

    @Inject
    public SaksbehandlerRestTjeneste(
        MicrosoftGraphTjeneste microsoftGraphTjeneste,
        @KonfigVerdi(value = "systembruker.username", required = false) String systembruker,
        @KonfigVerdi(value = "NAIS_APP_NAME", defaultVerdi = "ung-sak") String appName,
        HistorikkinnslagRepository historikkRepository,
        BehandlingRepository behandlingRepository) {
        this.microsoftGraphTjeneste = microsoftGraphTjeneste;
        this.systembruker = systembruker;
        this.appName = appName;
        this.historikkRepository = historikkRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        description = "Returnerer fullt navn for identer som har berørt en fagsak",
        tags = "nav-ansatt",
        summary = ("Identer hentes fra historikkinnslag og aksjonspunkt.")
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
            .filter(Objects::nonNull)
            .collect(Collectors.toSet()));

        unikeIdenter.addAll(behandling.getAksjonspunkter().stream()
            .map(Aksjonspunkt::getAnsvarligSaksbehandler)
            .filter(Objects::nonNull)
            .toList());

        unikeIdenter.remove(systembruker); //bare relevant lokat
        unikeIdenter.remove(appName);

        Map<String, String> identTilNavn;
        try {
             identTilNavn = microsoftGraphTjeneste.navnPåNavAnsatte(unikeIdenter);
        } catch (Exception e) {
            logger.warn("Feil ved henting av navn for saksbehandlere fra Microsoft Graph. Returnerer tomt liste.", e);
            identTilNavn = Collections.emptyMap();
        }
        return new SaksbehandlerDto(identTilNavn);
    }

}
