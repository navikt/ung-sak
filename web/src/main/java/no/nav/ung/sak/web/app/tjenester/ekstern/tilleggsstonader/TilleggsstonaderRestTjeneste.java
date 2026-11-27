package no.nav.ung.sak.web.app.tjenester.ekstern.tilleggsstonader;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.kontrakt.ekstern.tilleggsstonader.AktivitetspengerPeriode;
import no.nav.ung.sak.kontrakt.ekstern.tilleggsstonader.AktivitetspengerPerioderRequest;
import no.nav.ung.sak.kontrakt.ekstern.tilleggsstonader.AktivitetspengerPerioderResponse;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.web.app.tjenester.ekstern.EksterntKallLoggkontekst;
import no.nav.ung.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.ung.sak.web.server.abac.EksterneSystemer;
import no.nav.ung.ytelse.aktivitetspenger.perioder.AktivitetspengerPerioderTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

/**
 * Systemtjenester for tilleggsstønader (tilleggsstonader-integrasjoner). Kun maskin-til-maskin.
 * Tilleggsstønader har ansvaret for tilgangskontroll per person og revisjonslogg; systemkall auditlogges ikke i ung-sak.
 */
@Path(TilleggsstonaderRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class TilleggsstonaderRestTjeneste {

    static final String BASE_PATH = "/ekstern/tilleggsstonader";
    static final String AKTIVITETSPENGER_PERIODER_PATH = "/aktivitetspenger/perioder";

    private static final Logger log = LoggerFactory.getLogger(TilleggsstonaderRestTjeneste.class);

    private AktørTjeneste aktørTjeneste;
    private AktivitetspengerPerioderTjeneste aktivitetspengerPerioderTjeneste;

    public TilleggsstonaderRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public TilleggsstonaderRestTjeneste(AktørTjeneste aktørTjeneste, AktivitetspengerPerioderTjeneste aktivitetspengerPerioderTjeneste) {
        this.aktørTjeneste = aktørTjeneste;
        this.aktivitetspengerPerioderTjeneste = aktivitetspengerPerioderTjeneste;
    }

    @POST
    @Path(AKTIVITETSPENGER_PERIODER_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter innvilgede perioder med aktivitetspenger som overlapper forespurt periode. Kun for tilleggsstønader.",
        tags = "ekstern",
        responses = {
            @ApiResponse(responseCode = "200", description = "Innvilgede perioder. Tom liste dersom personen ikke har vedtak om aktivitetspenger.",
                content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AktivitetspengerPerioderResponse.class)))
        })
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.EKSTERN_SYSTEM,
        eksterneSystemer = {
            EksterneSystemer.TILLEGGSSTØNADER_DEV,
            EksterneSystemer.TILLEGGSSTØNADER_PROD
        })
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public AktivitetspengerPerioderResponse hentAktivitetspengerPerioder(
        @NotNull @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) AktivitetspengerPerioderRequest request) {
        EksterntKallLoggkontekst.leggTilKall(BASE_PATH + AKTIVITETSPENGER_PERIODER_PATH);
        var aktørIder = aktørTjeneste.hentAlleAktørIderForPersonIdent(request.ident());
        var innvilgede = aktivitetspengerPerioderTjeneste.hentInnvilgedeOverlappendePerioder(aktørIder, new Periode(request.fom(), request.tom()));
        EksterntKallLoggkontekst.leggTilSaksnumre(innvilgede.saksnumre().stream().map(Saksnummer::getVerdi).toList());
        var perioder = innvilgede.perioder().stream()
            .map(p -> new AktivitetspengerPeriode(p.getFom(), p.getTom()))
            .toList();

        log.info("Tilleggsstønader hentet aktivitetspengeperioder. antallPerioder={}", perioder.size());
        return new AktivitetspengerPerioderResponse(perioder);
    }
}
