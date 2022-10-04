package no.nav.k9.sak.web.app.tjenester.forvaltning;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.OVERSTYRING_FRISINN_OPPGITT_OPPTJENING;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.AbacDto;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.hendelse.stønadstatistikk.StønadstatistikkService;
import no.nav.k9.sak.kontrakt.FeilDto;
import no.nav.k9.sak.kontrakt.KortTekst;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto;
import no.nav.k9.sak.kontrakt.stønadstatistikk.StønadstatistikkSerializer;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tasks.OpprettManuellRevurderingTask;
import no.nav.k9.sak.web.app.tjenester.behandling.SjekkProsessering;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.k9.sak.web.app.tjenester.forvaltning.rapportering.DriftLesetilgangVurderer;
import no.nav.k9.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.frisinn.mottak.FrisinnSøknadInnsending;
import no.nav.k9.sak.ytelse.frisinn.mottak.FrisinnSøknadMottaker;
import no.nav.k9.sak.ytelse.pleiepengerbarn.utils.Hjelpetidslinjer;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.felles.type.Språk;
import no.nav.k9.søknad.felles.type.SøknadId;
import no.nav.k9.søknad.frisinn.FrisinnSøknad;
import no.nav.k9.søknad.frisinn.Inntekter;
import no.nav.k9.søknad.frisinn.PeriodeInntekt;
import no.nav.k9.søknad.frisinn.SelvstendigNæringsdrivende;
import no.nav.k9.søknad.ytelse.pls.v1.PleipengerLivetsSluttfase;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn;

/**
 * DENNE TJENESTEN ER BARE FOR MIDLERTIDIG BEHOV, OG SKAL AVVIKLES SÅ RASKT SOM MULIG.
 */
@Path("")
@ApplicationScoped
@Transactional
public class ForvaltningMidlertidigDriftRestTjeneste {

    private FrisinnSøknadMottaker frisinnSøknadMottaker;
    private TpsTjeneste tpsTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private ProsessTaskTjeneste prosessTaskRepository;
    private FagsakTjeneste fagsakTjeneste;
    private EntityManager entityManager;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRepository behandlingRepository;

    private SjekkProsessering sjekkProsessering;

    private StønadstatistikkService stønadstatistikkService;

    private DriftLesetilgangVurderer lesetilgangVurderer;

    public ForvaltningMidlertidigDriftRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public ForvaltningMidlertidigDriftRestTjeneste(@FagsakYtelseTypeRef(FRISINN) FrisinnSøknadMottaker frisinnSøknadMottaker,
                                                   TpsTjeneste tpsTjeneste,
                                                   BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                                   FagsakTjeneste fagsakTjeneste,
                                                   ProsessTaskTjeneste prosessTaskRepository,
                                                   MottatteDokumentRepository mottatteDokumentRepository,
                                                   BehandlingRepository behandlingRepository,
                                                   SjekkProsessering sjekkProsessering,
                                                   EntityManager entityManager,
                                                   StønadstatistikkService stønadstatistikkService,
                                                   DriftLesetilgangVurderer lesetilgangVurderer) {

        this.frisinnSøknadMottaker = frisinnSøknadMottaker;
        this.tpsTjeneste = tpsTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.behandlingRepository = behandlingRepository;
        this.sjekkProsessering = sjekkProsessering;
        this.entityManager = entityManager;
        this.stønadstatistikkService = stønadstatistikkService;
        this.lesetilgangVurderer = lesetilgangVurderer;
    }

