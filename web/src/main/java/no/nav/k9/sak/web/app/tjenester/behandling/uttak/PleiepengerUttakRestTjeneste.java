package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.UttakRestKlient;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class PleiepengerUttakRestTjeneste {

    static final String GET_UTTAKSPLAN_PATH = "/behandling/pleiepenger/uttak";

    private UttakRestKlient uttakRestKlient;

    public PleiepengerUttakRestTjeneste() {
        // for proxying
    }

    @Inject
    public PleiepengerUttakRestTjeneste(UttakRestKlient uttakRestKlient) {
        this.uttakRestKlient = uttakRestKlient;
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(GET_UTTAKSPLAN_PATH)
    @Operation(description = "Hent uttaksplan for behandling", tags = "behandling - pleiepenger/uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer uttaksplan for angitt behandling", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Uttaksplan.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Uttaksplan getUttaksplan(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        return uttakRestKlient.hentUttaksplan(behandlingIdDto.getBehandlingUuid());
    }

}
