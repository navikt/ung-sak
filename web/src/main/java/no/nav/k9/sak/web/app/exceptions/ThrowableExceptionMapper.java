package no.nav.k9.sak.web.app.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import no.nav.k9.felles.log.mdc.MDCOperations;
import no.nav.k9.felles.log.util.LoggerUtils;
import no.nav.k9.sak.kontrakt.FeilDto;
import no.nav.k9.sak.kontrakt.FeilType;

public class ThrowableExceptionMapper implements ExceptionMapper<Throwable> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Response toResponse(Throwable exception) {
        String message = exception.getMessage() != null ? LoggerUtils.removeLineBreaks(exception.getMessage()) : "";
        log.error("Fikk uventet feil: " + message, exception); // NOSONAR //$NON-NLS-1$
        String callId = MDCOperations.getCallId();
        String generellFeilmelding = "Det oppstod en serverfeil: " + exception.getMessage() + ". Meld til support med referanse-id: " + callId; //$NON-NLS-1$ //$NON-NLS-2$
        try {
            return Response.serverError()
                .entity(new FeilDto(FeilType.GENERELL_FEIL, generellFeilmelding))
                .type(MediaType.APPLICATION_JSON)
                .build();
        } finally {
            // key for å tracke prosess -- nullstill denne
            MDC.remove("prosess"); //$NON-NLS-1$
        }
    }

}
