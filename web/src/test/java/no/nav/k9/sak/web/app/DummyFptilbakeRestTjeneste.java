package no.nav.k9.sak.web.app;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;

/**
 * DummyRestTjeneste returnerer alltid tomt resultat. Klienten for tilbakekreving krever at det retureres en verdi,
 * derfor kan ikke DummyRestTjeneste benyttes
 */
@Path("/dummy-fptilbake")
public class DummyFptilbakeRestTjeneste {

    @GET
    @Operation(description = "Dummy-sjekk for om det finnes en tilbakekrevingsbehandling", hidden = true)
    @Path("/behandlinger/tilbakekreving/aapen")
    public Response harÅpenTilbakekrevingBehandling(@SuppressWarnings("unused") @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        return Response.ok().entity(false).build();
    }
}
