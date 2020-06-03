package no.nav.k9.sak.web.app.tjenester.frisinn;


import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.OVERSTYRING_FRISINN_OPPGITT_OPPTJENING;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
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
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("")
@ApplicationScoped
@Transactional
public class ManuellFRISINNsøknadRestTjeneste {

    private static final String JSON_UTF8 = "application/json; charset=UTF-8";
    public static final String IKKE_I_BRUK = "IKKE_I_BRUK";
    private FrisinnSøknadMottaker frisinnSøknadMottaker;
    private TpsTjeneste tpsTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    public ManuellFRISINNsøknadRestTjeneste() {
        // For Rest-CDI
    }


    @Inject
    public ManuellFRISINNsøknadRestTjeneste(@FagsakYtelseTypeRef("FRISINN") FrisinnSøknadMottaker frisinnSøknadMottaker,
                                            TpsTjeneste tpsTjeneste,
                                            BehandlingskontrollTjeneste behandlingskontrollTjeneste) {

        this.frisinnSøknadMottaker = frisinnSøknadMottaker;
        this.tpsTjeneste = tpsTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }


    @POST
    @Path("/frisinn/opprett-manuell-frisinn")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Opprett behandling hvor saksbehandler kan legge inn inntektsopplysninger", summary = ("Returnerer saksnummer som er tilknyttet den nye fagsaken som har blitt opprettet."), tags = "frisinn", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer saksnummer", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SaksnummerDto.class)))
    })
    @Produces(JSON_UTF8)
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public Response opprettManuellFrisinnSøknad(@Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) ManuellSøknadDto manuellSøknadDto) {
        PersonIdent fnr = new PersonIdent(manuellSøknadDto.getFnr());
        Optional<AktørId> aktørIdOpt = tpsTjeneste.hentAktørForFnr(fnr);
        if (aktørIdOpt.isPresent()) {
            AktørId aktørId = aktørIdOpt.get();
            Fagsak fagsak = frisinnSøknadMottaker.finnEllerOpprettFagsak(FagsakYtelseType.FRISINN, aktørId, null, LocalDate.now());
            FrisinnSøknadInnsending frisinnSøknadInnsending = new FrisinnSøknadInnsending();

            frisinnSøknadInnsending.setSøknad(FrisinnSøknad.builder()
                .språk(Språk.NORSK_BOKMÅL)
                //denne lagres ikke
                .søknadId(SøknadId.of(IKKE_I_BRUK))
                .inntekter(lagDummyInntekt(manuellSøknadDto))
                .søknadsperiode(manuellSøknadDto.getPeriode())
                .mottattDato(ZonedDateTime.now(ZoneId.of("Europe/Paris")))
                .søker(Søker.builder().norskIdentitetsnummer(NorskIdentitetsnummer.of(fnr.getIdent())).build())
                .build());

            var behandling = frisinnSøknadMottaker.mottaSøknad(fagsak.getSaksnummer(), null, frisinnSøknadInnsending);

            BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
            //ønsker at saksbehandler må ta stilling til disse
            behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, List.of(OVERSTYRING_FRISINN_OPPGITT_OPPTJENING, KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING));

            return Response.ok(new SaksnummerDto(fagsak.getSaksnummer())).build();

        } else {
            return Response.noContent().build();
        }
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
