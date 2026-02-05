package no.nav.ung.sak.web.app.tjenester.fordeling;


import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.ytelse.ungdomsprogramytelsen.papirsøknad.PapirsøknadHåndteringTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.papirsøknad.PapirsøknadPdf;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostResponse;
import no.nav.ung.sak.kontrakt.søknad.JournalførPapirSøknadDto;
import no.nav.ung.sak.kontrakt.søknad.SendInnPapirsøknadopplysningerRequestDto;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.io.ByteArrayInputStream;

import static no.nav.ung.sak.web.app.tjenester.fordeling.PapirSøknadRestTjeneste.BASE_PATH;

@Path(BASE_PATH)
@ApplicationScoped
@Transactional
public class PapirSøknadRestTjeneste {
    static final String BASE_PATH = "/papir";
    private static final String PAPIRSØKNAD_TAG = "papirsøknad";

    private PapirsøknadHåndteringTjeneste papirsøknadHåndteringTjeneste;


    public PapirSøknadRestTjeneste() {// For Rest-CDI

    }

    @Inject
    public PapirSøknadRestTjeneste(
        PapirsøknadHåndteringTjeneste papirsøknadHåndteringTjeneste) {
        this.papirsøknadHåndteringTjeneste = papirsøknadHåndteringTjeneste;
    }

    @POST
    @Path("/steg-1/hent-søknad-pdf")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(description = "Henter og viser papirsøknad. Husk å slette dokumentet lokalt etter at du er ferdig.", summary = ("Henter og viser papirsøknad"), tags = PAPIRSØKNAD_TAG)
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.DRIFT)
    // Kan bruke drift fordi kallet mot SAF gjør tilgangskontroll uansett.
    public Response hentPapirSøknad(@NotNull @QueryParam("journalpostId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) JournalpostId journalpostId) {
        try {
            // SafTjeneste gjør tilgangskontroll på journalpostId internt gjennom kall til SAF
            PapirsøknadPdf papirsøknadPdf = papirsøknadHåndteringTjeneste.hentDokumentForJournalpostId(journalpostId);

            return Response.ok(new ByteArrayInputStream(papirsøknadPdf.dokument()))
                .header("Content-Disposition", "inline; filename=\"" + papirsøknadPdf.filnavn() + "\"")
                .build();

        } catch (Exception e) {
            return Response.serverError().entity("Klarte ikke å generere PDF: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/steg-2/journalfør-papir-søknad-mot-fagsak")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter fagsak hvis det ikke allerede finnes en, og gjøre en endelig journalføring av papirsøknaden med fagsakstilknytning.", summary = ("Oppretter fagsak og journalfører papirsøknad"), tags = PAPIRSØKNAD_TAG)
    @BeskyttetRessurs(action = BeskyttetRessursActionType.CREATE, resource = BeskyttetRessursResourceType.FAGSAK)
    public Response journalførPapirSøknad(@NotNull @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) JournalførPapirSøknadDto journalførPapirSøknadDto) {
        try {
            String saksnummer = papirsøknadHåndteringTjeneste
                .journalførPapirsøknadMotFagsak(
                    journalførPapirSøknadDto.deltakerIdent(),
                    journalførPapirSøknadDto.journalpostId())
                .getVerdi();

            String response = """
                {
                  "saksnummer": "%s"
                }
                """.formatted(
                saksnummer);

            return Response.ok().entity(response).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/steg-3/send-inn-papirsøknadopplysninger")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Mapper til strukturert søknadsopplysninger og oppretter journalpost.", summary = ("Mapper til strukturert søknadsopplysninger og oppretter journalpost."), tags = PAPIRSØKNAD_TAG)
    @BeskyttetRessurs(action = BeskyttetRessursActionType.CREATE, resource = BeskyttetRessursResourceType.FAGSAK)
    public OpprettJournalpostResponse sendInnPapirsøknadopplysninger(@NotNull @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SendInnPapirsøknadopplysningerRequestDto dto) {
        return papirsøknadHåndteringTjeneste.opprettJournalpostForInnsendtPapirsøknad(
            PersonIdent.fra(dto.deltakerIdent()),
            dto.journalpostIdForPapirsøknad()
        );
    }
}
