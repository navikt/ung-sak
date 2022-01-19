package no.nav.k9.sak.web.app.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.slf4j.MDC;

import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.sak.kontrakt.FeilDto;
import no.nav.k9.sak.kontrakt.FeilType;

public class ManglerTilgangExceptionMapper implements ExceptionMapper<ManglerTilgangException> {

    @Override
    public Response toResponse(ManglerTilgangException exception) {

        // Logger ikke her, håndteres kun i auditLogg/arcsite
        try {
            String feilmelding = exception.getFeil().getFeilmelding();
            FeilType feilType = FeilType.MANGLER_TILGANG_FEIL;
            return Response.status(Response.Status.FORBIDDEN)
                .entity(new FeilDto(feilType, feilmelding))
                .type(MediaType.APPLICATION_JSON)
                .build();
        } finally {
            // key for å tracke prosess -- nullstill denne
            MDC.remove("prosess"); //$NON-NLS-1$
        }

    }

}