    /**
     * @deprecated Bør fjernes når FRISINN nedlegges.
     */
    @Deprecated(forRemoval = true)
    @POST
    @Path("/frisinn/opprett-manuell-frisinn/TO_BE_REMOVED")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Opprett behandling hvor saksbehandler kan legge inn inntektsopplysninger", summary = ("Returnerer saksnummer som er tilknyttet den nye fagsaken som har blitt opprettet."), tags = "frisinn", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer saksnummer", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SaksnummerDto.class)))
    })
    @Produces(MediaType.APPLICATION_JSON)
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public Response opprettManuellFrisinnSøknad(@Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) ManuellSøknadDto manuellSøknadDto) {
        PersonIdent fnr = new PersonIdent(manuellSøknadDto.getFnr());
        AktørId aktørId = tpsTjeneste.hentAktørForFnr(fnr).orElse(null);
        if (aktørId == null) {
            return Response.serverError().entity(new FeilDto("Oppgitt personummer er ukjent")).build();
        }

        // Samme utledning for fagsakperiode som i k9-fordel
        LocalDate fom = LocalDate.of(2020, 3, 1);
        LocalDate tom = manuellSøknadDto.getPeriode().getTilOgMed();

        Fagsak fagsak = frisinnSøknadMottaker.finnEllerOpprettFagsak(FRISINN, aktørId, null, null, fom, tom);

        loggForvaltningTjeneste(fagsak, "/frisinn/opprett-manuell-frisinn/", "kjører manuell frisinn søknad");

        FrisinnSøknad søknad = FrisinnSøknad.builder()
            .språk(Språk.NORSK_BOKMÅL)
            .søknadId(SøknadId.of("ManueltOpprettet" + fagsak.getSaksnummer().getVerdi())) // lagres ingensteds
            .inntekter(lagDummyInntekt(manuellSøknadDto))
            .søknadsperiode(manuellSøknadDto.getPeriode())
            .mottattDato(ZonedDateTime.now(ZoneId.of("Europe/Paris")))
            .søker(no.nav.k9.søknad.felles.personopplysninger.Søker.builder().norskIdentitetsnummer(NorskIdentitetsnummer.of(fnr.getIdent())).build())
            .build();
        var valideringsfeil = validerSøknad(fagsak, søknad);
        if (valideringsfeil.isPresent()) {
            return Response.serverError().entity(new FeilDto(valideringsfeil.get().getMessage())).build();
        }

        FrisinnSøknadInnsending frisinnSøknadInnsending = new FrisinnSøknadInnsending();
        frisinnSøknadInnsending.setSøknad(søknad);
        var behandling = frisinnSøknadMottaker.mottaSøknad(fagsak.getSaksnummer(), null, frisinnSøknadInnsending);

        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        // ønsker at saksbehandler må ta stilling til disse
        behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, List.of(OVERSTYRING_FRISINN_OPPGITT_OPPTJENING, KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING));

        return Response.ok(new SaksnummerDto(fagsak.getSaksnummer())).build();
    }

    @POST
    @Path("/stonadstatistikk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Henter ut stønadstatistikk-JSON.", summary = ("Henter ut stønadstatistikk-JSON."), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = FAGSAK)
    public Response hentUtStønadstatistikk(
        @Parameter(description = "Behandling-UUID")
        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingIdDto behandlingIdDto) {

        final var behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        final String json = StønadstatistikkSerializer.toJson(stønadstatistikkService.lagHendelse(behandling.getId()));
        return Response.ok(json).build();
    }

    @POST
    @Path("/manuell-revurdering")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(description = "Oppretter manuell revurdering med annet som årsak.", summary = ("Oppretter manuell revurdering med annet som årsak."), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public void opprettNyManuellRevurderingEllerTilbakestillingAvÅpenBehandling(@Parameter(description = "Saksnumre (skilt med mellomrom eller linjeskift)") @Valid OpprettManuellRevurdering opprettManuellRevurdering) {
        var alleSaksnummer = Objects.requireNonNull(opprettManuellRevurdering.getSaksnumre(), "saksnumre");
        var saknumre = new LinkedHashSet<>(Arrays.asList(alleSaksnummer.split("\\s+")));

        int idx = 0;
        for (var s : saknumre) {
            var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(new Saksnummer(s), false).orElseThrow(() -> new IllegalArgumentException("finnes ikke fagsak med saksnummer: " + s));
            loggForvaltningTjeneste(fagsak, "/manuell-revurdering", "kjører manuell revurdering/tilbakehopp");

            var taskData = ProsessTaskData.forProsessTask(OpprettManuellRevurderingTask.class);
            taskData.setSaksnummer(fagsak.getSaksnummer().getVerdi());
            taskData.setNesteKjøringEtter(LocalDateTime.now().plus(500L * idx, ChronoUnit.MILLIS)); // sprer utover hvert 1/2 sek.
            // lagrer direkte til ProsessTaskTjeneste så vi ikke går via FagsakProsessTask (siden den bestemmer rekkefølge). Får unik callId per task
            prosessTaskRepository.lagre(taskData);
            idx++;
        }

    }

    @GET
    @Path("/saker-med-feil")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Henter saksnumre med feil.", summary = ("Henter saksnumre med feil."), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response hentSakerMedFeil() {
        final Query q = entityManager.createNativeQuery("SELECT DISTINCT u.saksnummer FROM (("
            + "  SELECT f.saksnummer AS saksnummer"
            + "  FROM TMP_FIKS_AKTIV_ET_1 t1 INNER JOIN PSB_UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE p ON ("
            + "    t1.id = p.id"
            + "  ) INNER JOIN PSB_GR_UNNTAK_ETABLERT_TILSYN g ON ("
            + "    g.psb_unntak_etablert_tilsyn_pleietrengende_id = p.id"
            + "  ) INNER JOIN BEHANDLING b ON ("
            + "    b.id = g.behandling_id"
            + "  ) INNER JOIN FAGSAK f ON ("
            + "    F.id = b.fagsak_id"
            + "  ) "
            + ")  UNION ("
            + "  SELECT f.saksnummer AS saksnummer"
            + "  FROM TMP_FIKS_AKTIV_ET_2 t2 INNER JOIN PSB_GR_UNNTAK_ETABLERT_TILSYN g ON ("
            + "    g.id = t2.id"
            + "  ) INNER JOIN BEHANDLING b ON ("
            + "    b.id = g.behandling_id"
            + "  ) INNER JOIN FAGSAK f ON ("
            + "    F.id = b.fagsak_id"
            + "  )"
            + ")) u");

        @SuppressWarnings("unchecked") final List<String> result = q.getResultList();
        final String saksnummerliste = result.stream().reduce((a, b) -> a + ", " + b).orElse("");

        return Response.ok(saksnummerliste).build();
    }

    @GET
    @Path("/saker-med-feil2")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Henter saksnumre med feil.", summary = ("Henter saksnumre med feil."), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response hentSakerMedFeil2() {
        final Query q = entityManager.createNativeQuery(
            "SELECT convert_from(lo_get(d.payload), 'UTF-8') as payload, f.saksnummer "
                + "FROM MOTTATT_DOKUMENT d INNER JOIN Behandling b ON ( "
                + "  b.id = d.behandling_id "
                + ") INNER JOIN Fagsak f ON ( "
                + "  f.id = b.fagsak_id "
                + ") "
                + "WHERE d.type = 'PLEIEPENGER_SOKNAD' "
                + "  AND d.mottatt_dato >= to_date('2022-05-01', 'YYYY-MM-DD')"
                + "  AND d.mottatt_dato <= to_date('2022-06-03', 'YYYY-MM-DD')");
        q.setHint("javax.persistence.query.timeout", 5 * 60 * 1000); // 5 minutter

        final List<String> saksnumre = new ArrayList<>();

        @SuppressWarnings("unchecked") final Stream<Object[]> resultStream = q.getResultStream();
        resultStream.forEach(d -> {
            try {
                final Object[] result = (Object[]) d;
                final String soknadJson = (String) result[0];
                final String saksnummer = (String) result[1];
                final Søknad soknad = JsonUtils.fromString(soknadJson, Søknad.class);
                if (erFraBrukerdialogPsb(soknad) && harReellPeriodeMedNullNormal(soknad.getYtelse())) {
                    saksnumre.add(saksnummer);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        final String saksnummerliste = saksnumre.stream().reduce((a, b) -> a + ", " + b).orElse("");

        return Response.ok(saksnummerliste).build();
    }

    @GET
    @Path("/saker-med-feil3")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Henter saksnumre med feil på PPN.", summary = ("Henter saksnumre med feil på PPN."), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response hentSakerMedFeil3() {
        final Query q = entityManager.createNativeQuery(
            "SELECT convert_from(lo_get(d.payload), 'UTF-8') as payload, f.saksnummer "
                + "FROM MOTTATT_DOKUMENT d INNER JOIN Behandling b ON ( "
                + "  b.id = d.behandling_id "
                + ") INNER JOIN Fagsak f ON ( "
                + "  f.id = b.fagsak_id "
                + ") "
                + "WHERE d.type = 'PLEIEPENGER_LIVETS_SLUTTFASE_SOKNAD' ");
        q.setHint("javax.persistence.query.timeout", 5 * 60 * 1000); // 5 minutter

        final List<String> saksnumre = new ArrayList<>();

        @SuppressWarnings("unchecked") final Stream<Object[]> resultStream = q.getResultStream();
        resultStream.forEach(d -> {
            try {
                final Object[] result = (Object[]) d;
                final String soknadJson = (String) result[0];
                final String saksnummer = (String) result[1];
                final Søknad soknad = JsonUtils.fromString(soknadJson, Søknad.class);
                if (harTomSøknadsperiode(soknad.getYtelse())) {
                    saksnumre.add(saksnummer);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        final String saksnummerliste = saksnumre.stream().reduce((a, b) -> a + ", " + b).orElse("");

        return Response.ok(saksnummerliste).build();
    }
    
    @GET
    @Path("/saker-med-feil4")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Henter saksnumre med feil.", summary = ("Henter saksnumre med feil."), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response hentSakerMedFeil4() {
        final Query q = entityManager.createNativeQuery(
            "SELECT convert_from(lo_get(d.payload), 'UTF-8') as payload, f.saksnummer "
                + "FROM MOTTATT_DOKUMENT d INNER JOIN Behandling b ON ( "
                + "  b.id = d.behandling_id "
                + ") INNER JOIN Fagsak f ON ( "
                + "  f.id = b.fagsak_id "
                + ") "
                + "WHERE d.type = 'PLEIEPENGER_SOKNAD' "
                + "  AND d.mottatt_dato >= to_date('2022-09-22', 'YYYY-MM-DD')"
                + "  AND d.mottatt_dato <= to_date('2022-09-30', 'YYYY-MM-DD')");
        q.setHint("javax.persistence.query.timeout", 5 * 60 * 1000); // 5 minutter

        final List<String> saksnumre = new ArrayList<>();

        @SuppressWarnings("unchecked") final Stream<Object[]> resultStream = q.getResultStream();
        resultStream.forEach(d -> {
            try {
                final Object[] result = (Object[]) d;
                final String soknadJson = (String) result[0];
                final String saksnummer = (String) result[1];
                final Søknad soknad = JsonUtils.fromString(soknadJson, Søknad.class);
                if (erFraBrukerdialogPsb(soknad) && harOmsorgstilbud(soknad.getYtelse())) {
                    saksnumre.add(saksnummer);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        final String saksnummerliste = saksnumre.stream().reduce((a, b) -> a + ", " + b).orElse("");

        return Response.ok(saksnummerliste).build();
    }

    private boolean harTomSøknadsperiode(PleipengerLivetsSluttfase pls) {
        return pls.getSøknadsperiodeList().isEmpty();
    }

    private boolean erFraBrukerdialogPsb(Søknad søknad) {
        return søknad.getJournalposter() == null || søknad.getJournalposter().isEmpty();
    }
    
    private boolean harOmsorgstilbud(PleiepengerSyktBarn ytelse) {
        if (ytelse.getTilsynsordning() == null) {
            return false;
        }
        if (ytelse.getTilsynsordning().getPerioder() == null) {
            return false;
        }
        return ytelse.getTilsynsordning().getPerioder()
                .entrySet()
                .stream()
                .anyMatch(p -> !p.getValue().getEtablertTilsynTimerPerDag().isZero());
    }

    private boolean harReellPeriodeMedNullNormal(PleiepengerSyktBarn soknad) {
        return soknad.getArbeidstid().getArbeidstakerList().stream().anyMatch(a -> {
            return a.getArbeidstidInfo().getPerioder().entrySet().stream().anyMatch(tid ->
                tid.getValue().getJobberNormaltTimerPerDag() != null
                    && tid.getValue().getJobberNormaltTimerPerDag().equals(Duration.ofSeconds(0L))
                    && tid.getValue().getFaktiskArbeidTimerPerDag() != null
                    && tid.getValue().getFaktiskArbeidTimerPerDag().equals(Duration.ofSeconds(0L))
                    && erIkkeHelg(tid.getKey())
            );
        });
    }

    private boolean erIkkeHelg(Periode periode) {
        final LocalDateTimeline<Boolean> helePerioden = new LocalDateTimeline<>(periode.getFraOgMed(), periode.getTilOgMed(), Boolean.TRUE);
        final LocalDateTimeline<Boolean> kunHelger = Hjelpetidslinjer.lagTidslinjeMedKunHelger(helePerioden);
        return !helePerioden.disjoint(kunHelger).isEmpty();
    }

    @GET
    @Path("/starttidspunkt-aapen-behandling")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Henter starttidspunkt for åpen behandling. Dvs tidspunktet den første søknaden kom inn på behandlingen i k9-sak.", summary = ("Henter starttidspunkt for åpen behandling. Dvs tidspunktet den første søknaden kom inn på behandlingen i k9-sak."), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response hentStarttidspunktÅpenBehandling() {
        final Query q = entityManager.createNativeQuery("select\n"
            + "  f.saksnummer,\n"
            + "MIN(m.opprettet_tid) as tidspunkt,\n"
            + "  (select string_agg(a.aksjonspunkt_def||'('||a.vent_aarsak||')', ', ') from aksjonspunkt a where a.behandling_id = b.id and a.aksjonspunkt_status = 'OPPR') AS aksjonspunkt\n"
            + "from behandling b inner join mottatt_dokument m ON (\n"
            + "  m.behandling_id = b.id\n"
            + ") inner join fagsak f ON (\n"
            + "  f.id = m.fagsak_id\n"
            + ")\n"
            + "where b.behandling_status = 'UTRED'\n"
            + "  and m.type = 'PLEIEPENGER_SOKNAD'\n"
            + "  and m.status = 'GYLDIG'\n"
            + "  and f.ytelse_type = 'PSB'\n"
            + "group by saksnummer, b.id\n"
            + "order by tidspunkt ASC\n"
            + "limit 500");

        @SuppressWarnings("unchecked") final List<Object[]> result = q.getResultList();
        final String restApiPath = "/starttidspunkt-aapen-behandling";
        final String resultatString = result.stream()
            .filter(a -> lesetilgangVurderer.harTilgang(a[0].toString()))
            .map(a -> a[0].toString() + ";" + a[1].toString() + ";" + (a[2] != null ? a[2].toString() : ""))
            .reduce((a, b) -> a + "\n" + b)
            .orElse("");

        return Response.ok(resultatString).build();
    }

    @GET
    @Path("/aapne-psb-med-soknad")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Henter åpne saker i PSB.", summary = ("Henter åpne saker i PSB."), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response antallÅpnePsbMedSøknad() {
        final Query q = entityManager.createNativeQuery("select\n"
            + "    f.saksnummer, b.original_behandling_id IS NOT NULL AS revurdering, m.id IS NOT NULL AS med_soknad, COUNT(*) AS antall_soknader\n"
            + "  from behandling b inner join fagsak f ON (\n"
            + "    f.id = b.fagsak_id\n"
            + "    and f.ytelse_type = 'PSB'\n"
            + "  ) left outer join mottatt_dokument m ON (\n"
            + "    m.behandling_id = b.id\n"
            + "    and m.type = 'PLEIEPENGER_SOKNAD'\n"
            + "    and m.status = 'GYLDIG'\n"
            + "  ) \n"
            + "  where b.behandling_status = 'UTRED'\n"
            + "    AND b.original_behandling_id IS NOT NULL\n"
            + "    AND m.id IS NOT NULL\n"
            + "  group by saksnummer, b.original_behandling_id IS NOT NULL, m.id IS NOT NULL");

        @SuppressWarnings("unchecked") final List<Object[]> result = q.getResultList();
        final String restApiPath = "/aapne-psb-med-soknad";
        final String resultatString = result.stream()
            .filter(a -> lesetilgangVurderer.harTilgang(a[0].toString()))
            .map(a -> a[0].toString() + ";" + a[1].toString() + ";" + a[2].toString() + ";" + a[3].toString())
            .reduce((a, b) -> a + "\n" + b)
            .orElse("");
        return Response.ok(resultatString).build();
    }

    @GET
    @Path("/frisinn/uttrekk-antall")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Adhoc-uttrekk for Frisinn", summary = ("Adhoc-uttrekk for Frisinn."), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response antallFrisinnsøknader() {

        final Query q = entityManager.createNativeQuery("SELECT '2020' aar, (\n"
            + "  SELECT COUNT(*)\n"
            + "  FROM Behandling b INNER JOIN Fagsak f on (\n"
            + "    f.id = b.fagsak_id\n"
            + "  )\n"
            + "  WHERE f.ytelse_type = 'FRISINN'\n"
            + "    AND b.opprettet_tid < '2021-02-01'\n"
            + "    AND b.behandling_status IN ('AVSLU', 'FVED', 'IVED')\n"
            + "    AND b.ansvarlig_saksbehandler is not null\n"
            + "    AND NOT EXISTS (\n"
            + "      SELECT * FROM BEHANDLING_ARSAK aarsak WHERE aarsak.behandling_id = b.id AND manuelt_opprettet = true\n"
            + "    )\n"
            + ") AS manuell, (\n"
            + "  SELECT COUNT(*)\n"
            + "  FROM Behandling b INNER JOIN Fagsak f on (\n"
            + "    f.id = b.fagsak_id\n"
            + "  )\n"
            + "  WHERE f.ytelse_type = 'FRISINN'\n"
            + "    AND b.opprettet_tid < '2021-02-01'\n"
            + "    AND b.behandling_status IN ('AVSLU', 'FVED', 'IVED')\n"
            + "    AND b.ansvarlig_saksbehandler is null\n"
            + "    AND NOT EXISTS (\n"
            + "      SELECT * FROM BEHANDLING_ARSAK aarsak WHERE aarsak.behandling_id = b.id AND manuelt_opprettet = true\n"
            + "    )\n"
            + ") AS automatisk\n"
            + "\n"
            + "UNION\n"
            + "\n"
            + "SELECT '2021' aar, (\n"
            + "  SELECT COUNT(*)\n"
            + "  FROM Behandling b INNER JOIN Fagsak f on (\n"
            + "    f.id = b.fagsak_id\n"
            + "  )\n"
            + "  WHERE f.ytelse_type = 'FRISINN'\n"
            + "    AND b.opprettet_tid >= '2021-02-01'\n"
            + "    AND b.behandling_status IN ('AVSLU', 'FVED', 'IVED')\n"
            + "    AND b.ansvarlig_saksbehandler is not null\n"
            + "    AND NOT EXISTS (\n"
            + "      SELECT * FROM BEHANDLING_ARSAK aarsak WHERE aarsak.behandling_id = b.id AND manuelt_opprettet = true\n"
            + "    )\n"
            + ") AS manuell, (\n"
            + "  SELECT COUNT(*)\n"
            + "  FROM Behandling b INNER JOIN Fagsak f on (\n"
            + "    f.id = b.fagsak_id\n"
            + "  )\n"
            + "  WHERE f.ytelse_type = 'FRISINN'\n"
            + "    AND b.opprettet_tid >= '2021-02-01'\n"
            + "    AND b.behandling_status IN ('AVSLU', 'FVED', 'IVED')\n"
            + "    AND b.ansvarlig_saksbehandler is null\n"
            + "    AND NOT EXISTS (\n"
            + "      SELECT * FROM BEHANDLING_ARSAK aarsak WHERE aarsak.behandling_id = b.id AND manuelt_opprettet = true\n"
            + "    )\n"
            + ") AS automatisk\n"
            + "\n"
            + "UNION\n"
            + "\n"
            + "SELECT '2020-ubehandlet' aar, (\n"
            + "  SELECT COUNT(*)\n"
            + "  FROM Behandling b INNER JOIN Fagsak f on (\n"
            + "    f.id = b.fagsak_id\n"
            + "  )\n"
            + "  WHERE f.ytelse_type = 'FRISINN'\n"
            + "    AND b.opprettet_tid < '2021-02-01'\n"
            + "    AND b.behandling_status NOT IN ('AVSLU', 'FVED', 'IVED')\n"
            + "    AND b.ansvarlig_saksbehandler is not null\n"
            + "    AND NOT EXISTS (\n"
            + "      SELECT * FROM BEHANDLING_ARSAK aarsak WHERE aarsak.behandling_id = b.id AND manuelt_opprettet = true\n"
            + "    )\n"
            + ") AS manuell, (\n"
            + "  SELECT COUNT(*)\n"
            + "  FROM Behandling b INNER JOIN Fagsak f on (\n"
            + "    f.id = b.fagsak_id\n"
            + "  )\n"
            + "  WHERE f.ytelse_type = 'FRISINN'\n"
            + "    AND b.opprettet_tid < '2021-02-01'\n"
            + "    AND b.behandling_status NOT IN ('AVSLU', 'FVED', 'IVED')\n"
            + "    AND b.ansvarlig_saksbehandler is null\n"
            + "    AND NOT EXISTS (\n"
            + "      SELECT * FROM BEHANDLING_ARSAK aarsak WHERE aarsak.behandling_id = b.id AND manuelt_opprettet = true\n"
            + "    )\n"
            + ") AS automatisk\n"
            + "\n"
            + "UNION\n"
            + "\n"
            + "SELECT '2021-ubehandlet' aar, (\n"
            + "  SELECT COUNT(*)\n"
            + "  FROM Behandling b INNER JOIN Fagsak f on (\n"
            + "    f.id = b.fagsak_id\n"
            + "  )\n"
            + "  WHERE f.ytelse_type = 'FRISINN'\n"
            + "    AND b.opprettet_tid >= '2021-02-01'\n"
            + "    AND b.behandling_status NOT IN ('AVSLU', 'FVED', 'IVED')\n"
            + "    AND b.ansvarlig_saksbehandler is not null\n"
            + "    AND NOT EXISTS (\n"
            + "      SELECT * FROM BEHANDLING_ARSAK aarsak WHERE aarsak.behandling_id = b.id AND manuelt_opprettet = true\n"
            + "    )\n"
            + ") AS manuell, (\n"
            + "  SELECT COUNT(*)\n"
            + "  FROM Behandling b INNER JOIN Fagsak f on (\n"
            + "    f.id = b.fagsak_id\n"
            + "  )\n"
            + "  WHERE f.ytelse_type = 'FRISINN'\n"
            + "    AND b.opprettet_tid >= '2021-02-01'\n"
            + "    AND b.behandling_status NOT IN ('AVSLU', 'FVED', 'IVED')\n"
            + "    AND b.ansvarlig_saksbehandler is null\n"
            + "    AND NOT EXISTS (\n"
            + "      SELECT * FROM BEHANDLING_ARSAK aarsak WHERE aarsak.behandling_id = b.id AND manuelt_opprettet = true\n"
            + "    )\n"
            + ") AS automatisk");

        @SuppressWarnings("unchecked") final List<Object[]> result = q.getResultList();
        final String resultatString = result.stream()
            .map(a -> a[0].toString() + ";" + a[1].toString() + ";" + a[2].toString())
            .reduce((a, b) -> a + "\n" + b)
            .orElse("");
        return Response.ok(resultatString).build();
    }

    @POST
    @Path("/marker-ugyldig")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Markerer et mottatt dokument som ugyldig", summary = ("Markerer angitt dokument som ugyldig"), tags = "forvaltning")
    // TODO: (Endre fra CREATE til UPDATE når policy er på plass)
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = DRIFT)
    public Response markerMottattDokumentUgyldig(@NotNull @FormParam("saksnummer") @Parameter(description = "saksnummer", allowEmptyValue = false, required = true, schema = @Schema(type = "string", maximum = "10")) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto,
                                                 @NotNull @FormParam("journalpost") @Parameter(description = "journalpost", allowEmptyValue = false, required = true, schema = @Schema(type = "string", maximum = "20")) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) JournalpostIdDto journalpostDto,
                                                 @NotNull @FormParam("begrunnelse") @Parameter(description = "begrunnelse", allowEmptyValue = false, required = true, schema = @Schema(type = "string", maximum = "2000")) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) KortTekst begrunnelse) {

        var saksnummer = Objects.requireNonNull(saksnummerDto.getVerdi());
        var fagsakOpt = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true);

        if (fagsakOpt.isEmpty()) {
            return Response.status(Status.BAD_REQUEST.getStatusCode(), "Fant ikke fagsak for angitt saksnummer").build();
        }
        var fagsak = fagsakOpt.get();
        loggForvaltningTjeneste(fagsak, "/marker-ugyldig", begrunnelse.getTekst());

        var dokumenter = mottatteDokumentRepository.hentMottatteDokument(fagsak.getId(), List.of(journalpostDto.getJournalpostId()), DokumentStatus.GYLDIG, DokumentStatus.MOTTATT, DokumentStatus.BEHANDLER);
        if (dokumenter.size() > 1) {
            return Response.status(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Fant flere dokumenter for angitt saksnummer/journalpost - " + dokumenter.size()).build();
        }
        if (dokumenter.isEmpty()) {
            return Response.status(Status.NOT_FOUND.getStatusCode(), "Fant ingen dokumenter for angitt saksnummer/journalpost").build();
        }

        mottatteDokumentRepository.oppdaterStatus(dokumenter, DokumentStatus.UGYLDIG);

        return Response.ok().build();

    }

    @POST
    @Path("/innhent-registerdata")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Innhent registerdata på nytt", summary = ("Innhent registerdata på nytt"), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = DRIFT)
    public Response innhentRegisterdataPåNytt(@NotNull @FormParam("saksnummer") @Parameter(description = "saksnummer", allowEmptyValue = false, required = true, schema = @Schema(type = "string", maximum = "10")) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto,
                                              @NotNull @FormParam("begrunnelse") @Parameter(description = "begrunnelse", allowEmptyValue = false, required = true, schema = @Schema(type = "string", maximum = "2000")) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) KortTekst begrunnelse) {

        var saksnummer = Objects.requireNonNull(saksnummerDto.getVerdi());
        var fagsakOpt = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true);

        if (fagsakOpt.isEmpty()) {
            return Response.status(Status.BAD_REQUEST.getStatusCode(), "Fant ikke fagsak for angitt saksnummer").build();
        }
        var fagsak = fagsakOpt.get();
        loggForvaltningTjeneste(fagsak, "/innhent-registerdata", begrunnelse.getTekst());

        var åpneBehandlinger = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId(), BehandlingType.FØRSTEGANGSSØKNAD, BehandlingType.REVURDERING);

        if (åpneBehandlinger.isEmpty()) {
            return Response.status(Status.BAD_REQUEST).build();
        } else if (åpneBehandlinger.size() > 1) {
            return Response.status(Status.NOT_IMPLEMENTED).entity("Støtter kun oppfrisking av en åpen behandling").build();
        }

        var behandling = åpneBehandlinger.get(0);
        if (sjekkProsessering.opprettTaskForOppfrisking(behandling, true)) {
            return Response.status(Status.ACCEPTED).build();
        } else {
            return Response.status(Status.CONFLICT).entity("Kan ikke innhente registeropplysninger nå - kan være feilede tasks eller prosesstilstand").build();
        }

    }

    private void loggForvaltningTjeneste(Fagsak fagsak, String tjeneste, String begrunnelse) {
        /*
         * logger at tjenesten er kalt (er en forvaltnings tjeneste)
         */
        entityManager.persist(new DiagnostikkFagsakLogg(fagsak.getId(), tjeneste, begrunnelse));
        entityManager.flush();
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }

    @NotNull
    private Optional<Exception> validerSøknad(Fagsak fagsak, FrisinnSøknad søknad) {
        try {
            frisinnSøknadMottaker.validerSøknad(fagsak, søknad);
        } catch (Exception e) {
            return Optional.of(e);
        }
        return Optional.empty();
    }

    private Inntekter lagDummyInntekt(ManuellSøknadDto manuellSøknadDto) {
        Map<Periode, PeriodeInntekt> periodePeriodeInntektMap = new HashMap<>();
        periodePeriodeInntektMap.put(manuellSøknadDto.getPeriode(), new PeriodeInntekt(BigDecimal.ZERO));
        Map<Periode, PeriodeInntekt> periodeFør = new HashMap<>();
        periodeFør.put(new Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31)), new PeriodeInntekt(BigDecimal.ONE));

        SelvstendigNæringsdrivende selvstendigNæringsdrivende = SelvstendigNæringsdrivende.builder()
            .inntekterSøknadsperiode(periodePeriodeInntektMap)
            .inntekterFør(periodeFør)
            .build();

        return new Inntekter(null, selvstendigNæringsdrivende, null);
    }

    public static class OpprettManuellRevurdering implements AbacDto {

        @NotNull
        @Pattern(regexp = "^[\\p{Alnum}\\s]+$", message = "OpprettManuellRevurdering [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
        private String saksnumre;

        public OpprettManuellRevurdering() {
            // empty ctor
        }

        public OpprettManuellRevurdering(@NotNull String saksnumre) {
            this.saksnumre = saksnumre;
        }

        @NotNull
        public String getSaksnumre() {
            return saksnumre;
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett();
        }

        @Provider
        public static class OpprettManuellRevurderingMessageBodyReader implements MessageBodyReader<OpprettManuellRevurdering> {

            @Override
            public boolean isReadable(Class<?> type, Type genericType,
                                      Annotation[] annotations, MediaType mediaType) {
                return (type == OpprettManuellRevurdering.class);
            }

            @Override
            public OpprettManuellRevurdering readFrom(Class<OpprettManuellRevurdering> type, Type genericType,
                                                      Annotation[] annotations, MediaType mediaType,
                                                      MultivaluedMap<String, String> httpHeaders,
                                                      InputStream inputStream)
                throws IOException, WebApplicationException {
                var sb = new StringBuilder(200);
                try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(inputStream))) {
                    sb.append(br.readLine()).append('\n');
                }

                return new OpprettManuellRevurdering(sb.toString());

            }
        }
    }
}
