package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.uttak.FastsattUttakDto;
import no.nav.k9.sak.kontrakt.uttak.UtenlandsoppholdDto;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PSBVurdererSøknadsfristTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.PeriodeFraSøknadForBrukerTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.utenlandsopphold.MapUtenlandsopphold;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.HentPerioderTilVurderingTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.UtenlandsoppholdInfo;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class UtenlandsoppholdRestTjeneste {
    static final String BASE_PATH = "/behandling/uttak";

    public static final String UTTAK_UTENLANDSOPPHOLD = BASE_PATH + "/utenlandsopphold";

    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private BehandlingRepository behandlingRepository;

    private PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste;
    private PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste;
    private HentPerioderTilVurderingTjeneste hentPerioderTilVurderingTjeneste;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    public UtenlandsoppholdRestTjeneste() {
    }

    @Inject
    public UtenlandsoppholdRestTjeneste(
            UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
            BehandlingRepository behandlingRepository,
            PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste,
            PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste,
            HentPerioderTilVurderingTjeneste hentPerioderTilVurderingTjeneste,
            SøknadsperiodeTjeneste søknadsperiodeTjeneste,
            Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
        this.periodeFraSøknadForBrukerTjeneste = periodeFraSøknadForBrukerTjeneste;
        this.hentPerioderTilVurderingTjeneste = hentPerioderTilVurderingTjeneste;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    @GET
    @Path(UTTAK_UTENLANDSOPPHOLD)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent oppgitt utenlandsopphold", tags = "behandling - uttak",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returnerer søkers oppgitte utenlandsopphold, tom liste hvis det ikke finnes noe",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = FastsattUttakDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public UtenlandsoppholdDto getUtenlandsopphold(
        @NotNull @QueryParam(BehandlingUuidDto.NAME)
        @Parameter(description = BehandlingUuidDto.DESC) @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingIdDto) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        Optional<UttaksPerioderGrunnlag> uttaksPerioderGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(behandling.getId());
        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        var vurderteSøknadsperioder = søknadsfristTjeneste.vurderSøknadsfrist(behandlingReferanse);
        var perioderFraSøknad = periodeFraSøknadForBrukerTjeneste.hentPerioderFraSøknad(behandlingReferanse);

        NavigableSet<DatoIntervallEntitet> perioderTilVurderingTidslinje = hentPerioderTilVurderingTjeneste.hentPerioderTilVurderingUtenUbesluttet(behandling);

        LocalDateTimeline<Boolean> tidslinjeTilVurdering =
            new LocalDateTimeline<>(perioderTilVurderingTidslinje
                .stream()
                .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true))
                .collect(Collectors.toList()))
                .compress();

        Map<LukketPeriode, UtenlandsoppholdInfo> utenlandsopphold = MapUtenlandsopphold.map(vurderteSøknadsperioder, perioderFraSøknad, tidslinjeTilVurdering);

        utenlandsopphold.entrySet().stream().map(e -> e.)
    }

}
