package no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsgiverOversiktDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path("")
@Transactional
public class ArbeidsgiverRestTjeneste {

    public static final String ARBEIDSGIVER_PATH = "/behandling/arbeidsgiver";

    private ArbeidsgiverOversiktTjeneste arbeidsgiverOversiktTjeneste;

    public ArbeidsgiverRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public ArbeidsgiverRestTjeneste(ArbeidsgiverOversiktTjeneste arbeidsgiverOversiktTjeneste) {
        this.arbeidsgiverOversiktTjeneste = arbeidsgiverOversiktTjeneste;
    }

    @GET
    @Path(ARBEIDSGIVER_PATH)
    @Operation(description = "Henter informasjon om alle arbeidsgivere knyttet til bruker",
        summary = ("Henter informasjon om alle arbeidsgivere (navn, fødselsnr for privat arbeidsgiver osv)"),
        tags = "arbeidsgiver",
        responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer ArbeidsgiverOversiktDto",
                content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ArbeidsgiverOversiktDto.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public ArbeidsgiverOversiktDto getArbeidsgiverOpplysninger(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) @Valid BehandlingUuidDto uuidDto) {
        UUID behandlingUuid = uuidDto.getBehandlingUuid();
        return arbeidsgiverOversiktTjeneste.getArbeidsgiverOpplysninger(behandlingUuid);
    }

}
