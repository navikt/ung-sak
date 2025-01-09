package no.nav.ung.sak.web.app.tjenester.kravperioder;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.krav.PeriodeMedUtfall;
import no.nav.ung.sak.kontrakt.krav.StatusForPerioderPåBehandling;
import no.nav.ung.sak.kontrakt.krav.StatusForPerioderPåBehandlingInkludertVilkår;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.søknadsfrist.UngdomsytelseVurdererSøknadsfristTjeneste;
import no.nav.ung.sak.trigger.ProsessTriggere;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class PerioderTilBehandlingMedKildeRestTjeneste {

    public static final String BEHANDLING_PERIODER = "/behandling/perioder";
    public static final String BEHANDLING_PERIODER_MED_VILKÅR = "/behandling/perioder-med-vilkar";
    private BehandlingRepository behandlingRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private UngdomsytelseVurdererSøknadsfristTjeneste søknadsfristTjeneste;
    private ProsessTriggereRepository prosessTriggereRepository;

    public PerioderTilBehandlingMedKildeRestTjeneste() {
    }

    @Inject
    public PerioderTilBehandlingMedKildeRestTjeneste(BehandlingRepository behandlingRepository,
                                                     @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                                     UngdomsytelseVurdererSøknadsfristTjeneste søknadsfristTjeneste,
                                                     ProsessTriggereRepository prosessTriggereRepository) {
        this.behandlingRepository = behandlingRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
        this.prosessTriggereRepository = prosessTriggereRepository;
    }

    @GET
    @Path(BEHANDLING_PERIODER)
    @Operation(description = "Hent perioder til behandling og kilden til disse",
        summary = ("Hent perioder til behandling og kilden til disse"),
        tags = "perioder",
        responses = {
            @ApiResponse(description = "Liste med periode og årsaken til at perioden behandles"),
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public StatusForPerioderPåBehandling hentPerioderTilBehandling(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var ref = BehandlingReferanse.fra(behandling);
        StatusForPerioderPåBehandling statusForPerioderPåBehandling = getStatusForPerioderPåBehandling(ref, VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType()));

        return statusForPerioderPåBehandling;
    }

    @GET
    @Path(BEHANDLING_PERIODER_MED_VILKÅR)
    @Operation(
        description = "Hent perioder til behandling og kilden til disse",
        summary = ("Hent perioder til behandling og kilden til disse"),
        tags = "perioder",
        responses = {
            @ApiResponse(description = "Liste med periode og årsaken til at perioden behandles"),
        }
    )
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public StatusForPerioderPåBehandlingInkludertVilkår hentPerioderMedVilkårForBehandling(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var ref = BehandlingReferanse.fra(behandling);
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType());
        StatusForPerioderPåBehandling statusForPerioderPåBehandling = getStatusForPerioderPåBehandling(ref, perioderTilVurderingTjeneste);

        var timelineTilVurdering = utledTidslinjeTilVurdering(behandling, perioderTilVurderingTjeneste);

        return new StatusForPerioderPåBehandlingInkludertVilkår(statusForPerioderPåBehandling,
            mapVilkårMedUtfall(timelineTilVurdering),
            behandling.getOriginalBehandlingId()
                .map(it -> behandlingRepository.hentBehandling(it))
                .map(b -> mapVilkårMedUtfall(new LocalDateTimeline<>(List.of()))).orElse(List.of()));
    }

    private LocalDateTimeline<Utfall> utledTidslinjeTilVurdering(Behandling behandling, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        var definerendeVilkår = perioderTilVurderingTjeneste.definerendeVilkår();

        return new LocalDateTimeline<>(definerendeVilkår.stream()
            .map(it -> perioderTilVurderingTjeneste.utled(behandling.getId(), it))
            .flatMap(Collection::stream)
            .map(it -> new LocalDateSegment<>(it.toLocalDateInterval(), Utfall.IKKE_VURDERT))
            .collect(Collectors.toSet()));
    }

    private List<PeriodeMedUtfall> mapVilkårMedUtfall(LocalDateTimeline<Utfall> timelineTilVurdering) {
        LocalDateTimeline<Utfall> timeline = LocalDateTimeline.empty();

        // TODO: Map utfall

        timeline = timeline.combine(timelineTilVurdering, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return timeline.compress()
            .stream()
            .map(it -> new PeriodeMedUtfall(DatoIntervallEntitet.fra(it.getLocalDateInterval()).tilPeriode(), it.getValue()))
            .toList();
    }


    private StatusForPerioderPåBehandling getStatusForPerioderPåBehandling(BehandlingReferanse ref, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        var perioderTilVurdering = perioderTilVurderingTjeneste.utledFraDefinerendeVilkår(ref.getBehandlingId());
        var kravdokumenterTilBehandling = søknadsfristTjeneste.hentPerioderTilVurdering(ref);
        var prosesstriggere = prosessTriggereRepository.hentGrunnlag(ref.getBehandlingId());
        return UtledStatusForPerioderPåBehandling.utledStatus(
            perioderTilVurdering,
            kravdokumenterTilBehandling,
            prosesstriggere.stream().map(ProsessTriggere::getTriggere).flatMap(Collection::stream).toList()
        );
    }

}
