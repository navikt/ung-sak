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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

@Path(ÅrskvantumRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Transactional
@ApplicationScoped
public class ÅrskvantumRestTjeneste {

    public static final String FORBRUKTEDAGER_PATH = "forbruktedager";
    public static final String INPUT_PATH = "input";
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
    @Path(FORBRUKTEDAGER_PATH)
    @Operation(description = "Hent forbrukte dager", tags = "behandling - årskvantum/uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer forbrukte dager av totalt årskvantum", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ÅrskvantumForbrukteDager.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public ÅrskvantumForbrukteDager getForbrukteDager(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        return årskvantumTjeneste.hentÅrskvantumForBehandling(behandlingIdDto.getBehandlingUuid());
    }


    /**
     * Hent oppgitt uttak for angitt behandling.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(INPUT_PATH)
    @Operation(description = "Hent input til beregning av årskvantum", tags = "behandling - årskvantum/uttak", responses = {
        @ApiResponse(responseCode = "200", description = "input til beregning av årskvantum", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response inputTilÅrskvantumsBeregning(@NotNull @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {

        var request = årskvantumTjeneste.hentInputTilBeregning(behandlingIdDto.getBehandlingUuid());

        return Response.ok(request).build();
    }
}
