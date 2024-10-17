package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrUttakRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrtUttakPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.uttak.søskensaker.EgneOverlappendeSakerDto;
import no.nav.k9.sak.kontrakt.uttak.søskensaker.PeriodeMedOverlapp;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.søsken.FinnTidslinjeForOverlappendeSøskensaker;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class EgneOverlappendeSakerRestTjeneste {

    public static final String EGNE_OVERLAPPENDE_SAKER = "/behandling/pleiepenger/uttak/egne-overlappende-saker";

    private BehandlingRepository behandlingRepository;
    private OverstyrUttakRepository overstyrUttakRepository;
    private FinnTidslinjeForOverlappendeSøskensaker finnTidslinjeForOverlappendeSøskensaker;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    public EgneOverlappendeSakerRestTjeneste() {
        // for proxying
    }

    @Inject
    public EgneOverlappendeSakerRestTjeneste(BehandlingRepository behandlingRepository,
                                             OverstyrUttakRepository overstyrUttakRepository,
                                             FinnTidslinjeForOverlappendeSøskensaker finnTidslinjeForOverlappendeSøskensaker,
                                             @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester) {
        this.behandlingRepository = behandlingRepository;
        this.overstyrUttakRepository = overstyrUttakRepository;
        this.finnTidslinjeForOverlappendeSøskensaker = finnTidslinjeForOverlappendeSøskensaker;
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
    }


    @POST
    @Path(EGNE_OVERLAPPENDE_SAKER)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent egne overlappende saker for perioder til behandling", tags = "behandling - uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer egne overlappende saker", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EgneOverlappendeSakerDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public EgneOverlappendeSakerDto hentEgneOverlappendeSaker(@NotNull @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        UUID behandlingUuid = behandlingIdDto.getBehandlingUuid();
        var behandling = behandlingRepository.hentBehandling(behandlingUuid);
        var overlappendeSaker = finnTidslinjeForOverlappendeSøskensaker.finnTidslinje(behandling.getAktørId(), behandling.getFagsakYtelseType());
        var saksnummer = behandling.getFagsak().getSaksnummer();
        var overlapperMedDenneSaken = overlappendeSaker.filterValue(v -> v.contains(saksnummer))
            .mapValue(v -> {
                var ny = new HashSet<>(v);
                ny.remove(saksnummer);
                return ny;
            });

        var overstyrtUttak = overstyrUttakRepository.hentOverstyrtUttak(behandling.getId());
        var tidslinjeTilVurdering = finnTidslinjeTilVurdering(behandling);
        var kombinertMedFastsattUttaksgrad = finnKombinertTidslinje(overlapperMedDenneSaken, overstyrtUttak, tidslinjeTilVurdering);
        var overlappendePerioder = kombinertMedFastsattUttaksgrad.toSegments().stream().map(s -> new PeriodeMedOverlapp(new Periode(s.getFom(), s.getTom()),
            s.getValue().tilVurdering(),
            s.getValue().fastsattUttaksgrad(),
            s.getValue().saksnummer())).toList();
        return new EgneOverlappendeSakerDto(overlappendePerioder);
    }

    private static LocalDateTimeline<OverlappData> finnKombinertTidslinje(LocalDateTimeline<HashSet<Saksnummer>> overlapperMedDenneSaken, LocalDateTimeline<OverstyrtUttakPeriode> overstyrtUttak, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        return overlapperMedDenneSaken.crossJoin(overstyrtUttak, (di, lhs, rhs) -> {
            var erTilVurdering = !tidslinjeTilVurdering.intersection(di).isEmpty();
            if (lhs != null && rhs != null) {
                return new LocalDateSegment<>(di, new OverlappData(erTilVurdering, rhs.getValue().getSøkersUttaksgrad(), lhs.getValue()));
            } else if (lhs != null) {
                return new LocalDateSegment<>(di, new OverlappData(erTilVurdering, null, lhs.getValue()));
            }
            return new LocalDateSegment<>(di, new OverlappData(erTilVurdering, rhs.getValue().getSøkersUttaksgrad(), Set.of()));
        });
    }

    private LocalDateTimeline<Boolean> finnTidslinjeTilVurdering(Behandling behandling) {
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
        // Henter alle perioder til vurdering
        var perioderTilVurdering = perioderTilVurderingTjeneste.utledFraDefinerendeVilkår(behandling.getId());
        return TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurdering);
    }


    public record OverlappData(
        Boolean tilVurdering,
        BigDecimal fastsattUttaksgrad,
        Set<Saksnummer> saksnummer
    ) {
    }


}
