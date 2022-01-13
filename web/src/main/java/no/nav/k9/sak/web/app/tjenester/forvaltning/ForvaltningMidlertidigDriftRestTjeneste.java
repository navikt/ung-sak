package no.nav.k9.sak.web.app.tjenester.forvaltning;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.OVERSTYRING_FRISINN_OPPGITT_OPPTJENING;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.felles.sikkerhet.abac.AbacAttributtSamling;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.AbacDto;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.Pep;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.k9.felles.sikkerhet.abac.Tilgangsbeslutning;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
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
import no.nav.k9.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.frisinn.mottak.FrisinnSøknadInnsending;
import no.nav.k9.sak.ytelse.frisinn.mottak.FrisinnSøknadMottaker;
import no.nav.k9.sikkerhet.oidc.token.bruker.BrukerTokenProvider;
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.felles.type.Språk;
import no.nav.k9.søknad.felles.type.SøknadId;
import no.nav.k9.søknad.frisinn.FrisinnSøknad;
import no.nav.k9.søknad.frisinn.Inntekter;
import no.nav.k9.søknad.frisinn.PeriodeInntekt;
import no.nav.k9.søknad.frisinn.SelvstendigNæringsdrivende;

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

    private ProsessTaskRepository prosessTaskRepository;
    private FagsakTjeneste fagsakTjeneste;
    private EntityManager entityManager;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRepository behandlingRepository;

    private SjekkProsessering sjekkProsessering;
    
    private Pep pep;
    private BrukerTokenProvider tokenProvider;
    private StønadstatistikkService stønadstatistikkService;

    public ForvaltningMidlertidigDriftRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public ForvaltningMidlertidigDriftRestTjeneste(@FagsakYtelseTypeRef("FRISINN") FrisinnSøknadMottaker frisinnSøknadMottaker,
                                                   TpsTjeneste tpsTjeneste,
                                                   BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                                   FagsakTjeneste fagsakTjeneste,
                                                   ProsessTaskRepository prosessTaskRepository,
                                                   MottatteDokumentRepository mottatteDokumentRepository,
                                                   BehandlingRepository behandlingRepository,
                                                   SjekkProsessering sjekkProsessering,
                                                   EntityManager entityManager,
                                                   Pep pep,
                                                   BrukerTokenProvider tokenProvider,
                                                   StønadstatistikkService stønadstatistikkService) {

        this.frisinnSøknadMottaker = frisinnSøknadMottaker;
        this.tpsTjeneste = tpsTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.behandlingRepository = behandlingRepository;
        this.sjekkProsessering = sjekkProsessering;
        this.entityManager = entityManager;
        this.pep = pep;
        this.tokenProvider = tokenProvider;
        this.stønadstatistikkService = stønadstatistikkService;
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

        Fagsak fagsak = frisinnSøknadMottaker.finnEllerOpprettFagsak(FagsakYtelseType.FRISINN, aktørId, null, null, fom, tom);

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

            var taskData = new ProsessTaskData(OpprettManuellRevurderingTask.TASKTYPE);
            taskData.setSaksnummer(fagsak.getSaksnummer().getVerdi());
            taskData.setNesteKjøringEtter(LocalDateTime.now().plus(500L * idx, ChronoUnit.MILLIS)); // sprer utover hvert 1/2 sek.
            // lagrer direkte til prosessTaskRepository så vi ikke går via FagsakProsessTask (siden den bestemmer rekkefølge). Får unik callId per task
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
        
        @SuppressWarnings("unchecked")
        final List<String> result = q.getResultList();
        final String saksnummerliste = result.stream().reduce((a, b) -> a + ", " + b).orElse("");
        
        return Response.ok(saksnummerliste).build();
    }

    @GET
    @Path("/starttidspunkt-aapen-behandling")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Henter starttidspunkt for åpen behandling. Dvs tidspunktet den første søknaden kom inn på behandlingen i k9-sak.", summary = ("Henter starttidspunkt for åpen behandling. Dvs tidspunktet den første søknaden kom inn på behandlingen i k9-sak."), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response hentStarttidspunktÅpenBehandling() {
        final Query q = entityManager.createNativeQuery("select\n"
                + "  f.saksnummer,\n"
                +   "MIN(m.opprettet_tid) as tidspunkt,\n"
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

        @SuppressWarnings("unchecked")
        final List<Object[]> result = q.getResultList();
        final String restApiPath = "/starttidspunkt-aapen-behandling";
        final String resultatString = result.stream()
                .filter(a -> harLesetilgang(a[0].toString(), restApiPath))
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

        @SuppressWarnings("unchecked")
        final List<Object[]> result = q.getResultList();
        final String restApiPath = "/aapne-psb-med-soknad";
        final String resultatString = result.stream()
                .filter(a -> harLesetilgang(a[0].toString(), restApiPath))
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

        @SuppressWarnings("unchecked")
        final List<Object[]> result = q.getResultList();
        final String resultatString = result.stream()
                .map(a -> a[0].toString() + ";" + a[1].toString() + ";" + a[2].toString())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
        return Response.ok(resultatString).build();
    }

    private final boolean harLesetilgang(String saksnummer, String restApiPath) {
        final AbacAttributtSamling attributter = AbacAttributtSamling.medJwtToken(tokenProvider.getToken().getToken());
        attributter.setActionType(BeskyttetRessursActionAttributt.READ);
        attributter.setResource(DRIFT);

        // Package private:
        //attributter.setAction(restApiPath);
        attributter.leggTil(AbacDataAttributter.opprett().leggTil(StandardAbacAttributtType.SAKSNUMMER, new Saksnummer(saksnummer)));

        final Tilgangsbeslutning beslutning = pep.vurderTilgang(attributter);
        return beslutning.fikkTilgang();
    }

    @POST
    @Path("/marker-ugyldig")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Markerer et mottatt dokument som ugyldig", summary = ("Markerer angitt dokument som ugyldig"), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.UPDATE, resource = DRIFT)
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

        var dokumenter = mottatteDokumentRepository.hentMottatteDokument(fagsak.getId(), List.of(journalpostDto.getJournalpostId()));
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
