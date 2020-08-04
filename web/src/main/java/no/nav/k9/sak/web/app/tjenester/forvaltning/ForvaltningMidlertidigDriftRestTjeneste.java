package no.nav.k9.sak.web.app.tjenester.forvaltning;


import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.OVERSTYRING_FRISINN_OPPGITT_OPPTJENING;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jetbrains.annotations.NotNull;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.person.tps.TpsAdapter;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.kontrakt.FeilDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.frisinn.mottak.FrisinnSøknadInnsending;
import no.nav.k9.sak.ytelse.frisinn.mottak.FrisinnSøknadMottaker;
import no.nav.k9.søknad.felles.NorskIdentitetsnummer;
import no.nav.k9.søknad.felles.Periode;
import no.nav.k9.søknad.felles.Språk;
import no.nav.k9.søknad.felles.Søker;
import no.nav.k9.søknad.felles.SøknadId;
import no.nav.k9.søknad.frisinn.FrisinnSøknad;
import no.nav.k9.søknad.frisinn.Inntekter;
import no.nav.k9.søknad.frisinn.PeriodeInntekt;
import no.nav.k9.søknad.frisinn.SelvstendigNæringsdrivende;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;


/**
 * DENNE TJENESTEN ER BARE FOR MIDLERTIDIG BEHOV, OG SKAL AVVIKLES SÅ RASKT SOM MULIG.
 * ENDRINGER I DENNE KLASSEN SKAL KLARERES OG KODE-REVIEWES MED ANSVARLIG APPLIKASJONSARKITEKT.
 */
@Path("")
@ApplicationScoped
@Transactional
public class ForvaltningMidlertidigDriftRestTjeneste {

    private static final String JSON_UTF8 = "application/json; charset=UTF-8";
    private FrisinnSøknadMottaker frisinnSøknadMottaker;
    private TpsTjeneste tpsTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    
    private AksjonspunktRepository aksjonspunktRepository;
    private TpsAdapter tpsAdapter;

    public ForvaltningMidlertidigDriftRestTjeneste() {
        // For Rest-CDI
    }


    @Inject
    public ForvaltningMidlertidigDriftRestTjeneste(@FagsakYtelseTypeRef("FRISINN") FrisinnSøknadMottaker frisinnSøknadMottaker,
                                        TpsTjeneste tpsTjeneste,
                                        BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                        AksjonspunktRepository aksjonspunktRepository, 
                                        TpsAdapter tpsAdapter) {

        this.frisinnSøknadMottaker = frisinnSøknadMottaker;
        this.tpsTjeneste = tpsTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.aksjonspunktRepository = aksjonspunktRepository;
        this.tpsAdapter = tpsAdapter;
    }


    @POST
    @Path("/frisinn/opprett-manuell-frisinn/TO_BE_REMOVED")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Opprett behandling hvor saksbehandler kan legge inn inntektsopplysninger", summary = ("Returnerer saksnummer som er tilknyttet den nye fagsaken som har blitt opprettet."), tags = "frisinn", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer saksnummer", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SaksnummerDto.class)))
    })
    @Produces(JSON_UTF8)
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public Response opprettManuellFrisinnSøknad(@Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) ManuellSøknadDto manuellSøknadDto) {
        PersonIdent fnr = new PersonIdent(manuellSøknadDto.getFnr());
        AktørId aktørId = tpsTjeneste.hentAktørForFnr(fnr).orElse(null);
        if (aktørId == null) {
            return Response.serverError().entity(new FeilDto("Oppgitt personummer er ukjent")).build();
        }

        Fagsak fagsak = frisinnSøknadMottaker.finnEllerOpprettFagsak(FagsakYtelseType.FRISINN, aktørId, null, LocalDate.now());

        FrisinnSøknad søknad = FrisinnSøknad.builder()
            .språk(Språk.NORSK_BOKMÅL)
            .søknadId(SøknadId.of("ManueltOpprettet" + fagsak.getSaksnummer().getVerdi())) // lagres ingensteds
            .inntekter(lagDummyInntekt(manuellSøknadDto))
            .søknadsperiode(manuellSøknadDto.getPeriode())
            .mottattDato(ZonedDateTime.now(ZoneId.of("Europe/Paris")))
            .søker(Søker.builder().norskIdentitetsnummer(NorskIdentitetsnummer.of(fnr.getIdent())).build())
            .build();
        var valideringsfeil = validerSøknad(fagsak, søknad);
        if (valideringsfeil.isPresent()) {
            return Response.serverError().entity(new FeilDto(valideringsfeil.get().getMessage())).build();
        }

        FrisinnSøknadInnsending frisinnSøknadInnsending = new FrisinnSøknadInnsending();
        frisinnSøknadInnsending.setSøknad(søknad);
        var behandling = frisinnSøknadMottaker.mottaSøknad(fagsak.getSaksnummer(), null, frisinnSøknadInnsending);

        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        //ønsker at saksbehandler må ta stilling til disse
        behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, List.of(OVERSTYRING_FRISINN_OPPGITT_OPPTJENING, KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING));

        return Response.ok(new SaksnummerDto(fagsak.getSaksnummer())).build();
    }
    
    @GET
    @Path("/uttrekk-aksjonspunkt-9003")
    @Operation(description = "Henter fødselsnummer for alle personer med fagsak som har åpent 9003-aksjonspunkt.", summary = ("Henter fødselsnummer for alle personer med fagsak som har åpent 9003-aksjonspunkt."), tags = "forvaltning", responses = {
        @ApiResponse(responseCode = "200", description = "Gir linjeskiftseparerte fødselsnummere,", content = @Content(mediaType = MediaType.TEXT_PLAIN))
    })
    @Produces(MediaType.TEXT_PLAIN)
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = FAGSAK)
    public Response personerMedAksjonspunkt9003(@TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class) @Valid @Parameter(description = "Sidenummer (starter på 0)") @QueryParam("side") int side,
            @TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class) @Valid @Parameter(description = "Antall per side") @QueryParam("antall") int antall) {
        final List<AktørId> aktører = new ArrayList<>(aksjonspunktRepository.hentAktørerMedAktivtAksjonspunkt(AksjonspunktDefinisjon.VURDER_ÅRSKVANTUM_KVOTE));
        Collections.sort(aktører);
        
        final int begin = side * antall;
        if (begin >= aktører.size()) {
            return Response.ok("").build();
        }
        
        final int end = Math.min(begin + antall, aktører.size());
        
        final String[] result = aktører.subList(begin, end).stream()
            .map(a -> tpsAdapter.hentIdentForAktørId(a)
                .map(v -> v.getIdent())
                .orElseGet(() -> "UKJENT AKTØRID")
            )
            .distinct()
            .toArray(String[]::new);
        
        return Response.ok(String.join("\n", result)).build();
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
}
