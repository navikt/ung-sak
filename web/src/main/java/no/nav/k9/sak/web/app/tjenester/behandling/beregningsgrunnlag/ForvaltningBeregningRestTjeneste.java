package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.AbacDto;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.felles.util.InputValideringRegex;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.prosesstask.rest.AbacEmptySupplier;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.FinnPerioderMedStartIKontrollerFakta;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.SkalForlengeAktivitetstatus;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.KortTekst;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningAktivitet;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningInputPeriode;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrOpphørsdatoRefusjon;
import no.nav.k9.sak.trigger.ProsessTriggerForvaltningTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.ContainerContextRunner;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.k9.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.PleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering;

@ApplicationScoped
@Transactional
@Path("/beregning")
@Produces(MediaType.APPLICATION_JSON)
public class ForvaltningBeregningRestTjeneste {

    private static final MediaType JSON = MediaType.APPLICATION_JSON_TYPE;

    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private RevurderBeregningTjeneste revurderBeregningTjeneste;
    private EntityManager entityManager;
    private FagsakTjeneste fagsakTjeneste;
    private HentKalkulatorInputDump hentKalkulatorInputDump;
    private ProsessTriggerForvaltningTjeneste prosessTriggerForvaltningTjeneste;
    private PleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering pleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering;
    private FinnPerioderMedStartIKontrollerFakta finnPerioderMedStartIKontrollerFakta;
    private ForvaltningOverstyrInputBeregning forvaltningOverstyrInputBeregning;


    public ForvaltningBeregningRestTjeneste() {
    }

