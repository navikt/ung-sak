package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
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
import jakarta.ws.rs.core.Response;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.uttak.ArbeidsgiverMedPerioderSomManglerDto;
import no.nav.k9.sak.kontrakt.uttak.ManglendeArbeidstidDto;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.kontrakt.uttak.UttakArbeidsforhold;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriode;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriodeRepository;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.MapInputTilUttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.AktivitetIdentifikator;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.ArbeidBrukerBurdeSøktOmUtleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class PleiepengerUttakRestTjeneste {

    public static final String GET_UTTAKSPLAN_PATH = "/behandling/pleiepenger/uttak";
    public static final String GET_UTTAKSPLAN_MED_UTSATT_PERIODE_PATH = "/behandling/pleiepenger/uttak-med-utsatt";
    public static final String GET_SKULLE_SØKT_OM_PATH = "/behandling/pleiepenger/arbeidstid-mangler";
    public static final String GET_DEBUG_INPUT_PATH = "/behandling/pleiepenger/debug-input";

    private UttakTjeneste uttakRestKlient;
    private BehandlingRepository behandlingRepository;
    private ArbeidBrukerBurdeSøktOmUtleder manglendeArbeidstidUtleder;
    private MapInputTilUttakTjeneste mapInputTilUttakTjeneste;
    private UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository;

    public PleiepengerUttakRestTjeneste() {
        // for proxying
    }

    @Inject
    public PleiepengerUttakRestTjeneste(UttakTjeneste uttakRestKlient,
                                        BehandlingRepository behandlingRepository,
                                        ArbeidBrukerBurdeSøktOmUtleder manglendeArbeidstidUtleder,
                                        MapInputTilUttakTjeneste mapInputTilUttakTjeneste,
                                        UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository) {
        this.uttakRestKlient = uttakRestKlient;
        this.behandlingRepository = behandlingRepository;
        this.manglendeArbeidstidUtleder = manglendeArbeidstidUtleder;
        this.mapInputTilUttakTjeneste = mapInputTilUttakTjeneste;
        this.utsattBehandlingAvPeriodeRepository = utsattBehandlingAvPeriodeRepository;
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(GET_UTTAKSPLAN_PATH)
    @Operation(description = "Hent uttaksplan for behandling", tags = "behandling - pleiepenger/uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer uttaksplan for angitt behandling", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Uttaksplan.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Uttaksplan getUttaksplan(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        return uttakRestKlient.hentUttaksplan(behandlingIdDto.getBehandlingUuid(), true);
    }


    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(GET_UTTAKSPLAN_MED_UTSATT_PERIODE_PATH)
    @Operation(description = "Hent uttaksplan for behandling med utsatte perioder", tags = "behandling - pleiepenger/uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer uttaksplan for angitt behandling", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UttaksplanMedUtsattePerioder.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public UttaksplanMedUtsattePerioder uttaksplanMedUtsattePerioder(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        var uttaksplan = uttakRestKlient.hentUttaksplan(behandlingIdDto.getBehandlingUuid(), true);
        var behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        var utsattBehandlingAvPeriode = utsattBehandlingAvPeriodeRepository.hentGrunnlag(behandling.getId());

        var utsattePerioder = utsattBehandlingAvPeriode.stream()
            .map(UtsattBehandlingAvPeriode::getPerioder)
            .flatMap(Collection::stream)
            .map(it -> new LukketPeriode(it.getPeriode().getFomDato(), it.getPeriode().getTomDato()))
            .collect(Collectors.toSet());

        return new UttaksplanMedUtsattePerioder(uttaksplan, utsattePerioder);
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(GET_SKULLE_SØKT_OM_PATH)
    @Operation(description = "Henter ut arbeidstid som bruker skulle oppgitt", tags = "behandling - pleiepenger/uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Henter ut arbeidstid som bruker skulle oppgitt", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ManglendeArbeidstidDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public ManglendeArbeidstidDto getArbeidstidSomMangler(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        var behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());

        var mangler = manglendeArbeidstidUtleder.utledMangler(BehandlingReferanse.fra(behandling));
        return new ManglendeArbeidstidDto(mangler.entrySet()
            .stream()
            .map(this::mapArbeidsgiver).collect(Collectors.toList()));
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path(GET_DEBUG_INPUT_PATH)
    @Operation(description = "Henter ut uttaksgrunnlag for behandling", tags = "behandling - pleiepenger/uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Uttaksgrunnlag", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response debugInput(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        var behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());

        var uttaksgrunnlag = mapInputTilUttakTjeneste.hentUtOgMapRequest(BehandlingReferanse.fra(behandling));

        return Response.ok(uttaksgrunnlag).build();
    }

    private ArbeidsgiverMedPerioderSomManglerDto mapArbeidsgiver(Map.Entry<AktivitetIdentifikator, LocalDateTimeline<Boolean>> entry) {
        var arbeidsgiver = entry.getKey().getArbeidsgiver();
        var uttakArbeidsgiver = new UttakArbeidsforhold(arbeidsgiver != null ? arbeidsgiver.getArbeidsgiverOrgnr() : null, arbeidsgiver != null ? arbeidsgiver.getAktørId() : null, entry.getKey().getAktivitetType(), null);
        var perioder = entry.getValue().stream().map(it -> new Periode(it.getFom(), it.getTom())).collect(Collectors.toList());

        return new ArbeidsgiverMedPerioderSomManglerDto(uttakArbeidsgiver, perioder);
    }

}
