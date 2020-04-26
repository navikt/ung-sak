package no.nav.k9.sak.web.app.tjenester.behandling.årskvantum;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
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
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumForbrukteDager;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class ÅrskvantumRestTjeneste {

    static final String BASE_PATH = "/behandling/aarskvantum";

    public static final String FORBRUKTEDAGER = BASE_PATH + "/forbruktedager";

    private ÅrskvantumTjeneste årskvantumTjeneste;



    public ÅrskvantumRestTjeneste() {
        // for proxying
    }

    @Inject
    public ÅrskvantumRestTjeneste(ÅrskvantumTjeneste årskvantumTjeneste) {
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    /**
     * Hent oppgitt uttak for angitt behandling.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(FORBRUKTEDAGER)
    @Operation(description = "Hent forbrukte dager", tags = "behandling - årskvantum/uttak", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer forbrukte dager av totalt årskvantum", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ÅrskvantumForbrukteDager.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public ÅrskvantumForbrukteDager getOppgittUttak(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        return årskvantumTjeneste.hentÅrskvantumForBehandling(behandlingIdDto.getBehandlingUuid());
    }

}
