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
import no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.InntektsmeldingRestKlient;
import no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.OpprettForespørselRequest;
import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.FinnPerioderMedEndringVedFeilInntektsmelding;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.AbacDto;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.felles.util.InputValideringRegex;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.forvaltning.DumpFeilImRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.trigger.ProsessTriggerForvaltningTjeneste;
import no.nav.k9.sak.typer.AktørId;
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

    private static final MediaType JSON = MediaType.APPLICATION_JSON_TYPE;

    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private RevurderBeregningTjeneste revurderBeregningTjeneste;
    private EntityManager entityManager;
    private FagsakTjeneste fagsakTjeneste;
    private HentKalkulatorInputDump hentKalkulatorInputDump;
    private ProsessTriggerForvaltningTjeneste prosessTriggerForvaltningTjeneste;
    private FinnPerioderMedEndringVedFeilInntektsmelding finnPerioderMedEndringVedFeilInntektsmelding;
    private InntektsmeldingRestKlient inntektsmeldingRestKlient;
    private DumpFeilImRepository dumpFeilImRepository;


    public ForvaltningBeregningRestTjeneste() {
    }

    @Inject
    public ForvaltningBeregningRestTjeneste(BehandlingRepository behandlingRepository,
                                            InntektArbeidYtelseTjeneste iayTjeneste,
                                            BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                            RevurderBeregningTjeneste revurderBeregningTjeneste,
                                            EntityManager entityManager, FagsakTjeneste fagsakTjeneste,
                                            HentKalkulatorInputDump hentKalkulatorInputDump, FinnPerioderMedEndringVedFeilInntektsmelding finnPerioderMedEndringVedFeilInntektsmelding, InntektsmeldingRestKlient inntektsmeldingRestKlient, DumpFeilImRepository dumpFeilImRepository) {
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.revurderBeregningTjeneste = revurderBeregningTjeneste;
        this.entityManager = entityManager;
        this.fagsakTjeneste = fagsakTjeneste;
        this.hentKalkulatorInputDump = hentKalkulatorInputDump;
        this.prosessTriggerForvaltningTjeneste = new ProsessTriggerForvaltningTjeneste(entityManager);
        this.finnPerioderMedEndringVedFeilInntektsmelding = finnPerioderMedEndringVedFeilInntektsmelding;
        this.dumpFeilImRepository = dumpFeilImRepository;
        this.inntektsmeldingRestKlient = inntektsmeldingRestKlient;
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
    @Path("finn-feil-inntektsmelding-bruk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(description = "Finner behandlinger og informasjon om perioder som er rammet av IM-feil", tags = "beregning")
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response finnBehandlingerMedFeilIM() { // NOSONAR
        return dumpFeilIMResultat().map(d -> Response.ok(d)
            .type(MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition", String.format("attachment; filename=\"feil_im.csv\""))
            .build()).orElse(Response.noContent().build());
    }


    private Optional<String> dumpFeilIMResultat() {
        String sql = """
            select f.saksnummer
                 ,b.fagsak_id
                 ,d.behandling_id
                 ,vp.fom
                 ,vp.tom
                 ,fp.fom
                 ,fp.tom
              from dump_feil_im d
              left join dump_feil_im_vilkar_periode vp on vp.dump_grunnlag_id = d.id
              left join dump_feil_im_fordel_periode fp on fp.dump_grunnlag_id = d.id
              inner join behandling b on b.id=d.behandling_id
              inner join fagsak f on f.id=b.fagsak_id
             """;

        Query query = entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);

        @SuppressWarnings("unchecked")
        List<jakarta.persistence.Tuple> results = query.getResultList();

        return CsvOutput.dumpResultSetToCsv(results);


    }


    @POST
    @Path("opprett-dummy-foresporsel")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Opprett forespørsel for IM", tags = "beregning")
    @BeskyttetRessurs(action = CREATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response opprettForespørselForIM() { // NOSONAR
        var testResquest = new OpprettForespørselRequest(
            AktørId.dummy().getAktørId(),
            "123456789",
            LocalDate.now(),
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN.getKode(),
            "SAK"
        );
        inntektsmeldingRestKlient.opprettForespørsel(testResquest);
        return Response.ok().build();
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
