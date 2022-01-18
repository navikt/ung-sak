package no.nav.k9.sak.web.app.exceptions;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import no.nav.k9.felles.jpa.TomtResultatException;
import no.nav.k9.felles.log.util.LoggerUtils;
import no.nav.k9.sak.kontrakt.FeilDto;
import no.nav.k9.sak.kontrakt.FeilType;

public class TomtResultatExceptionMapper implements ExceptionMapper<TomtResultatException> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Response toResponse(TomtResultatException exception) {

        String message = exception.getMessage() != null ? LoggerUtils.removeLineBreaks(exception.getMessage()) : "";
        log.info("Fikk ikke-implementert-feil: " + message, exception); // NOSONAR //$NON-NLS-1$
        // key for å tracke prosess -- nullstill denne

        try {
            return Response
                .status(Response.Status.NOT_FOUND)
                .entity(new FeilDto(FeilType.TOMT_RESULTAT_FEIL, exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
        } finally {
            MDC.remove("prosess"); //$NON-NLS-1$
        }

    }

}
