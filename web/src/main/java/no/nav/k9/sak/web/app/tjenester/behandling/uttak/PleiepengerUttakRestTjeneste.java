package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
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
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.prosesstask.rest.AbacEmptySupplier;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrUttakRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrtUttakPeriode;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrtUttakUtbetalingsgrad;
import no.nav.k9.sak.behandlingslager.behandling.uttak.UttakNyeReglerRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.KortTekst;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsgiverOversiktDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.uttak.ArbeidsgiverMedPerioderSomManglerDto;
import no.nav.k9.sak.kontrakt.uttak.ManglendeArbeidstidDto;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.kontrakt.uttak.UttakArbeidsforhold;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakPeriodeDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakUtbetalingsgradDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrbareUttakAktiviterDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrtUttakDto;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriode;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriodeRepository;
import no.nav.k9.sak.utsatt.UtsattPeriode;
import no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold.ArbeidsgiverOversiktTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.uttak.overstyring.OverstyrbareAktiviteterForUttakRequest;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.ContainerContextRunner;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.kjøreplan.KjøreplanUtleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.MapInputTilUttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.AktivitetIdentifikator;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.ArbeidBrukerBurdeSøktOmUtleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Simulering;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class PleiepengerUttakRestTjeneste {

    public static final String GET_UTTAKSPLAN_PATH = "/behandling/pleiepenger/uttak";
    public static final String SIMULER_UTTAKSPLAN_PATH = "/behandling/pleiepenger/simuler";
    public static final String GET_UTTAKSPLAN_MED_UTSATT_PERIODE_PATH = "/behandling/pleiepenger/uttak-med-utsatt";
    public static final String GET_SKULLE_SØKT_OM_PATH = "/behandling/pleiepenger/arbeidstid-mangler";
    public static final String GET_DEBUG_INPUT_PATH = "/behandling/pleiepenger/debug-input";

    public static final String GET_DEBUG_KJØREPLAN_PATH = "/behandling/pleiepenger/debug-kjøreplan";

    public static final String UTTAK_OVERSTYRT = "/behandling/pleiepenger/uttak/overstyrt";
    public static final String UTTAK_OVERSTYRBARE_AKTIVITETER = "/behandling/pleiepenger/uttak/overstyrbare-aktiviteter";

    private UttakTjeneste uttakTjeneste;
    private BehandlingRepository behandlingRepository;
    private ArbeidBrukerBurdeSøktOmUtleder manglendeArbeidstidUtleder;
    private MapInputTilUttakTjeneste mapInputTilUttakTjeneste;
    private UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository;
    private KjøreplanUtleder kjøreplanUtleder;
    private UttakNyeReglerRepository uttakNyeReglerRepository;
    private OverstyrUttakRepository overstyrUttakRepository;

    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    private EntityManager entityManager;
    private ArbeidsgiverOversiktTjeneste arbeidsgiverOversiktTjeneste;

    public PleiepengerUttakRestTjeneste() {
        // for proxying
    }

    @Inject
    public PleiepengerUttakRestTjeneste(UttakTjeneste uttakTjeneste,
                                        BehandlingRepository behandlingRepository,
                                        ArbeidBrukerBurdeSøktOmUtleder manglendeArbeidstidUtleder,
                                        MapInputTilUttakTjeneste mapInputTilUttakTjeneste,
                                        UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository,
                                        KjøreplanUtleder kjøreplanUtleder,
                                        UttakNyeReglerRepository uttakNyeReglerRepository,
                                        OverstyrUttakRepository overstyrUttakRepository,
                                        @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
                                        EntityManager entityManager,
                                        ArbeidsgiverOversiktTjeneste arbeidsgiverOversiktTjeneste) {
        this.uttakTjeneste = uttakTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.manglendeArbeidstidUtleder = manglendeArbeidstidUtleder;
        this.mapInputTilUttakTjeneste = mapInputTilUttakTjeneste;
        this.utsattBehandlingAvPeriodeRepository = utsattBehandlingAvPeriodeRepository;
        this.kjøreplanUtleder = kjøreplanUtleder;
        this.uttakNyeReglerRepository = uttakNyeReglerRepository;
        this.overstyrUttakRepository = overstyrUttakRepository;
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.entityManager = entityManager;
        this.arbeidsgiverOversiktTjeneste = arbeidsgiverOversiktTjeneste;
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
        return uttakTjeneste.hentUttaksplan(behandlingIdDto.getBehandlingUuid(), true);
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
        var behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        var utsattePerioder = mapUtsattePerioder(behandling);
        var perioderTilVurdering = mapPerioderTilVurdering(behandling);
        final LocalDate virkningsdatoUttakNyeRegler = uttakNyeReglerRepository.finnDatoForNyeRegler(behandling.getId()).orElse(null);

        var uttaksplan = uttakTjeneste.hentUttaksplan(behandlingIdDto.getBehandlingUuid(), true);
        if (uttaksplan != null) {
            return UttaksplanMedUtsattePerioder.medUttaksplan(uttaksplan, utsattePerioder, virkningsdatoUttakNyeRegler, perioderTilVurdering);
        }

        var harAPForVurderingAvDato = behandling.getAksjonspunkter().stream().anyMatch(a -> a.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK) && !a.erAvbrutt());
        if (harAPForVurderingAvDato || virkningsdatoUttakNyeRegler != null) {
            final Uttaksgrunnlag uttaksgrunnlag = mapInputTilUttakTjeneste.hentUtOgMapRequestUtenInntektsgradering(BehandlingReferanse.fra(behandling));
            var simulerUttaksplan = uttakTjeneste.simulerUttaksplan(uttaksgrunnlag);
            return UttaksplanMedUtsattePerioder.medSimulertUttaksplan(simulerUttaksplan.getSimulertUttaksplan(), utsattePerioder, virkningsdatoUttakNyeRegler, perioderTilVurdering);
        }

        return UttaksplanMedUtsattePerioder.medUttaksplan(null, utsattePerioder, null, perioderTilVurdering);
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(SIMULER_UTTAKSPLAN_PATH)
    @Operation(description = "Simuler uttaksplan mot ubesluttede data", tags = "behandling - pleiepenger/uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer simulert uttaksplan for angitt behandling", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Simulering.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Simulering simulertUttaksplan(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        final var behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        final var ref = BehandlingReferanse.fra(behandling);

        final Uttaksgrunnlag uttaksgrunnlag = mapInputTilUttakTjeneste.hentUtUbesluttededataOgMapRequest(ref);
        return uttakTjeneste.simulerUttaksplan(uttaksgrunnlag);
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
            .filter(it -> !it.getValue().isEmpty())
            .map(this::mapArbeidsgiver)
            .collect(Collectors.toList()));
    }

    @POST
    @Path(GET_DEBUG_INPUT_PATH)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter ut uttaksgrunnlag for behandling", tags = "behandling - pleiepenger/uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Uttaksgrunnlag", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response debugInput(@NotNull @FormParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto,
                               @NotNull @FormParam("begrunnelse") @Parameter(description = "begrunnelse", allowEmptyValue = false, required = true, schema = @Schema(type = "string", maximum = "2000")) @Valid @TilpassetAbacAttributt(supplierClass = AbacEmptySupplier.class) KortTekst begrunnelse) {
        var behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());

        entityManager.persist(new DiagnostikkFagsakLogg(behandling.getFagsak().getId(), GET_DEBUG_INPUT_PATH, begrunnelse.getTekst()));
        entityManager.flush();

        // Gjer kall via egen tråd for å kunne kalle med system kontekst (kreves ved kall til kalkulus)
        var uttaksgrunnlag = ContainerContextRunner.doRun(behandling, () -> mapInputTilUttakTjeneste.hentUtOgMapRequest(BehandlingReferanse.fra(behandling)));

        return Response.ok(uttaksgrunnlag).build();
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path(GET_DEBUG_KJØREPLAN_PATH)
    @Operation(description = "Henter ut uttaksgrunnlag for behandling", tags = "behandling - pleiepenger/uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Uttaksgrunnlag", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response kjøreplan(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        var behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());

        var referanse = BehandlingReferanse.fra(behandling);
        var input = kjøreplanUtleder.utledInput(referanse);
        var kjøreplan = kjøreplanUtleder.utled(referanse);

        var respons = new DebugKjøreplan(input, kjøreplan.getPlan());

        return Response.ok(respons).build();
    }

    @GET
    @Path(UTTAK_OVERSTYRT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent overstyrt uttak for behandling", tags = "behandling - uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer uttak overstyrt av overstyrer, null hvis ikke finnes noe", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OverstyrtUttakDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public OverstyrtUttakDto getOverstyrtUttak(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        UUID behandlingUuid = behandlingIdDto.getBehandlingUuid();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid);
        LocalDateTimeline<OverstyrtUttakPeriode> overstyrtUttak = overstyrUttakRepository.hentOverstyrtUttak(behandling.getId());
        if (overstyrtUttak.isEmpty()) {
            return null;
        }
        ArbeidsgiverOversiktDto arbeidsgiverOversikt = arbeidsgiverOversiktTjeneste.getArbeidsgiverOpplysninger(behandlingUuid);
        return new OverstyrtUttakDto(overstyrtUttak.stream().map(this::map).toList(), arbeidsgiverOversikt);
    }

    @POST
    @Path(UTTAK_OVERSTYRBARE_AKTIVITETER)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent overstyrbare aktiviteter for uttak for behandling", tags = "behandling - uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer overstyrbare aktiviteter", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OverstyrbareUttakAktiviterDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public OverstyrbareUttakAktiviterDto hentOverstyrbareAktiviterForUttak(@NotNull @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) OverstyrbareAktiviteterForUttakRequest request) {
        BehandlingUuidDto behandlingIdDto = request.getBehandlingIdDto();
        UUID behandlingUuid = behandlingIdDto.getBehandlingUuid();
        no.nav.k9.sak.typer.Periode periode = new no.nav.k9.sak.typer.Periode(request.getFom(), request.getTom());
        Uttaksplan uttaksplan = uttakTjeneste.hentUttaksplan(behandlingUuid, false);

        Set<OverstyrUttakArbeidsforholdDto> aktiviteter = new LinkedHashSet<>();

        for (Map.Entry<LukketPeriode, UttaksperiodeInfo> entry : uttaksplan.getPerioder().entrySet()) {
            no.nav.k9.sak.typer.Periode uttakperiode = new no.nav.k9.sak.typer.Periode(entry.getKey().getFom(), entry.getKey().getTom());
            if (uttakperiode.overlaps(periode)) {
                UttaksperiodeInfo periodeInfo = entry.getValue();
                for (Utbetalingsgrader utbetalingsgrader : periodeInfo.getUtbetalingsgrader()) {
                    aktiviteter.add(map(utbetalingsgrader.getArbeidsforhold()));
                }
            }
        }

        ArbeidsgiverOversiktDto arbeidsgiverOversikt = arbeidsgiverOversiktTjeneste.getArbeidsgiverOpplysninger(behandlingUuid);
        return new OverstyrbareUttakAktiviterDto(new ArrayList<>(aktiviteter), arbeidsgiverOversikt);
    }

    private Set<LukketPeriode> mapPerioderTilVurdering(Behandling behandling) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType())
            .utledFraDefinerendeVilkår(behandling.getId())
            .stream()
            .map(p -> new LukketPeriode(p.getFomDato(), p.getTomDato()))
            .collect(Collectors.toSet());
    }

    private Set<LukketPeriode> mapUtsattePerioder(Behandling behandling) {
        var utsattBehandlingAvPeriode = utsattBehandlingAvPeriodeRepository.hentGrunnlag(behandling.getId());

        var utsattePerioderSegmenter = utsattBehandlingAvPeriode.stream()
            .map(UtsattBehandlingAvPeriode::getPerioder)
            .flatMap(Collection::stream)
            .map(UtsattPeriode::getPeriode)
            .map(it -> new LocalDateSegment<>(it.toLocalDateInterval(), true))
            .collect(Collectors.toList());

        return new LocalDateTimeline<>(utsattePerioderSegmenter)
            .compress()
            .getLocalDateIntervals()
            .stream()
            .map(DatoIntervallEntitet::fra)
            .collect(Collectors.toCollection(TreeSet::new))
            .stream()
            .map(it -> new LukketPeriode(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toSet());
    }


    private OverstyrUttakPeriodeDto map(LocalDateSegment<OverstyrtUttakPeriode> periode) {
        return new OverstyrUttakPeriodeDto(periode.getValue().getId(), new no.nav.k9.sak.typer.Periode(periode.getFom(), periode.getTom()), periode.getValue().getSøkersUttaksgrad(), map(periode.getValue().getOverstyrtUtbetalingsgrad()), periode.getValue().getBegrunnelse(), periode.getValue().getSaksbehandler());
    }

    private List<OverstyrUttakUtbetalingsgradDto> map(Set<OverstyrtUttakUtbetalingsgrad> overstyrtUtbetalingsgrad) {
        return overstyrtUtbetalingsgrad.stream().map(this::map).toList();
    }

    private OverstyrUttakUtbetalingsgradDto map(OverstyrtUttakUtbetalingsgrad overstyrtUtbetalingsgrad) {
        OverstyrUttakArbeidsforholdDto aktivitet = new OverstyrUttakArbeidsforholdDto(
            overstyrtUtbetalingsgrad.getAktivitetType(),
            overstyrtUtbetalingsgrad.getArbeidsgiver() != null && overstyrtUtbetalingsgrad.getArbeidsgiver().getOrgnr() != null ? new OrgNummer(overstyrtUtbetalingsgrad.getArbeidsgiver().getOrgnr()) : null,
            overstyrtUtbetalingsgrad.getArbeidsgiver() != null ? overstyrtUtbetalingsgrad.getArbeidsgiver().getAktørId() : null,
            overstyrtUtbetalingsgrad.getInternArbeidsforholdRef());
        return new OverstyrUttakUtbetalingsgradDto(aktivitet, overstyrtUtbetalingsgrad.getUtbetalingsgrad());
    }

    private OverstyrUttakArbeidsforholdDto map(Arbeidsforhold arbeidsforhold) {
        return new OverstyrUttakArbeidsforholdDto(
            UttakArbeidType.fraKode(arbeidsforhold.getType()),
            arbeidsforhold.getOrganisasjonsnummer() != null ? new OrgNummer(arbeidsforhold.getOrganisasjonsnummer()) : null,
            arbeidsforhold.getAktørId() != null ? new AktørId(arbeidsforhold.getAktørId()) : null,
            arbeidsforhold.getArbeidsforholdId() != null ? InternArbeidsforholdRef.ref(arbeidsforhold.getArbeidsforholdId()) : null
        );
    }


    private ArbeidsgiverMedPerioderSomManglerDto mapArbeidsgiver(Map.Entry<AktivitetIdentifikator, LocalDateTimeline<Boolean>> entry) {
        var arbeidsgiver = entry.getKey().getArbeidsgiver();
        var uttakArbeidsgiver = new UttakArbeidsforhold(arbeidsgiver != null ? arbeidsgiver.getArbeidsgiverOrgnr() : null, arbeidsgiver != null ? arbeidsgiver.getAktørId() : null, entry.getKey().getAktivitetType(), null);
        var perioder = entry.getValue().stream().map(it -> new Periode(it.getFom(), it.getTom())).collect(Collectors.toList());

        return new ArbeidsgiverMedPerioderSomManglerDto(uttakArbeidsgiver, perioder);
    }


}
