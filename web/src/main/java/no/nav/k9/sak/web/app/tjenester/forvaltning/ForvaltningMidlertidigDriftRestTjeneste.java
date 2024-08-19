package no.nav.k9.sak.web.app.tjenester.forvaltning;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.weld.exceptions.IllegalArgumentException;
import org.jboss.weld.exceptions.IllegalStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.felles.sikkerhet.abac.AbacAttributtSamling;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.AbacDto;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.Pep;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.k9.felles.sikkerhet.abac.Tilgangsbeslutning;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.felles.util.InputValideringRegex;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.kodeverk.person.Diskresjonskode;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.pip.PipRepository;
import no.nav.k9.sak.domene.person.pdl.TilknytningTjeneste;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.hendelse.stønadstatistikk.StønadstatistikkService;
import no.nav.k9.sak.kontrakt.KortTekst;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto;
import no.nav.k9.sak.kontrakt.mottak.AktørListeDto;
import no.nav.k9.sak.kontrakt.stønadstatistikk.StønadstatistikkSerializer;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentValidatorProvider;
import no.nav.k9.sak.mottak.dokumentmottak.HåndterMottattDokumentTask;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tasks.OpprettManuellRevurderingTask;
import no.nav.k9.sak.web.app.tasks.OpprettRevurderingService;
import no.nav.k9.sak.web.app.tjenester.behandling.SjekkProsessering;
import no.nav.k9.sak.web.app.tjenester.fordeling.FordelRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.k9.sak.web.app.tjenester.forvaltning.rapportering.DriftLesetilgangVurderer;
import no.nav.k9.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sikkerhet.context.SubjectHandler;

/**
 * DENNE TJENESTEN ER BARE FOR MIDLERTIDIG BEHOV, OG SKAL AVVIKLES SÅ RASKT SOM MULIG.
 */
