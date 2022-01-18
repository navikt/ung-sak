package no.nav.k9.sak.web.app.exceptions;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import no.nav.k9.felles.log.mdc.MDCOperations;
import no.nav.k9.felles.log.util.LoggerUtils;
import no.nav.k9.sak.kontrakt.FeilDto;
import no.nav.k9.sak.kontrakt.FeilType;

public class UnsupportedOperationExceptionMapper implements ExceptionMapper<UnsupportedOperationException> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Response toResponse(UnsupportedOperationException exception) {
        String callId = MDCOperations.getCallId();

        String message = exception.getMessage() != null ? LoggerUtils.removeLineBreaks(exception.getMessage()) : "";
        log.info("Fikk ikke-implementert-feil: " + message, exception); // NOSONAR //$NON-NLS-1$
        // key for å tracke prosess -- nullstill denne

        try {
            String feilmelding = "Funksjonalitet er ikke støttet: " + exception.getMessage() + ". Meld til support med referanse-id: " + callId; //$NON-NLS-1$ //$NON-NLS-2$
            FeilType feilType = FeilType.GENERELL_FEIL;
            return Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity(new FeilDto(feilType, feilmelding))
                .type(MediaType.APPLICATION_JSON)
                .build();
        } finally {
            MDC.remove("prosess"); //$NON-NLS-1$
        }

    }

}
