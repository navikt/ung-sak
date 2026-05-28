package no.nav.ung.sak.web.app.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import no.nav.k9.felles.sikkerhet.abac.PepNektetTilgangException;
import no.nav.k9.felles.sikkerhet.abac.ÅrsakIkkeTilgang;
import org.slf4j.MDC;

import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.ung.sak.kontrakt.FeilDto;
import no.nav.ung.sak.kontrakt.FeilType;

import java.util.Set;

public class ManglerTilgangExceptionMapper implements ExceptionMapper<ManglerTilgangException> {

    @Override
    public Response toResponse(ManglerTilgangException exception) {

        // Logger ikke her, håndteres kun i auditLogg/arcsite

        Set<ÅrsakIkkeTilgang> årsaker;
        if (exception instanceof PepNektetTilgangException pepNektetTilgangException){
            årsaker = pepNektetTilgangException.getÅrsaker();
        } else {
            årsaker = null;
        }
        String feilmelding = exception.getFeil().getFeilmelding();
        FeilType feilType = FeilType.MANGLER_TILGANG_FEIL;
        return Response.status(Response.Status.FORBIDDEN)
            .entity(new FeilDto(feilType, feilmelding, årsaker))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

}
