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
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.kontrakt.ekstern.tilleggsstonader.AktivitetspengerPeriode;
import no.nav.ung.sak.kontrakt.ekstern.tilleggsstonader.AktivitetspengerPerioderRequest;
import no.nav.ung.sak.kontrakt.ekstern.tilleggsstonader.AktivitetspengerPerioderResponse;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.web.server.abac.EksterneSystemer;
import no.nav.ung.ytelse.aktivitetspenger.perioder.AktivitetspengerPerioderTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

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
        @NotNull @Valid @TilpassetAbacAttributt(supplierClass = IngenAbacAttributter.class) AktivitetspengerPerioderRequest request) {
        var aktørIder = aktørTjeneste.hentAlleAktørIderForPersonIdent(new PersonIdent(request.ident()));
        var perioder = aktivitetspengerPerioderTjeneste.hentInnvilgedePerioder(aktørIder, new Periode(request.fom(), request.tom())).stream()
            .map(p -> new AktivitetspengerPeriode(p.getFom(), p.getTom()))
            .toList();

        log.info("Tilleggsstønader hentet aktivitetspengeperioder. konsument={}, antallPerioder={}",
            SubjectHandler.getSubjectHandler().getUid(), perioder.size());
        return new AktivitetspengerPerioderResponse(perioder);
    }

    /**
     * Endepunktet er kun for systemkall, og tilgang avgjøres på ressurstype. Fnr sendes derfor ikke videre som abac-attributt.
     */
    public static class IngenAbacAttributter implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }
}