    @Inject
    public ForvaltningBeregningRestTjeneste(BehandlingRepository behandlingRepository,
                                            InntektArbeidYtelseTjeneste iayTjeneste,
                                            BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                            RevurderBeregningTjeneste revurderBeregningTjeneste,
                                            EntityManager entityManager, FagsakTjeneste fagsakTjeneste,
                                            HentKalkulatorInputDump hentKalkulatorInputDump,
                                            @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) @VilkårTypeRef(VilkårType.BEREGNINGSGRUNNLAGVILKÅR) PleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering pleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering,
                                            FinnPerioderMedStartIKontrollerFakta finnPerioderMedStartIKontrollerFakta,
                                            ForvaltningOverstyrInputBeregning forvaltningOverstyrInputBeregning) {
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.revurderBeregningTjeneste = revurderBeregningTjeneste;
        this.entityManager = entityManager;
        this.fagsakTjeneste = fagsakTjeneste;
        this.hentKalkulatorInputDump = hentKalkulatorInputDump;
        this.prosessTriggerForvaltningTjeneste = new ProsessTriggerForvaltningTjeneste(entityManager);
        this.pleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering = pleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering;
        this.finnPerioderMedStartIKontrollerFakta = finnPerioderMedStartIKontrollerFakta;
        this.forvaltningOverstyrInputBeregning = forvaltningOverstyrInputBeregning;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent kalkulatorinput for behandling", tags = "beregning", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer kalkulatorinput på JSON format", content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = KalkulatorInputPrVilkårperiodeDto.class)), mediaType = MediaType.APPLICATION_JSON)),
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getKalkulatorInput(@QueryParam("behandlingId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) BehandlingIdDto behandlingIdDto) { // NOSONAR
        var behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        List<KalkulatorInputPrVilkårperiodeDto> inputListe = hentKalkulatorInputDump.getKalkulatorInputPrVilkårperiodeDtos(ref);

        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        cc.setPrivate(true);

        return Response.ok(inputListe, JSON).cacheControl(cc).build();
    }


    @POST
    @Path("inntektsmelding-sortering")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Sorterte inntektsmeldinger per vilkårsperiode", tags = "beregning",
        responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer kalkulatorinput på JSON format", content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = Map.class), schema = @Schema(implementation = Inntektsmelding.class)), mediaType = MediaType.APPLICATION_JSON)),
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response inntektsmeldingSortering(@QueryParam("behandlingId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) BehandlingIdDto behandlingIdDto) { // NOSONAR
        var behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(ref.getSaksnummer());

        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true);

        Map<DatoIntervallEntitet, Set<Inntektsmelding>> fordeltInntektsmelding = new HashMap<>();

        perioderTilVurdering.forEach(periode -> fordeltInntektsmelding.put(periode,
            sakInntektsmeldinger.stream()
                .filter(it -> it.getOppgittFravær()
                    .stream()
                    .anyMatch(at -> DatoIntervallEntitet.fraOgMedTilOgMed(at.getFom(), at.getTom()).overlapper(periode))
                ).collect(Collectors.toSet())));
        fordeltInntektsmelding.put(behandling.getFagsak().getPeriode(), sakInntektsmeldinger.stream()
            .filter(it -> perioderTilVurdering.stream()
                .noneMatch(at -> it.getOppgittFravær().stream()
                    .noneMatch(of -> DatoIntervallEntitet.fraOgMedTilOgMed(of.getFom(), of.getTom()).overlapper(at))))
            .collect(Collectors.toSet()));

        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        cc.setPrivate(true);

        return Response.ok(fordeltInntektsmelding, JSON).cacheControl(cc).build();
    }


    @POST
    @Path("psb-aktivitetstatus-forlengelse-data")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Data til utledning av forlengelse av aktivitetstatus", tags = "beregning")
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response finnAktivitetstatusForlengelseData(@NotNull @FormParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto,
                                                       @NotNull @FormParam("begrunnelse") @Parameter(description = "begrunnelse", allowEmptyValue = false, required = true, schema = @Schema(type = "string", maximum = "2000")) @Valid @TilpassetAbacAttributt(supplierClass = AbacEmptySupplier.class) KortTekst begrunnelse) { // NOSONAR
        Behandling behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        var ref = BehandlingReferanse.fra(behandling);

        // Kaller i egen context for å kunne hente inntektsmeldinger fra abakus
        var skalForlengeAktivitetstatusInput = ContainerContextRunner.doRun(behandling, () -> {
            var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledDetaljertPerioderTilVurdering(ref);
            return finnPerioderMedStartIKontrollerFakta.lagInput(ref, perioderTilVurdering);
        });

        var skalForlengeAktivitetstatus = new SkalForlengeAktivitetstatus(pleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering);
        var dataTilUtledning = skalForlengeAktivitetstatus.finnDataForUtledning(skalForlengeAktivitetstatusInput);
        var output = new SkalForlengeAktivitetstatusUtledningPerioder(
            dataTilUtledning.kravForForlengelseTidslinjer().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getLocalDateIntervals())),
            dataTilUtledning.årsakTilIngenForlengelseTidslinjer().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getLocalDateIntervals()))
        );
        loggForvaltningTjeneste(behandling.getFagsak(), "/psb-aktivitetstatus-forlengelse-data", begrunnelse.getTekst());
        return Response.ok(output, JSON).build();
    }


    @POST
    @Path("/manuell-revurdering-beregning")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter manuell revurdering grunnet nye opplysninger om beregning.", summary = ("Oppretter manuell revurdering grunnet nye opplysninger om beregning."), tags = "beregning")
    @BeskyttetRessurs(action = CREATE, resource = DRIFT)
    public void revurderGrunnetEndretOpplysning(@Parameter(description = "Saksnummer og skjæringstidspunkt (YYYY-MM-DD) på csv-format") @Valid OpprettManuellRevurderingBeregning opprettManuellRevurdering) {
        opprettProsesstriggerOgRevurder(opprettManuellRevurdering, "/manuell-revurdering-beregning", "kjører manuell revurdering/tilbakehopp grunnet nye opplysninger om beregningsgrunnlag", BehandlingÅrsakType.RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG);
    }

    @POST
    @Path("/manuell-revurdering-opptjening")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter manuell revurdering grunnet nye opplysninger om opptjening.", summary = ("Oppretter manuell revurdering grunnet nye opplysninger om opptjening."), tags = "beregning")
    @BeskyttetRessurs(action = CREATE, resource = DRIFT)
    public void revurderGrunnetOpplysningOmOpptjening(@Parameter(description = "Saksnummer og skjæringstidspunkt (YYYY-MM-DD) på csv-format") @Valid OpprettManuellRevurderingBeregning opprettManuellRevurdering) {
        opprettProsesstriggerOgRevurder(opprettManuellRevurdering, "/manuell-revurdering-opptjening", "kjører manuell revurdering/tilbakehopp grunnet nye opplysninger om opptjening", BehandlingÅrsakType.RE_OPPLYSNINGER_OM_OPPTJENING);
    }

    @POST
    @Path("/revurder-innhent-pgi")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter manuell revurdering for reinnhenting av PGI.", summary = ("Oppretter manuell revurdering for reinnhenting av PGI."), tags = "beregning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public void revurderOgInnhentPGI(@Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) RevurderPeriodeDto revurderPeriodeDto) {
        revurderBeregningTjeneste.revurderMedÅrsak(revurderPeriodeDto.getSaksnummer(), revurderPeriodeDto.getSkjæringstidspunkt(), BehandlingÅrsakType.RE_KLAGE_NY_INNH_LIGNET_INNTEKT, Optional.empty());
    }

    @POST
    @Path("/revurder-bruk-forrige-skatteoppgjør")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter manuell revurdering og bruker skatteoppgjør fra oppgitt behandling.", summary = ("Oppretter manuell revurdering og bruker skatteoppgjør fra oppgitt behandling."), tags = "beregning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public void revurderOgBrukForrigeSkatteoppgjør(@Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BrukForrigeSkatteoppgjørDto brukForrigeSkatteoppgjørDto) {
        revurderBeregningTjeneste.revurderOgBrukForrigeSkatteoppgjør(
            brukForrigeSkatteoppgjørDto.getSaksnummer(),
            brukForrigeSkatteoppgjørDto.getBehandlingIdForrigeSkatteoppgjør(),
            brukForrigeSkatteoppgjørDto.getSkjæringstidspunkt());
    }


    @POST
    @Path("/fjern-prosesstrigger-reberegning")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner prosesstrigger for reberegning av grunnlag", summary = ("Fjerner prosesstrigger for reberegning av grunnlag"), tags = "beregning")
    @BeskyttetRessurs(action = CREATE, resource = DRIFT)
    public void fjernProsessTriggerForReberegning(@Parameter(description = "Saksnummer og skjæringstidspunkt (YYYY-MM-DD) på csv-format") @Valid OpprettManuellRevurderingBeregning opprettManuellRevurdering) {

        var alleSaksnummerOgSkjæringstidspunkt = Objects.requireNonNull(opprettManuellRevurdering.getSaksnummerOgSkjæringstidspunkt(), "saksnummerOgSkjæringstidspunkt");
        var saknummerOgSkjæringstidspunkt = new LinkedHashSet<>(Arrays.asList(alleSaksnummerOgSkjæringstidspunkt.split("\\s+")));

        for (var s : saknummerOgSkjæringstidspunkt) {
            var sakOgStpSplitt = s.split(",");
            var saksnummer = new Saksnummer(sakOgStpSplitt[0]);
            var stp = LocalDate.parse(sakOgStpSplitt[1]);
            var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false).orElseThrow(() -> new IllegalArgumentException("finnes ikke fagsak med saksnummer: " + saksnummer.getVerdi()));
            loggForvaltningTjeneste(fagsak, "/fjern-prosesstrigger-reberegning", "fjerner prosesstrigger RE-ENDR-BER-GRUN for skjæringstidspunkt " + stp);

            var behandling = behandlingRepository.hentSisteBehandlingForFagsakId(fagsak.getId());

            if (behandling.isEmpty()) {
                throw new IllegalArgumentException("Fant ingen behandling");
            }

            if (behandling.get().erSaksbehandlingAvsluttet()) {
                throw new IllegalArgumentException("Behandling med id " + behandling.get().getId() + " hadde avsluttet saksbehandling.");
            }

            prosessTriggerForvaltningTjeneste.fjern(behandling.get().getId(), stp, BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG);

        }

    }

    @POST
    @Path("/overstyr-opphør-refusjon")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Setter opphørsdato for refusjon for alle eksisterende og fremtidige inntektsmeldinger for virksomhet", summary = ("Overstyrer opphør av refusjon"), tags = "beregning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public void overstyrInntektsmelding(
        @Parameter(description = "Behandling-id", required = true)
        @FormParam("behandlingId")
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        BehandlingIdDto behandlingIdDto,
        @NotNull @FormParam("opphørsdatoRefusjon")
        @Parameter(description = "Opphørsdato for refusjon", required = true, example = "2020-01-01")
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class)
        OverstyrOpphørsdatoRefusjon opphørsdatoRefusjon,
        @NotNull @FormParam("orgNummer")
        @Parameter(description = "Organisasjonsnummer for virksomhet", required = true, example = "123456789")
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class)
        OrgNummer orgNummer,
        @NotNull
        @FormParam("begrunnelse")
        @Parameter(description = "Begrunnelse for opphør av refusjon. Vil dukke opp i historikkinnslag og må derfor være selvforklarende.", allowEmptyValue = false, required = true, schema = @Schema(type = "string", maximum = "2000"))
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class)
        KortTekst begrunnelse) {
        var behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingId());
        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(BehandlingReferanse.fra(behandling));
        var overlappendePeriode = perioderTilVurdering.stream().filter(p -> p.overlapper(opphørsdatoRefusjon.getOpphørRefusjon().minusDays(1), opphørsdatoRefusjon.getOpphørRefusjon().minusDays(1)))
            .findFirst();
        if (overlappendePeriode.isEmpty()) {
            throw new IllegalArgumentException("Fant ingen overlappende periode med opphørdato");
        }
        loggForvaltningTjeneste(behandling.getFagsak(), "/overstyr-opphør-refusjon", begrunnelse.getTekst());
        forvaltningOverstyrInputBeregning.overstyrOpphørRefusjon(behandling,
            overlappendePeriode.get(),
            Arbeidsgiver.virksomhet(orgNummer.getId()),
            opphørsdatoRefusjon.getOpphørRefusjon(),
            begrunnelse.getTekst());
    }

    private void opprettProsesstriggerOgRevurder(OpprettManuellRevurderingBeregning opprettManuellRevurdering, String tjeneste, String begrunnelse, BehandlingÅrsakType reOpplysningerOmOpptjening) {
        var alleSaksnummerOgSkjæringstidspunkt = Objects.requireNonNull(opprettManuellRevurdering.getSaksnummerOgSkjæringstidspunkt(), "saksnummerOgSkjæringstidspunkt");
        var saknummerOgSkjæringstidspunkt = new LinkedHashSet<>(Arrays.asList(alleSaksnummerOgSkjæringstidspunkt.split("\\s+")));

        int idx = 0;
        for (var s : saknummerOgSkjæringstidspunkt) {
            var sakOgStpSplitt = s.split(",");
            var saksnummer = new Saksnummer(sakOgStpSplitt[0]);
            var stp = LocalDate.parse(sakOgStpSplitt[1]);
            var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false).orElseThrow(() -> new IllegalArgumentException("finnes ikke fagsak med saksnummer: " + saksnummer.getVerdi()));
            loggForvaltningTjeneste(fagsak, tjeneste, begrunnelse);
            var nesteKjøring = LocalDateTime.now().plus(500L * idx, ChronoUnit.MILLIS);

            revurderBeregningTjeneste.revurderMedÅrsak(saksnummer, stp, reOpplysningerOmOpptjening, Optional.of(nesteKjøring));

            // lagrer direkte til ProsessTaskTjeneste så vi ikke går via FagsakProsessTask (siden den bestemmer rekkefølge). Får unik callId per task
            idx++;
        }
    }

    public static class OpprettManuellRevurderingBeregning implements AbacDto {

        @NotNull
        @Pattern(regexp = InputValideringRegex.FRITEKST, message = "OpprettManuellRevurderingBeregning [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
        private String saksnummerOgSkjæringstidspunkt;

        public OpprettManuellRevurderingBeregning() {
            // empty ctor
        }

        public OpprettManuellRevurderingBeregning(@NotNull String saksnummerOgSkjæringstidspunkt) {
            this.saksnummerOgSkjæringstidspunkt = saksnummerOgSkjæringstidspunkt;
        }

        public String getSaksnummerOgSkjæringstidspunkt() {
            return saksnummerOgSkjæringstidspunkt;
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett();
        }

        @Provider
        public static class OpprettManuellRevurderingBeregningMessageBodyReader implements MessageBodyReader<OpprettManuellRevurderingBeregning> {

            @Override
            public boolean isReadable(Class<?> type, Type genericType,
                                      Annotation[] annotations, MediaType mediaType) {
                return (type == OpprettManuellRevurderingBeregning.class);
            }

            @Override
            public OpprettManuellRevurderingBeregning readFrom(Class<OpprettManuellRevurderingBeregning> type, Type genericType,
                                                               Annotation[] annotations, MediaType mediaType,
                                                               MultivaluedMap<String, String> httpHeaders,
                                                               InputStream inputStream)
                throws IOException, WebApplicationException {
                var sb = new StringBuilder(200);
                try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(inputStream))) {
                    br.lines().forEach(l -> sb.append(l).append('\n'));
                }

                return new OpprettManuellRevurderingBeregning(sb.toString());

            }
        }
    }

    private void loggForvaltningTjeneste(Fagsak fagsak, String tjeneste, String begrunnelse) {
        /*
         * logger at tjenesten er kalt (er en forvaltnings tjeneste)
         */
        entityManager.persist(new DiagnostikkFagsakLogg(fagsak.getId(), tjeneste, begrunnelse));
        entityManager.flush();
    }


}