@Path("")
@ApplicationScoped
@Transactional
public class ForvaltningMidlertidigDriftRestTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ForvaltningMidlertidigDriftRestTjeneste.class);

    private TpsTjeneste tpsTjeneste;
    private ProsessTaskTjeneste prosessTaskRepository;
    private FagsakTjeneste fagsakTjeneste;
    private EntityManager entityManager;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRepository behandlingRepository;

    private SjekkProsessering sjekkProsessering;

    private StønadstatistikkService stønadstatistikkService;

    private DriftLesetilgangVurderer lesetilgangVurderer;
    private OpprettRevurderingService opprettRevurderingService;

    private PipRepository pipRepository;

    private TilknytningTjeneste tilknytningTjeneste;
    private Pep pep;

    private DokumentValidatorProvider dokumentValidatorProvider;

    public ForvaltningMidlertidigDriftRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public ForvaltningMidlertidigDriftRestTjeneste(TpsTjeneste tpsTjeneste,
                                                   FagsakTjeneste fagsakTjeneste,
                                                   ProsessTaskTjeneste prosessTaskRepository,
                                                   MottatteDokumentRepository mottatteDokumentRepository,
                                                   BehandlingRepository behandlingRepository,
                                                   SjekkProsessering sjekkProsessering,
                                                   EntityManager entityManager,
                                                   StønadstatistikkService stønadstatistikkService,
                                                   DriftLesetilgangVurderer lesetilgangVurderer,
                                                   OpprettRevurderingService opprettRevurderingService,
                                                   PipRepository pipRepository,
                                                   TilknytningTjeneste tilknytningTjeneste,
                                                   Pep pep,
                                                   DokumentValidatorProvider dokumentValidatorProvider) {

        this.tpsTjeneste = tpsTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.behandlingRepository = behandlingRepository;
        this.sjekkProsessering = sjekkProsessering;
        this.entityManager = entityManager;
        this.stønadstatistikkService = stønadstatistikkService;
        this.lesetilgangVurderer = lesetilgangVurderer;
        this.opprettRevurderingService = opprettRevurderingService;
        this.pipRepository = pipRepository;
        this.tilknytningTjeneste = tilknytningTjeneste;
        this.pep = pep;
        this.dokumentValidatorProvider = dokumentValidatorProvider;
    }


    @GET
    @Path("finn-ubehandlede-saker-doed")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter liste av saker med ubehandlede behandlinger hvor dødsfall er årsak. Resultat er tenkt brukt i enhetenes prioritering av behandlinger. Kan fjernes når ny los løser samme behov. Bruk antall til å justere hvis kallet timer ut.")
    @Produces(MediaType.TEXT_PLAIN)
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response finnUbehandledeDødsfallBehandlinger(@NotNull @QueryParam("antall") @DecimalMin(value = "1") @DecimalMax(value = "1000") @Valid @TilpassetAbacAttributt(supplierClass = IngenAbacAttributterSupplier.class) Integer maxAntallSomSjekkes) {
        String preparedStatement = """
            select saksnummer
            from fagsak f
            join behandling b on f.id = b.fagsak_id
            join behandling_arsak ba on b.id = ba.behandling_id
            where f.ytelse_type = :ytelseType
               and behandling_status <> :behandlingAvsluttetStatus
               and avsluttet_dato is null
               and behandling_arsak_type in :behandlingAarsaker
            order by b.opprettet_tid;
            """;
        Query query = entityManager.createNativeQuery(preparedStatement)
            .setParameter("behandlingAvsluttetStatus", BehandlingStatus.AVSLUTTET.getKode())
            .setParameter("ytelseType", FagsakYtelseType.PSB.getKode())
            .setParameter("behandlingAarsaker", List.of(
                BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD.getKode(),
                BehandlingÅrsakType.RE_HENDELSE_DØD_BARN.getKode(),
                BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER.getKode()));

        int tellerKode6 = 0;
        int tellerKode7 = 0;
        List<String> saksnummerEgenAnsatt = new ArrayList<>();
        List<String> saksnummerUtenDiskresjonskode = new ArrayList<>();

        List resultat = query.getResultList();
        for (int i = 0; i < resultat.size() && i < maxAntallSomSjekkes; i++) {
            String saksnummer = (String) resultat.get(i);
            Set<AktørId> aktører = pipRepository.hentAktørIdKnyttetTilSaksnummer(new Saksnummer(saksnummer));
            Set<Diskresjonskode> diskresjonskoder = finnDiskresjonskoder(aktører);
            if (diskresjonskoder.contains(Diskresjonskode.KODE6)) {
                tellerKode6++;
            } else if (diskresjonskoder.contains(Diskresjonskode.KODE7)) {
                tellerKode7++;
            } else if (erEgenAnsatt(saksnummer)) {
                saksnummerEgenAnsatt.add(saksnummer);
            } else {
                saksnummerUtenDiskresjonskode.add(saksnummer);
            }
        }
        return Response.ok(
                "Saker med ubehandlede behandlinger pga dødsfall følger"
                    + ". Uten diskresjonskode:  " + saksnummerUtenDiskresjonskode
                    + ". Egen ansatt: " + saksnummerEgenAnsatt
                    + ". Antall med kode6 (ikke i listen): " + tellerKode6
                    + ". Antall med kode7 (ikke i listen): " + tellerKode7
                    + ". Spurte om første " + maxAntallSomSjekkes + " saker, fikk " + resultat.size() + " saker")
            .build();
    }

    public static class IngenAbacAttributterSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }

    private boolean erEgenAnsatt(String saksnummer) {
        //PRECONDITION: det SKAL sjekkes mot kode6 og kode7 før denne metoden kalles
        //PRECONDITION: virker bare hvis innlogget bruker ikke har tilgang til egen ansatt
        //i denne konteksten er det allerede sjekket om sak er beskyttet med kode 6/7 så hvis det ikke gis tilgang, skal det være pga egen ansatt
        String jwtForInnloggetBruker = Objects.requireNonNull(SubjectHandler.getSubjectHandler().getInternSsoToken());
        AbacAttributtSamling attributter = AbacAttributtSamling.medJwtToken(jwtForInnloggetBruker)
            .leggTil(AbacDataAttributter.opprett().leggTil(StandardAbacAttributtType.SAKSNUMMER, new Saksnummer(saksnummer)));
        attributter.setActionType(BeskyttetRessursActionAttributt.READ);
        attributter.setResource(DRIFT); //bruker samme som på REST-tjenesten
        Tilgangsbeslutning beslutning = pep.vurderTilgang(attributter);
        return !beslutning.fikkTilgang();
    }

    private Set<Diskresjonskode> finnDiskresjonskoder(Set<AktørId> aktører) {
        return aktører.stream()
            .map(aktørId -> tilknytningTjeneste.hentGeografiskTilknytning(aktørId).getDiskresjonskode())
            .collect(Collectors.toSet());
    }

    @POST
    @Path("/beskyttAktoerId")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Beskytt aktørid og oppdaterer nødvendige tabeller", tags = "forvaltning", responses = {
        @ApiResponse(responseCode = "200", description = "AktørId er endret."),
        @ApiResponse(responseCode = "400", description = "AktørId er uendret."),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")
    })
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = DRIFT)
    public Response beskyttAktoerId(@Parameter(description = "Liste med aktør-IDer") @TilpassetAbacAttributt(supplierClass = FordelRestTjeneste.AbacDataSupplier.class) @Valid AktørListeDto aktører) {
        /*
        for (AktørId aktørId : aktører.getAktører()) {
            personopplysningRepository.beskyttAktørId(aktørId);
        }

        return Response.ok().build();
        */
        throw new IllegalStateException("Dette kallet er deaktivert.");
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

    @POST
    @Path("/feriepengerevurdering")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Oppretter automatisk revurdering av feriepenger.", summary = ("Oppretter automatisk revurdering av feriepenger."), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public Response opprettFeriepengerevurdering(@Parameter(description = "Fødselsnumre (skilt med mellomrom eller linjeskift)") @Valid OpprettManuellRevurdering opprettManuellRevurdering) {
        var alleFødselsnumre = Objects.requireNonNull(opprettManuellRevurdering.getSaksnumre(), "fødselsnumre"); // XXX: gjenbruker DTO siden vi sletter koden etterpå.
        var fødselsnumre = new LinkedHashSet<>(Arrays.asList(alleFødselsnumre.split("\\s+")));

        final StringBuilder sb = new StringBuilder();
        for (var fødselsnummer : fødselsnumre) {
            try {
                final Optional<AktørId> aktørIdOpt = tpsTjeneste.hentAktørForFnr(PersonIdent.fra(fødselsnummer));
                final List<Fagsak> fagsaker = fagsakTjeneste.finnFagsakerForAktør(aktørIdOpt.get());
                for (Fagsak f : fagsaker) {
                    if (f.getYtelseType() == FagsakYtelseType.PLEIEPENGER_SYKT_BARN) {
                        opprettRevurderingService.opprettAutomatiskRevurdering(f.getSaksnummer(),
                            BehandlingÅrsakType.RE_REBEREGN_FERIEPENGER,
                            BehandlingStegType.START_STEG);
                    }
                }
            } catch (RuntimeException e) {
                sb.append(fødselsnummer + "\n");
            }
        }

        return Response.ok("Kjørt. Følgende fnr fikk ikke task: " + sb.toString()).build();
    }

    @POST
    @Path("/feriepengerevurdering-saksnumre")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Oppretter automatisk revurdering av feriepenger.", summary = ("Oppretter automatisk revurdering av feriepenger."), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = DRIFT)
    public Response opprettFeriepengerevurderingSaksnumre(@Parameter(description = "Saksnumre (skilt med mellomrom eller linjeskift)") @Valid OpprettManuellRevurdering opprettRevurdering) {
        var saksnummerStreng = Objects.requireNonNull(opprettRevurdering.getSaksnumre(), "saksnumre");
        var saksnumrene = new LinkedHashSet<>(Arrays.asList(saksnummerStreng.split("\\s+")));

        final StringBuilder sb = new StringBuilder();
        for (var saksnummer : saksnumrene) {
            try {
                Fagsak fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(new Saksnummer(saksnummer), false).orElseThrow(() -> new IllegalArgumentException("Finner ikke sak " + saksnummer));
                if (fagsak.getYtelseType() != PLEIEPENGER_SYKT_BARN && fagsak.getYtelseType() != PLEIEPENGER_NÆRSTÅENDE) {
                    throw new IllegalArgumentException("Fagsak " + saksnummer + " var ikke PSB eller PPN");
                }
                opprettRevurderingService.opprettAutomatiskRevurdering(new Saksnummer(saksnummer),
                    BehandlingÅrsakType.RE_REBEREGN_FERIEPENGER,
                    BehandlingStegType.START_STEG);
            } catch (RuntimeException e) {
                sb.append(saksnummer + "\n");
                logger.warn("Ignorerte " + saksnummer, e);
            }
        }

        return Response.ok("Kjørt. Følgende saksnumre fikk ikke task: " + sb).build();
    }

    @POST
    @Path("/feriepengerevurderingkandidater")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Oppretter task som logger saker som vør vurderes for revurdering av feriepenger etter fiks av at kvote er pr år", tags = "feriepenger")
    @BeskyttetRessurs(action = CREATE, resource = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response finnSakerSomSkalRevurderes() {
        String preparedStatement = """
            insert into prosess_task (task_type, id, task_sekvens, task_gruppe, task_parametere)
             with gruppe as (select nextval('seq_prosess_task_gruppe') as gruppe_id)
             select 'feriepenger.revurdering.kandidatutleder.task', nextval('seq_prosess_task'), row_number() over (order by saksnummer), gruppe_id, 'saksnummer=' || saksnummer
             from (
                     select saksnummer,
                            b.avsluttet_dato,
                            rank() over (partition by saksnummer order by b.avsluttet_dato desc nulls first) omvendt_rekkefolge
                     from fagsak f
                     join behandling b on f.id = b.fagsak_id
                     where f.ytelse_type = :ytelse_type
                      and b.behandling_resultat_type not in (:henleggelse_aarsaker)
                 ) t
             left outer join gruppe on (true)
             where omvendt_rekkefolge = 1 and avsluttet_dato < to_date('2022-11-19', 'YYYY-MM-DD')
            """;
        Query query = entityManager.createNativeQuery(preparedStatement)
            .setParameter("ytelse_type", FagsakYtelseType.PSB.getKode())
            .setParameter("henleggelse_aarsaker", BehandlingResultatType.getAlleHenleggelseskoder().stream().map(BehandlingResultatType::getKode).toList());
        int resultat = query.executeUpdate();
        logger.info("Lagde {} tasker for å sjekke kandidater for revurdering av feriepenger", resultat);
        return Response.ok("Lagde " + resultat + " tasker for å sjekke kandidater for revurdering av feriepenger").build();
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
        final String resultatString = result.stream()
            .filter(a -> lesetilgangVurderer.harTilgang(a[0].toString()))
            .map(a -> a[0].toString() + ";" + a[1].toString() + ";" + a[2].toString() + ";" + a[3].toString())
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
    @Path("/motta-ugyldig")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Mottar et dokument som er markert ugyldig", summary = ("Mottar et dokument som er markert ugyldig"), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = DRIFT)
    public Response mottaUgyldigDokument(@NotNull @FormParam("saksnummer") @Parameter(description = "saksnummer", required = true, schema = @Schema(type = "string", maximum = "10")) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto,
                                         @NotNull @FormParam("journalpost") @Parameter(description = "journalpost", required = true, schema = @Schema(type = "string", maximum = "20")) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) JournalpostIdDto journalpostDto,
                                         @NotNull @FormParam("begrunnelse") @Parameter(description = "begrunnelse", required = true, schema = @Schema(type = "string", maximum = "2000")) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) KortTekst begrunnelse) {

        var saksnummer = Objects.requireNonNull(saksnummerDto.getVerdi());
        var fagsakOpt = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true);

        if (fagsakOpt.isEmpty()) {
            return Response.status(Status.BAD_REQUEST.getStatusCode(), "Fant ikke fagsak for angitt saksnummer").build();
        }
        var fagsak = fagsakOpt.get();
        loggForvaltningTjeneste(fagsak, "/motta-ugyldig", begrunnelse.getTekst());

        List<MottattDokument> dokumenter = mottatteDokumentRepository.hentMottatteDokument(fagsak.getId(), List.of(journalpostDto.getJournalpostId()), DokumentStatus.UGYLDIG);
        if (dokumenter.size() > 1) {
            return Response.status(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Fant flere dokumenter for angitt saksnummer/journalpost - " + dokumenter.size()).build();
        }
        if (dokumenter.isEmpty()) {
            return Response.status(Status.NOT_FOUND.getStatusCode(), "Fant ingen ugyldige dokumenter for angitt saksnummer/journalpost").build();
        }

        var dokument = dokumenter.getFirst();

        var dokumentValidator = dokumentValidatorProvider.finnValidator(dokument.getType());
        try {
            dokumentValidator.validerDokument(dokument);
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Validering av dokumentet feilet med: " + e.getMessage()).build();
        }

        mottatteDokumentRepository.oppdaterStatus(dokumenter, DokumentStatus.MOTTATT);

        var prosessTaskData = ProsessTaskData.forProsessTask(HåndterMottattDokumentTask.class);
        prosessTaskData.setFagsakId(fagsak.getId());
        prosessTaskData.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, dokument.getId().toString());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);

        return Response.ok(prosessTaskData.getId()).build();
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

    @POST
    @Path("/saksnummerForBehandling")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Finner saksnummer for behandlinguuid", summary = ("Finner saksnummer for behandlinguuid"), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response hentSaksnummerForBehandling(
        @Parameter(description = "Behandling-UUID")
        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        BehandlingIdDto behandlingIdDto) {
        var behandlingUuid = behandlingIdDto.getBehandlingUuid();
        var behandlingId = behandlingIdDto.getBehandlingId();
        final var behandling = behandlingUuid != null ? behandlingRepository.hentBehandling(behandlingUuid) : behandlingRepository.hentBehandling(behandlingId);
        return Response.ok(behandling.getFagsak().getSaksnummer().getVerdi()).build();
    }

    @POST
    @Path("/vilkar-historikk-beregning")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter vilkårshistorikk for beregning", summary = ("Henter vilkårshistorikk for beregning"), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response hentVilkårhistorikkForBeregning(
        @Parameter(description = "Behandling-UUID")
        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        BehandlingIdDto behandlingIdDto) {
        var behandlingId = behandlingIdDto.getBehandlingId();

        var query = entityManager.createNativeQuery(
            "SELECT " +
                "vr.aktiv as aktiv, " +
                "vr.opprettet_tid as resultatOpprettetTid, " +
                "vr.endret_tid as resultatEndretTid, " +
                "v.opprettet_tid as vilkarOpprettetTid, " +
                "v.endret_tid as vilkarEndretTid, " +
                "p.fom as stp, " +
                "p.utfall as utfall " +
                "FROM RS_VILKARS_RESULTAT vr " +
                "INNER JOIN VR_VILKAR_RESULTAT vs on vs.id = vr.vilkarene_id " +
                "INNER JOIN VR_VILKAR v on v.vilkar_resultat_id = vs.id " +
                "INNER JOIN VR_VILKAR_PERIODE p on p.vilkar_id = v.id " +
                "WHERE vr.behandling_id = :behandlingId and v.vilkar_type = :vilkarType", Tuple.class);
        query.setParameter("behandlingId", behandlingId)
            .setParameter("vilkarType", "FP_VK_41");

        List<Tuple> resultList = query.getResultList();

        Optional<String> dataDump = CsvOutput.dumpResultSetToCsv(resultList);

        return dataDump.map(d -> Response.ok(d)
            .type(MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition", String.format("attachment; filename=\"dump.csv\""))
            .build()).orElse(Response.noContent().build());

    }

    @POST
    @Path("/behandlingsteg-historikk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter behandlingsteghistorikk",
        summary = ("Henter behandlingsteghistorikk"),
        tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response hentBehandlingHistorikk(
        @Parameter(description = "Behandling-UUID")
        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        BehandlingIdDto behandlingIdDto) {
        var behandlingId = behandlingIdDto.getBehandlingId();

        var query = entityManager.createNativeQuery(
            "SELECT " +
                "opprettet_tid, " +
                "endret_tid, " +
                "behandling_steg,  " +
                "behandling_steg_status " +
                "FROM BEHANDLING_STEG_TILSTAND stegTilstand " +
                "WHERE stegTilstand.behandling_id = :behandlingId order by opprettet_tid asc ", Tuple.class);
        query.setParameter("behandlingId", behandlingId);

        List<Tuple> resultList = query.getResultList();

        Optional<String> dataDump = CsvOutput.dumpResultSetToCsv(resultList);

        return dataDump.map(d -> Response.ok(d)
            .type(MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition", String.format("attachment; filename=\"behandlingsteghistorikk.csv\""))
            .build()).orElse(Response.noContent().build());
    }


    public static class AvbrytAksjonspunktDto {

        @Valid
        @NotNull
        private BehandlingIdDto behandlingId;

        @NotNull
        @Size(min = 1, max = 15)
        @Pattern(regexp = "^\\d+$")
        private String aksjonspunktKode;

        @NotNull
        @Size(min = 1, max = 1000)
        @Pattern(regexp = InputValideringRegex.FRITEKST)
        private String begrunnelse;

        @AbacAttributt("behandlingId")
        public Long getBehandlingId() {
            return behandlingId.getBehandlingId();
        }


        @AbacAttributt("behandlingUuid")
        public UUID getBehandlingUuid() {
            return behandlingId.getBehandlingUuid();
        }

        public void setBehandlingId(BehandlingIdDto behandlingId) {
            this.behandlingId = behandlingId;
        }

        public String getAksjonspunktKode() {
            return aksjonspunktKode;
        }

        public void setAksjonspunktKode(String aksjonspunktKode) {
            this.aksjonspunktKode = aksjonspunktKode;
        }

        public String getBegrunnelse() {
            return begrunnelse;
        }

        public void setBegrunnelse(String begrunnelse) {
            this.begrunnelse = begrunnelse;
        }
    }

    @POST
    @Path("/avbryt-aksjonspunkt")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Avbryter feilopprettet aksjonspunkt",
        summary = ("Avbryter feilopprettet aksjonspunkt"),
        tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    @Produces(MediaType.TEXT_PLAIN)
    public Response avbrytAksjonspunkt(@NotNull @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) AvbrytAksjonspunktDto dto) {

        List<String> aksjonspunktSomKanAvbrytesHer = List.of(AksjonspunktKodeDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP_KODE);
        if (!aksjonspunktSomKanAvbrytesHer.contains(dto.getAksjonspunktKode())) {
            throw new java.lang.IllegalArgumentException("Aksjonspunkt " + dto.getAksjonspunktKode() + " kan ikke avbrytes her");
        }

        Behandling behandling = dto.getBehandlingId() != null
            ? behandlingRepository.hentBehandling(dto.getBehandlingId())
            : behandlingRepository.hentBehandling(dto.getBehandlingUuid());
        if (behandling.erAvsluttet()) {
            throw new IllegalStateException("Behandling er avsluttet");
        }
        if (!behandling.isBehandlingPåVent()){
            throw new IllegalStateException("Behandlingen må være på vent");
        }
        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktKode()).orElseThrow(() -> new IllegalStateException("Har ikke aksjonspunkt med kode " + dto.getAksjonspunktKode()));
        if (!aksjonspunkt.erÅpentAksjonspunkt()) {
            throw new IllegalStateException("Aksjonspunktet har status: " + aksjonspunkt.getStatus());
        }


        aksjonspunkt.avbryt();
        entityManager.persist(aksjonspunkt);

        loggForvaltningTjeneste(behandling.getFagsak(), "avbryt-aksjonspunkt:" + dto.getAksjonspunktKode(), dto.begrunnelse);

        return Response.ok("Aksjonspunkt ble fjernet").build();
    }

    private void loggForvaltningTjeneste(Fagsak fagsak, String tjeneste, String begrunnelse) {
        /*
         * logger at tjenesten er kalt (er en forvaltnings tjeneste)
         */
        entityManager.persist(new DiagnostikkFagsakLogg(fagsak.getId(), tjeneste, begrunnelse));
        entityManager.flush();
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
                    br.lines().forEach(l -> sb.append(l).append('\n'));
                }

                return new OpprettManuellRevurdering(sb.toString());

            }
        }
    }
}
