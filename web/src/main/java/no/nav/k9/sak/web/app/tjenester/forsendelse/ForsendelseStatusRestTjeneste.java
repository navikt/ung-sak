package no.nav.k9.sak.web.app.tjenester.forsendelse;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.mottak.forsendelse.tjeneste.ForsendelseStatusTjeneste;
import no.nav.k9.sak.kontrakt.mottak.ForsendelseIdDto;
import no.nav.k9.sak.kontrakt.mottak.ForsendelseStatusData;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("/dokumentforsendelse")
@ApplicationScoped
@Transactional
public class ForsendelseStatusRestTjeneste {

    private ForsendelseStatusTjeneste forsendelseStatusTjeneste;

    public ForsendelseStatusRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public ForsendelseStatusRestTjeneste(ForsendelseStatusTjeneste forsendelseStatusTjeneste) {
        this.forsendelseStatusTjeneste = forsendelseStatusTjeneste;
    }

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Operation(description = "Søker om status på prossesseringen av et mottatt dokument", tags = "dokumentforsendelse", responses = {
            @ApiResponse(responseCode = "200", description = "Status og Periode", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ForsendelseStatusData.class)))
    })
    public ForsendelseStatusData getStatusInformasjon(@NotNull @QueryParam("forsendelseId") @Parameter(description = "forsendelseId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) ForsendelseIdDto forsendelseIdDto) {
        return forsendelseStatusTjeneste.getStatusInformasjon(forsendelseIdDto);
    }
}
