package no.nav.ung.sak.web.app.tjenester.ekstern.ytelse;

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
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.BarnetilleggResponse;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.YtelseRequest;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.YtelseResponse;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.vedtattytelse.VedtakYtelseTjeneste;
import no.nav.ung.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.ung.sak.web.server.abac.EksterneSystemer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

/**
 * Vedtak om aktivitetspenger og ungdomsprogramytelsen for andre ytelser i Nav som skal samordnes mot dem. Kun maskin-til-maskin.
 * <p>
 * Tilgang avgjøres av ressurstypen {@code EKSTERN_SYSTEM} og listen med systemer på hvert endepunkt.
 */
@Path(EksternYtelseRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class EksternYtelseRestTjeneste {

    static final String BASE_PATH = "/ekstern/ytelse";
    static final String VEDTAK_PATH = "/v1/vedtak";
    static final String BARNETILLEGG_PATH = "/v1/barnetillegg";

    private static final Logger log = LoggerFactory.getLogger(EksternYtelseRestTjeneste.class);

    private AktørTjeneste aktørTjeneste;
    private VedtakYtelseTjeneste vedtakYtelseTjeneste;

    public EksternYtelseRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public EksternYtelseRestTjeneste(AktørTjeneste aktørTjeneste, VedtakYtelseTjeneste vedtakYtelseTjeneste) {
        this.aktørTjeneste = aktørTjeneste;
        this.vedtakYtelseTjeneste = vedtakYtelseTjeneste;
    }

    @POST
    @Path(VEDTAK_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter gjeldende vedtak om aktivitetspenger og ungdomsprogramytelsen som overlapper forespurt periode, med alle periodene i vedtaket. Kun for andre ytelser i Nav.",
        tags = "ekstern",
        responses = {
            @ApiResponse(responseCode = "200", description = "Vedtak med perioder. Tom liste dersom personen ikke har vedtak i perioden.",
                content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = YtelseResponse.class)))
        })
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.EKSTERN_SYSTEM,
        eksterneSystemer = {
            EksterneSystemer.AAP_DEV,
            EksterneSystemer.DAGPENGER_DEV,
            EksterneSystemer.TILTAKSPENGER_DEV,
            EksterneSystemer.AZURE_TOKEN_GENERATOR_DEV
        })
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public YtelseResponse hentVedtak(
        @NotNull @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) YtelseRequest request) {
        var aktørIder = aktørTjeneste.hentAlleAktørIderForPersonIdent(request.ident());
        var respons = vedtakYtelseTjeneste.hentVedtak(aktørIder, new Periode(request.fom(), request.tom()));
        log.info("Eksternt system hentet vedtak. konsument={}, antallVedtak={}", SubjectHandler.getSubjectHandler().getUid(), respons.vedtak().size());
        return respons;
    }

    @POST
    @Path(BARNETILLEGG_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter barnetillegg i gjeldende vedtak om aktivitetspenger og ungdomsprogramytelsen som overlapper forespurt periode. Kun for barnebidrag.",
        tags = "ekstern",
        responses = {
            @ApiResponse(responseCode = "200", description = "Vedtak med perioder med barnetillegg. Tom liste dersom personen ikke har barnetillegg i perioden.",
                content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BarnetilleggResponse.class)))
        })
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.EKSTERN_SYSTEM,
        eksterneSystemer = {
            EksterneSystemer.BIDRAG_DEV,
            EksterneSystemer.AZURE_TOKEN_GENERATOR_DEV
        })
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BarnetilleggResponse hentBarnetillegg(
        @NotNull @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) YtelseRequest request) {
        var aktørIder = aktørTjeneste.hentAlleAktørIderForPersonIdent(request.ident());
        var respons = vedtakYtelseTjeneste.hentBarnetillegg(aktørIder, new Periode(request.fom(), request.tom()));
        log.info("Eksternt system hentet barnetillegg. konsument={}, antallVedtak={}", SubjectHandler.getSubjectHandler().getUid(), respons.vedtak().size());
        return respons;
    }
}
