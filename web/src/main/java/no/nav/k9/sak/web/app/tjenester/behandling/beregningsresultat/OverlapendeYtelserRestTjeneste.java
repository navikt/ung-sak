package no.nav.k9.sak.web.app.tjenester.behandling.beregningsresultat;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.fpsak.tidsserie.LocalDateInterval;
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
    @Operation(description = "Hent andre overlappende ytelser som påvirker denne behandlingen", summary = ("Hent andre overlappende ytelser som påvirker denne behandlingen"), tags = "ytelser")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<OverlappendeYtelseDto> hentOverlappendeYtelser(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var overlappendeYtelser = overlappendeYtelserTjeneste.finnOverlappendeYtelser(BehandlingReferanse.fra(behandling));

        var overlappendeYtelseDtoer = overlappendeYtelser.entrySet().stream()
            .sorted(Comparator.comparing((Map.Entry<Ytelse, NavigableSet<LocalDateInterval>> e) -> e.getKey().getYtelseType().getKode())
                .thenComparing(e -> e.getKey().getKilde()))
            .map(entry -> {
                var ytelse = entry.getKey();
                var overlappendePerioder = entry.getValue().stream()
                    .map(dateInterval -> new OverlappendeYtelsePeriodeDto(dateInterval.getFomDato(), dateInterval.getTomDato()))
                    .collect(Collectors.toList());
                return new OverlappendeYtelseDto(ytelse.getYtelseType(), ytelse.getKilde(), overlappendePerioder);
            })
            .collect(Collectors.toList());

        return overlappendeYtelseDtoer;
    }

}
