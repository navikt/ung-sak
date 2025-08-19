package no.nav.ung.sak.web.app.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import no.nav.ung.sak.web.server.abac.UkjentAbacVerdiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class UkjentAbacVerdiExceptionMapper implements ExceptionMapper<UkjentAbacVerdiException> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Response toResponse(UkjentAbacVerdiException exception) {
        String feilmelding = exception.getMessage();
        log.warn(feilmelding, exception);
        try {
            return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .entity(exception.getMessage())
                .build();
        } finally {
            // key for å tracke prosess -- nullstill denne
            MDC.remove("prosess");
        }
    }

}
