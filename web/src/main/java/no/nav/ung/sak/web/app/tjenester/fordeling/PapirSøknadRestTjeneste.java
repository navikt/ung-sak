package no.nav.ung.sak.web.app.tjenester.fordeling;


import io.swagger.v3.oas.annotations.Operation;
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
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.dokument.arkiv.DokumentArkivTjeneste;
import no.nav.ung.sak.kontrakt.søknad.HentPapirSøknadRequestDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.io.ByteArrayInputStream;

import static no.nav.ung.sak.web.app.tjenester.fordeling.PapirSøknadRestTjeneste.BASE_PATH;

@Path(BASE_PATH)
@ApplicationScoped
@Transactional
public class PapirSøknadRestTjeneste {
    static final String BASE_PATH = "/papir";

    private DokumentArkivTjeneste dokumentArkivTjeneste;

    public PapirSøknadRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public PapirSøknadRestTjeneste(DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    @POST
    @Path("/hentPapirSøknad")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(description = "Henter og viser papirsøknad. Husk å slette dokumentet lokalt etter at du er ferdig.", summary = ("Henter og viser papirsøknad"), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.DRIFT)
    // Kan bruke drift fordi kallet mot SAF gjør tilgangskontroll uansett.
    public Response hentPapirSøknad(@NotNull @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) HentPapirSøknadRequestDto hentPapirSøknadRequestDto) {

        // SafTjeneste gjør tilgangskontroll på journalpostId internt gjennom kall til SAF
        byte[] dokument = dokumentArkivTjeneste.hentDokument(hentPapirSøknadRequestDto.journalpostId(), hentPapirSøknadRequestDto.dokumentId().getVerdi());
        String filnavn = "søknadsdokument-" + hentPapirSøknadRequestDto.dokumentId() + ".pdf";

        try {
            Response.ResponseBuilder responseBuilder = Response.ok(new ByteArrayInputStream(dokument));
            responseBuilder.header("Content-Disposition", "inline; filename=\"" + filnavn + "\"");
            return responseBuilder.build();
        } catch (Exception e) {
            return Response.serverError().entity("Klarte ikke å generere PDF: " + e.getMessage()).build();
        }
    }

}
