package no.nav.k9.sak.web.app.tjenester.behandling.beregningsresultat;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.vedtak.ekstern.OverlappendeYtelserTjeneste;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.ytelser.OverlappendeYtelseDto;
import no.nav.k9.sak.kontrakt.ytelser.OverlappendeYtelsePeriodeDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@Path(OverlapendeYtelserRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class OverlapendeYtelserRestTjeneste {

    public static final String BASE_PATH = "/behandling/ytelser";
    public static final String OVERLAPPENDE_YTELSER_PATH = BASE_PATH;

    private BehandlingRepository behandlingRepository;
    private OverlappendeYtelserTjeneste overlappendeYtelserTjeneste;

    public OverlapendeYtelserRestTjeneste() {
        // for resteasy
    }

    @Inject
    public OverlapendeYtelserRestTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                          OverlappendeYtelserTjeneste overlappendeYtelserTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.overlappendeYtelserTjeneste = overlappendeYtelserTjeneste;
    }


    @GET
    @Operation(description = "Hent andre overlappende K9-ytelser som påvirker denne behandlingen", summary = ("Hent andre overlappende K9-ytelser som påvirker denne behandlingen"), tags = "ytelser")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<OverlappendeYtelseDto> hentOverlappendeYtelser(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var ytelseTyperSomSjekkesMot = behandling.getFagsakYtelseType().hentK9YtelserForOverlappSjekk();
        var overlappendeYtelser = overlappendeYtelserTjeneste.finnOverlappendeYtelser(BehandlingReferanse.fra(behandling), ytelseTyperSomSjekkesMot);

        return overlappendeYtelser.entrySet().stream()
            .sorted(Comparator.comparing((Map.Entry<Ytelse, ?> e) -> e.getKey().getYtelseType().getKode())
                .thenComparing(e -> e.getKey().getKilde()))
            .map(entry -> {
                var ytelse = entry.getKey();
                var overlappendePerioder = entry.getValue().stream()
                    .map(segment -> new OverlappendeYtelsePeriodeDto(segment.getFom(), segment.getTom()))
                    .toList();
                return new OverlappendeYtelseDto(ytelse.getYtelseType(), ytelse.getKilde(), ytelse.getSaksnummer(), overlappendePerioder);
            })
            .toList();
    }

}
