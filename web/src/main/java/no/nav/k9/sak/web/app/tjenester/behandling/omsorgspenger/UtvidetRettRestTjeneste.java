package no.nav.k9.sak.web.app.tjenester.behandling.omsorgspenger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.aarskvantum.kontrakter.RammevedtakResponse;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk.KroniskSykRammevedtakTjeneste;
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

@Path(UtvidetRettRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Transactional
@ApplicationScoped
public class UtvidetRettRestTjeneste {
    static final String BASE_PATH = "/behandling/utvidetRett";
    private static final String RAMMEVEDTAK_PATH = "/rammevedtak";

    public static final String RAMMEVEDTAK = BASE_PATH + RAMMEVEDTAK_PATH;

    private KroniskSykRammevedtakTjeneste kroniskSykRammevedtakTjeneste;

    public UtvidetRettRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public UtvidetRettRestTjeneste(KroniskSykRammevedtakTjeneste kroniskSykRammevedtakTjeneste) {
        this.kroniskSykRammevedtakTjeneste = kroniskSykRammevedtakTjeneste;
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(RAMMEVEDTAK_PATH)
    @Operation(description = "Hent rammevedtak", tags = "behandling - utvidet rett", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer relevante rammevedtak for en behandling om utvidet rett", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RammevedtakResponse.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public RammevedtakResponse hentRammevedtak(
                @QueryParam(BehandlingUuidDto.NAME)
                @Parameter(description = BehandlingUuidDto.DESC)
                @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
                @NotNull
                @Valid
                BehandlingUuidDto behandlingUuid) {
        return kroniskSykRammevedtakTjeneste.hentRammevedtak(behandlingUuid);
    }
}
