package no.nav.ung.sak.web.app.tjenester.fordeling;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import no.nav.k9.felles.sikkerhet.abac.*;
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt;
import no.nav.sif.abac.kontrakt.abac.ResourceType;
import no.nav.sif.abac.kontrakt.abac.dto.OperasjonDto;
import no.nav.sif.abac.kontrakt.abac.dto.SaksinformasjonOgPersonerTilgangskontrollInputDto;
import no.nav.sif.abac.kontrakt.abac.resultat.Tilgangsbeslutning;
import no.nav.sif.abac.kontrakt.person.PersonIdent;
import no.nav.ung.sak.dokument.arkiv.DokumentArkivTjeneste;
import no.nav.ung.sak.kontrakt.søknad.HentPapirSøknadRequestDto;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.sak.web.server.abac.SifAbacPdpRestKlient;
import org.jboss.logging.annotations.Param;

import java.util.List;
import java.util.Set;

@Path(FordelRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class PapirSøknadRestTjeneste {
    static final String BASE_PATH = "/papir";
    private static final String JSON_UTF8 = "application/json; charset=UTF-8";
    private SifAbacPdpRestKlient sifAbacPdpRestKlient;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    public PapirSøknadRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public PapirSøknadRestTjeneste(DokumentArkivTjeneste dokumentArkivTjeneste, SifAbacPdpRestKlient sifAbacPdpRestKlient) {
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
        this.sifAbacPdpRestKlient = sifAbacPdpRestKlient;
    }

    @POST
    @Path("/hentPapirSøknad")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(description = "Henter og viser papirsøknad", summary = ("Henter og viser papirsøknad"), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.FAGSAK)
    public Response hentPapirSøknad(@NotNull @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) HentPapirSøknadRequestDto hentPapirSøknadRequestDto) {

        // SafTjeneste gjør tilgangskontroll på journalpostId internt gjennom kall til SAF
        byte[] dokument = dokumentArkivTjeneste.hentDokumnet(hentPapirSøknadRequestDto.journalpostId(), hentPapirSøknadRequestDto.dokumentId());
        String filnavn = "søknadsdokument-" + hentPapirSøknadRequestDto.dokumentId() + ".pdf";

        try {
            return Response.ok(dokument).type("application/pdf").header("Content-Disposition", "inline; filename=\"" + filnavn + "\"").build();
        } catch (Exception e) {
            return Response.serverError().entity("Klarte ikke å generere PDF: " + e.getMessage()).build();
        }
    }

}
