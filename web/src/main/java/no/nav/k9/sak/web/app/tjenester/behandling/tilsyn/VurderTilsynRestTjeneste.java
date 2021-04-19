package no.nav.k9.sak.web.app.tjenester.behandling.tilsyn;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.tilsyn.EtablertTilsynNattevåkOgBeredskapDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(VurderTilsynRestTjeneste.BASEPATH)
@Transactional
public class VurderTilsynRestTjeneste {

    static final String BASEPATH = "/behandling/tilsyn";
    private static final String NATTEVÅK_PATH = BASEPATH + "/nattevak";
    private static final String BEREDSKAP_PATH = BASEPATH + "/beredskap";

    VurderTilsynRestTjeneste() {
        // for CDI proxy
    }


    @GET
    @Operation(description = "Hent etablert tilsyn perioder",
        summary = "Returnerer alle perioder med etablert tilsyn",
        tags="tilsyn",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "perioder med etablert tilsyn, nattevåk og beredskap",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = EtablertTilsynNattevåkOgBeredskapDto.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public EtablertTilsynNattevåkOgBeredskapDto hentEtablertTilsyn(@NotNull @QueryParam(BehandlingUuidDto.NAME)
                                                @Parameter(description = BehandlingUuidDto.DESC)
                                                @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
                                                    BehandlingUuidDto behandlingUuid) {
        //TODO: implementer dette
        return null;
    }



}
