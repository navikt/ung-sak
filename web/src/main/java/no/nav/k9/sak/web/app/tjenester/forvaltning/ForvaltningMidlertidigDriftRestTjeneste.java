package no.nav.k9.sak.web.app.tjenester.forvaltning;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.OVERSTYRING_FRISINN_OPPGITT_OPPTJENING;

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
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jetbrains.annotations.NotNull;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.AbacDto;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.kontrakt.FeilDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.OpprettManuellRevurderingTask;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.frisinn.mottak.FrisinnSøknadInnsending;
import no.nav.k9.sak.ytelse.frisinn.mottak.FrisinnSøknadMottaker;
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

    private AksjonspunktRepository aksjonspunktRepository;

    private ProsessTaskRepository prosessTaskRepository;
    private FagsakTjeneste fagsakTjeneste;

    public ForvaltningMidlertidigDriftRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public ForvaltningMidlertidigDriftRestTjeneste(@FagsakYtelseTypeRef("FRISINN") FrisinnSøknadMottaker frisinnSøknadMottaker,
                                                   TpsTjeneste tpsTjeneste,
                                                   BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                                   AksjonspunktRepository aksjonspunktRepository,
                                                   FagsakTjeneste fagsakTjeneste,
                                                   ProsessTaskRepository prosessTaskRepository) {

        this.frisinnSøknadMottaker = frisinnSøknadMottaker;
        this.tpsTjeneste = tpsTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.aksjonspunktRepository = aksjonspunktRepository;
        this.fagsakTjeneste = fagsakTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
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
    @Path("/manuell-revurdering")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(description = "Oppretter manuell revurdering med annet som årsak.", summary = ("Oppretter manuell revurdering med annet som årsak."), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public void revurderAlleSomAnnenFeil(@Parameter(description = "Saksnumre (skilt med mellomrom eller linjeskift)") @Valid OpprettManuellRevurdering opprettManuellRevurdering) {
        var alleSaksnummer = Objects.requireNonNull(opprettManuellRevurdering.getSaksnumre(), "saksnumre");
        var saknumre = new LinkedHashSet<>(Arrays.asList(alleSaksnummer.split("\\s+")));

        int idx = 0;
        for (var s : saknumre) {
            var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(new Saksnummer(s), false).orElseThrow(() -> new IllegalArgumentException("finnes ikke fagsak med saksnummer: " + s));
            var taskData = new ProsessTaskData(OpprettManuellRevurderingTask.TASKTYPE);
            taskData.setSaksnummer(fagsak.getSaksnummer().getVerdi());
            taskData.setNesteKjøringEtter(LocalDateTime.now().plus(500L * idx, ChronoUnit.MILLIS)); // sprer utover hvert 1/2 sek.
            // lagrer direkte til prosessTaskRepository så vi ikke går via FagsakProsessTask (siden den bestemmer rekkefølge). Får unik callId per task
            prosessTaskRepository.lagre(taskData);
            idx++;
        }

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
        private final String saksnumre;

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
    }
}
