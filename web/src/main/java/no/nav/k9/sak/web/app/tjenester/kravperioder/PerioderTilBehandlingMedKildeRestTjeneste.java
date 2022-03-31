package no.nav.k9.sak.web.app.tjenester.kravperioder;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.krav.PeriodeMedUtfall;
import no.nav.k9.sak.kontrakt.krav.StatusForPerioderPåBehandling;
import no.nav.k9.sak.kontrakt.krav.StatusForPerioderPåBehandlingInkludertVilkår;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.søknadsfrist.SøknadsfristTjenesteProvider;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class PerioderTilBehandlingMedKildeRestTjeneste {

    public static final String BEHANDLING_PERIODER = "/behandling/perioder";
    public static final String BEHANDLING_PERIODER_MED_VILKÅR = "/behandling/perioder-med-vilkar";
    private BehandlingRepository behandlingRepository;
    private BehandlingModellRepository behandlingModellRepository;
    private UttakTjeneste uttakTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private SøknadsfristTjenesteProvider søknadsfristTjenesteProvider;
    private UtledStatusPåPerioderTjeneste statusPåPerioderTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    public PerioderTilBehandlingMedKildeRestTjeneste() {
    }

    @Inject
    public PerioderTilBehandlingMedKildeRestTjeneste(BehandlingRepository behandlingRepository,
                                                     @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                                     BehandlingModellRepository behandlingModellRepository,
                                                     UttakTjeneste uttakTjeneste,
                                                     VilkårResultatRepository vilkårResultatRepository,
                                                     SøknadsfristTjenesteProvider søknadsfristTjenesteProvider) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingModellRepository = behandlingModellRepository;
        this.uttakTjeneste = uttakTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.søknadsfristTjenesteProvider = søknadsfristTjenesteProvider;
        this.statusPåPerioderTjeneste = new UtledStatusPåPerioderTjeneste();
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    @GET
    @Path(BEHANDLING_PERIODER)
    @Operation(description = "Hent perioder til behandling og kilden til disse",
        summary = ("Hent perioder til behandling og kilden til disse"),
        tags = "perioder",
        responses = {
            @ApiResponse(responseCode = "200", description = "Liste med periode og årsaken til at perioden behandles", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = StatusForPerioderPåBehandling.class))
            }),
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public StatusForPerioderPåBehandling hentPerioderTilBehandling(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var ref = BehandlingReferanse.fra(behandling);
        StatusForPerioderPåBehandling statusForPerioderPåBehandling = getStatusForPerioderPåBehandling(ref, behandling, VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType()));

        return statusForPerioderPåBehandling;
    }

    @GET
    @Path(BEHANDLING_PERIODER_MED_VILKÅR)
    @Operation(description = "Hent perioder til behandling og kilden til disse",
        summary = ("Hent perioder til behandling og kilden til disse"),
        tags = "perioder",
        responses = {
            @ApiResponse(responseCode = "200", description = "Liste med periode og årsaken til at perioden behandles", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = StatusForPerioderPåBehandling.class))
            }),
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public StatusForPerioderPåBehandlingInkludertVilkår hentPerioderMedVilkårForBehandling(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var ref = BehandlingReferanse.fra(behandling);
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType());
        StatusForPerioderPåBehandling statusForPerioderPåBehandling = getStatusForPerioderPåBehandling(ref, behandling, perioderTilVurderingTjeneste);

        var timelineTilVurdering = utledTidslinjeTilVurdering(behandling, perioderTilVurderingTjeneste);

        return new StatusForPerioderPåBehandlingInkludertVilkår(statusForPerioderPåBehandling,
            mapVilkårMedUtfall(behandling, timelineTilVurdering),
            behandling.getOriginalBehandlingId()
                .map(it -> behandlingRepository.hentBehandling(it))
                .map(b -> mapVilkårMedUtfall(b, new LocalDateTimeline<>(List.of()))).orElse(List.of()));
    }

    private LocalDateTimeline<Utfall> utledTidslinjeTilVurdering(Behandling behandling, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        var definerendeVilkår = perioderTilVurderingTjeneste.definerendeVilkår();

        return new LocalDateTimeline<>(definerendeVilkår.stream()
            .map(it -> perioderTilVurderingTjeneste.utled(behandling.getId(), it))
            .flatMap(Collection::stream)
            .map(it -> new LocalDateSegment<>(it.toLocalDateInterval(), Utfall.IKKE_VURDERT))
            .collect(Collectors.toSet()));
    }

    private List<PeriodeMedUtfall> mapVilkårMedUtfall(Behandling behandling, LocalDateTimeline<Utfall> timelineTilVurdering) {
        LocalDateTimeline<Utfall> timeline = LocalDateTimeline.empty();
        if (harPSBUttak(behandling) &&
            (behandling.getStatus().erFerdigbehandletStatus() || harKommetTilUttak(behandling))) {
            var uttaksplan = uttakTjeneste.hentUttaksplan(behandling.getUuid(), true);
            List<LocalDateSegment<Utfall>> utfallFraUttak = new ArrayList<>();
            for (Map.Entry<LukketPeriode, UttaksperiodeInfo> entry : uttaksplan.getPerioder().entrySet()) {
                utfallFraUttak.add(new LocalDateSegment<>(entry.getKey().getFom(), entry.getKey().getTom(), mapUtfall(entry.getValue().getUtfall())));
            }
            timeline = new LocalDateTimeline<>(utfallFraUttak, StandardCombinators::coalesceRightHandSide);
        }

        timeline = timeline.combine(timelineTilVurdering, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return timeline.compress()
            .toSegments()
            .stream()
            .map(it -> new PeriodeMedUtfall(DatoIntervallEntitet.fra(it.getLocalDateInterval()).tilPeriode(), it.getValue()))
            .toList();
    }

    private boolean harPSBUttak(Behandling behandling) {
        return Set.of(FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE,
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
            FagsakYtelseType.OPPLÆRINGSPENGER).contains(behandling.getFagsakYtelseType());
    }

    private Utfall mapUtfall(no.nav.pleiepengerbarn.uttak.kontrakter.Utfall utfall) {
        return switch (utfall) {
            case OPPFYLT -> Utfall.OPPFYLT;
            case IKKE_OPPFYLT -> Utfall.IKKE_OPPFYLT;
        };
    }

    private boolean harKommetTilUttak(Behandling behandling) {
        final BehandlingModell modell = behandlingModellRepository.getModell(behandling.getType(), behandling.getFagsakYtelseType());
        return !modell.erStegAFørStegB(behandling.getAktivtBehandlingSteg(), BehandlingStegType.VURDER_UTTAK_V2);
    }

    private StatusForPerioderPåBehandling getStatusForPerioderPåBehandling(BehandlingReferanse ref, Behandling behandling, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        var søknadsfristTjeneste = søknadsfristTjenesteProvider.finnVurderSøknadsfristTjeneste(ref);

        var kravdokumenter = søknadsfristTjeneste.relevanteKravdokumentForBehandling(ref);
        var perioderSomSkalTilbakestilles = perioderTilVurderingTjeneste.perioderSomSkalTilbakestilles(ref.getBehandlingId());

        var kravdokumenterMedPeriode = søknadsfristTjeneste.hentPerioderTilVurdering(ref);
        var definerendeVilkår = perioderTilVurderingTjeneste.definerendeVilkår();

        var perioderTilVurdering = definerendeVilkår.stream()
            .map(it -> perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), it))
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(TreeSet::new));

        perioderTilVurdering.addAll(perioderTilVurderingTjeneste.utledUtvidetRevurderingPerioder(ref));

        var revurderingPerioderFraAndreParter = perioderTilVurderingTjeneste.utledRevurderingPerioder(ref);
        var kantIKantVurderer = perioderTilVurderingTjeneste.getKantIKantVurderer();

        var statusForPerioderPåBehandling = statusPåPerioderTjeneste.utled(behandling, kantIKantVurderer, kravdokumenter, kravdokumenterMedPeriode, perioderTilVurdering, perioderSomSkalTilbakestilles, revurderingPerioderFraAndreParter);
        return statusForPerioderPåBehandling;
    }
}
