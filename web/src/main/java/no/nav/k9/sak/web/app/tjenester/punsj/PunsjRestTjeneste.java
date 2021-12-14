package no.nav.k9.sak.web.app.tjenester.punsj;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.kontrakt.FeilDto;
import no.nav.k9.sak.kontrakt.FeilType;
import no.nav.k9.sak.kontrakt.dokument.JournalpostIderDto;
import no.nav.k9.sak.punsj.PunsjRestKlient;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@ApplicationScoped
@Transactional
@Path("/punsj")
@Produces(MediaType.APPLICATION_JSON)
public class PunsjRestTjeneste {

    private PunsjRestKlient klient;

    public PunsjRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public PunsjRestTjeneste(PunsjRestKlient klient) {
        this.klient = klient;
    }

    @POST
    @Operation(description = "Henter uferdig journalposter fra punsj for en gitt aktørId", tags = "aktoer", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer en liste med uferdig journalpostIder som ligger i punsj på gitt aktørId.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JournalpostIderDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path("/journalpost/uferdig")
    @Consumes(MediaType.APPLICATION_JSON)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getUferdigJournalpostIderPrAktoer(@Parameter(description = "Inneholder aktørIder, der det blir søket etter uferdig journalposter i punsj") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) UferdigJournalpostSøkDto dto) {
        if (dto.getAktorIdDto() != null && dto.getAktorIdDto().getAktorId() != null) {
            Optional<JournalpostIderDto> uferdigJournalpostIderPåAktør = klient.getUferdigJournalpostIderPåAktør(dto.getAktorIdDto().getAktorId(), dto.getAktorIdDtoBarn() != null ? dto.getAktorIdDtoBarn().getAktorId() : null);
            if (uferdigJournalpostIderPåAktør.isPresent()) {
                return Response.ok(uferdigJournalpostIderPåAktør.get()).build();
            }
            return Response.ok().build();
        } else {
            FeilDto feilDto = new FeilDto(FeilType.GENERELL_FEIL, "'aktoerId' mangler i bodyen.");
            return Response.ok(feilDto).status(400).build();
        }
    }
}
