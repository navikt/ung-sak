package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
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
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
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
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusRestKlient;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagKobling;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.migrerAksjonspunkt.MigrerAksjonspunktListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.migrerAksjonspunkt.MigrerAksjonspunktRequest;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.AbacDto;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.felles.util.InputValideringRegex;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.trigger.ProsessTriggerForvaltningTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.k9.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@ApplicationScoped
@Transactional
@Path("/beregning")
@Produces(MediaType.APPLICATION_JSON)
public class ForvaltningBeregningRestTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ForvaltningBeregningRestTjeneste.class);
    private static final MediaType JSON = MediaType.APPLICATION_JSON_TYPE;

    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private BehandlingRepository behandlingRepository;

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private AksjonspunktRepository aksjonspunktRepository;
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private KalkulusRestKlient kalkulusSystemRestKlient;
    private RevurderBeregningTjeneste revurderBeregningTjeneste;

    private EntityManager entityManager;

    private FagsakTjeneste fagsakTjeneste;

    private HentKalkulatorInputDump hentKalkulatorInputDump;

    private ProsessTriggerForvaltningTjeneste prosessTriggerForvaltningTjeneste;


    public ForvaltningBeregningRestTjeneste() {
    }

    @Inject
    public ForvaltningBeregningRestTjeneste(BehandlingRepository behandlingRepository,
                                            InntektArbeidYtelseTjeneste iayTjeneste,
                                            BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                            AksjonspunktRepository aksjonspunktRepository,
                                            BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                            SystemUserOidcRestClient systemUserOidcRestClient,
                                            @KonfigVerdi(value = "ftkalkulus.url") URI endpoint,
                                            RevurderBeregningTjeneste revurderBeregningTjeneste,
                                            EntityManager entityManager, FagsakTjeneste fagsakTjeneste,
                                            HentKalkulatorInputDump hentKalkulatorInputDump) {
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.aksjonspunktRepository = aksjonspunktRepository;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.revurderBeregningTjeneste = revurderBeregningTjeneste;
        this.entityManager = entityManager;
        this.fagsakTjeneste = fagsakTjeneste;
        this.hentKalkulatorInputDump = hentKalkulatorInputDump;
        this.prosessTriggerForvaltningTjeneste = new ProsessTriggerForvaltningTjeneste(entityManager);
        this.kalkulusSystemRestKlient = new KalkulusRestKlient(systemUserOidcRestClient, endpoint);
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

    @GET
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
    @Path("migrerAksjonspunkterKalkulus")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Migrerer aksjonspunkt til kalkulus", tags = "beregning")
    @BeskyttetRessurs(action = CREATE, resource = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response migrerAksjonspunkt(@Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) @Parameter(description = "migrerAksjonspunktDto") no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag.MigrerAksjonspunktRequest migrerAksjonspunktDto) { // NOSONAR
        Periode periode = migrerAksjonspunktDto.getPeriode();
        Map<Behandling, Aksjonspunkt> behandlingerMedAksjonspunkt = aksjonspunktRepository.hentAksjonspunkterForKodeUtenVenteÅrsak(periode.getFom(), periode.getTom(), migrerAksjonspunktDto.getAksjonspunktKode());
        List<String> saksummer = behandlingerMedAksjonspunkt.keySet().stream()
            .map(Behandling::getFagsak).map(Fagsak::getSaksnummer)
            .map(Saksnummer::getVerdi)
            .toList();
        logger.info("Fant følgende saksnummer med aksjonspunkt " + migrerAksjonspunktDto.getAksjonspunktKode()
            + ": " + saksummer);
        List<MigrerAksjonspunktRequest> aksjonspunktData = behandlingerMedAksjonspunkt.entrySet().stream().map(e -> lagAksjonspunktData(e.getKey(), e.getValue())).collect(Collectors.toList());
        MigrerAksjonspunktListeRequest migrerAksjonspunktListeRequest = new MigrerAksjonspunktListeRequest(aksjonspunktData, migrerAksjonspunktDto.getAksjonspunktKode());
        kalkulusSystemRestKlient.migrerAksjonspunkter(migrerAksjonspunktListeRequest);
        return Response.ok().build();
    }

    private MigrerAksjonspunktRequest lagAksjonspunktData(Behandling behandling, Aksjonspunkt aksjonspunkt) {
        var ref = BehandlingReferanse.fra(behandling);
        var stpTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true).stream()
            .map(DatoIntervallEntitet::getFomDato)
            .toList();
        var bgReferanser = beregningsgrunnlagTjeneste.hentKoblingerForPerioder(ref)
            .stream()
            .filter(kobling -> stpTilVurdering.contains(kobling.getSkjæringstidspunkt()))
            .map(BeregningsgrunnlagKobling::getReferanse)
            .collect(Collectors.toSet());
        return new MigrerAksjonspunktRequest(
            ref.getSaksnummer().getVerdi(),
            YtelseTyperKalkulusStøtterKontrakt.fraKode(ref.getFagsakYtelseType().getKode()),
            bgReferanser,
            aksjonspunkt.getStatus().getKode(),
            aksjonspunkt.getBegrunnelse()
        );
    }


    @POST
    @Path("/manuell-revurdering-beregning")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter manuell revurdering grunnet nye opplysninger om beregning.", summary = ("Oppretter manuell revurdering grunnet nye opplysninger om beregning."), tags = "beregning")
    @BeskyttetRessurs(action = CREATE, resource = DRIFT)
    public void revurderGrunnetEndretOpplysning(@Parameter(description = "Saksnummer og skjæringstidspunkt (YYYY-MM-DD) på csv-format") @Valid OpprettManuellRevurderingBeregning opprettManuellRevurdering) {

        var alleSaksnummerOgSkjæringstidspunkt = Objects.requireNonNull(opprettManuellRevurdering.getSaksnummerOgSkjæringstidspunkt(), "saksnummerOgSkjæringstidspunkt");
        var saknummerOgSkjæringstidspunkt = new LinkedHashSet<>(Arrays.asList(alleSaksnummerOgSkjæringstidspunkt.split("\\s+")));

        int idx = 0;
        for (var s : saknummerOgSkjæringstidspunkt) {
            var sakOgStpSplitt = s.split(",");
            var saksnummer = new Saksnummer(sakOgStpSplitt[0]);
            var stp = LocalDate.parse(sakOgStpSplitt[1]);
            var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false).orElseThrow(() -> new IllegalArgumentException("finnes ikke fagsak med saksnummer: " + saksnummer.getVerdi()));
            loggForvaltningTjeneste(fagsak, "/manuell-revurdering-beregning", "kjører manuell revurdering/tilbakehopp grunnet nye opplysninger om beregningsgrunnlag");
            var nesteKjøring = LocalDateTime.now().plus(500L * idx, ChronoUnit.MILLIS);

            revurderBeregningTjeneste.revurderBeregning(saksnummer, stp, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG, Optional.of(nesteKjøring));

            // lagrer direkte til ProsessTaskTjeneste så vi ikke går via FagsakProsessTask (siden den bestemmer rekkefølge). Får unik callId per task
            idx++;
        }
    }

    @POST
    @Path("/revurder-innhent-pgi")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter manuell revurdering for reinnhenting av PGI.", summary = ("Oppretter manuell revurdering for reinnhenting av PGI."), tags = "beregning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public void revurderOgInnhentPGI(@Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) RevurderPeriodeDto revurderPeriodeDto) {
        revurderBeregningTjeneste.revurderBeregning(revurderPeriodeDto.getSaksnummer(), revurderPeriodeDto.getSkjæringstidspunkt(), BehandlingÅrsakType.RE_KLAGE_NY_INNH_LIGNET_INNTEKT, Optional.empty());
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

    @GET
    @Path("/finn-saker-med-feil-trigger")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(description = "Henter saksnumre med feil.", summary = ("Henter saksnumre med feil."), tags = "beregning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response finnSakerMedFeilTrigger() {
        var query = entityManager.createNativeQuery(
            "SELECT distinct f.saksnummer saksnummer, LOWER(t.periode) fom " +
                "FROM PROSESS_TRIGGERE s " +
                "INNER JOIN PT_TRIGGER t on s.triggere_id = t.triggere_id " +
                "INNER JOIN BEHANDLING b on b.id = s.behandling_id " +
                "INNER JOIN FAGSAK f on b.fagsak_id = f.id " +
                "WHERE s.aktiv = true AND t.arsak = :aktuellArsak and " +
                "UPPER(t.periode) - LOWER(t.periode) = 1 and " +
                "t.opprettet_tid > :feilFra and " +
                "b.behandling_status = :utredes and " +
                "not exists(" +
                "select 1 from GR_BEREGNINGSGRUNNLAG gr " +
                "inner join BG_PERIODE p on p.bg_grunnlag_id = gr.bg_grunnlag_id " +
                "where gr.behandling_id = b.id and p.skjaeringstidspunkt = LOWER(t.periode))", Tuple.class);

        query.setParameter("aktuellArsak", BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG.getKode())
            .setParameter("feilFra", LocalDateTime.of(2023, 12, 12, 6, 55))
            .setParameter("utredes", BehandlingStatus.UTREDES.getKode());

        Stream<Tuple> results = query.getResultStream();

        Optional<String> dataDump = CsvOutput.dumpResultSetToCsv(results);

        return dataDump
            .map(s -> s.replace("\"", "")) //hack for å kunne bruke fjernProsessTriggerForReberegning direkte fra respons
            .map(d -> Response.ok(d)
                .type(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", String.format("attachment; filename=\"dump.csv\""))
                .build()).orElse(Response.noContent().build());
    }

    @GET
    @Path("/finn-saker-med-feil-trigger-overlapp-stp")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(description = "Henter saksnumre med feil.", summary = ("Henter saksnumre med feil."), tags = "beregning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response finnSakerMedFeilTriggerMedOverlappStp() {
        var query = entityManager.createNativeQuery(
            "SELECT distinct f.saksnummer saksnummer, LOWER(t.periode) fom " +
                "FROM PROSESS_TRIGGERE s " +
                "INNER JOIN PT_TRIGGER t on s.triggere_id = t.triggere_id " +
                "INNER JOIN BEHANDLING b on b.id = s.behandling_id " +
                "INNER JOIN FAGSAK f on b.fagsak_id = f.id " +
                "inner join GR_BEREGNINGSGRUNNLAG gr on gr.behandling_id = b.id " +
                "inner join BG_PERIODE p on p.bg_grunnlag_id = gr.bg_grunnlag_id " +
                "where gr.behandling_id = b.id " +
                "and p.skjaeringstidspunkt = LOWER(t.periode) " +
                "AND s.aktiv = true " +
                "AND t.arsak = :aktuellArsak " +
                "and UPPER(t.periode) - LOWER(t.periode) = 1 " +
                "and t.opprettet_tid > :feilFra " +
                "and b.behandling_status = :utredes ", Tuple.class);

        query.setParameter("aktuellArsak", BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG.getKode())
            .setParameter("feilFra", LocalDateTime.of(2023, 12, 12, 6, 55))
            .setParameter("utredes", BehandlingStatus.UTREDES.getKode());

        Stream<Tuple> results = query.getResultStream();

        Optional<String> dataDump = CsvOutput.dumpResultSetToCsv(results);

        return dataDump
            .map(s -> s.replace("\"", "")) //hack for å kunne bruke fjernProsessTriggerForReberegning direkte fra respons
            .map(d -> Response.ok(d)
                .type(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", String.format("attachment; filename=\"dump.csv\""))
                .build()).orElse(Response.noContent().build());
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
