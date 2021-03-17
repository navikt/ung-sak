package no.nav.k9.sak.web.app.exceptions;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import no.nav.k9.felles.exception.VLException;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FunksjonellFeil;
import no.nav.k9.felles.log.mdc.MDCOperations;
import no.nav.k9.sak.kontrakt.FeilDto;
import no.nav.k9.sak.kontrakt.FeilType;

public class VLExceptionMapper implements ExceptionMapper<VLException> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Response toResponse(VLException exception) {
        String callId = MDCOperations.getCallId();
        exception.log(log);
        Feil feil = exception.getFeil();
        String feilmelding = getVLExceptionFeilmelding(callId, feil);

        try {
            return Response.serverError()
                .entity(new FeilDto(FeilType.GENERELL_FEIL, feilmelding))
                .type(MediaType.APPLICATION_JSON)
                .build();
        } finally {
            // key for å tracke prosess -- nullstill denne
            MDC.remove("prosess");
        }
    }

    private String getVLExceptionFeilmelding(String callId, Feil feil) {
        String feilbeskrivelse = feil.getKode() + ": " + feil.getFeilmelding(); //$NON-NLS-1$
        if (feil instanceof FunksjonellFeil) {
            String løsningsforslag = ((FunksjonellFeil) feil).getLøsningsforslag();
            return "Det oppstod en feil: " //$NON-NLS-1$
                + avsluttMedPunktum(feilbeskrivelse)
                + avsluttMedPunktum(løsningsforslag)
                + ". Referanse-id: " + callId; //$NON-NLS-1$
        } else {
            return "Det oppstod en serverfeil: " //$NON-NLS-1$
                + avsluttMedPunktum(feilbeskrivelse)
                + ". Meld til support med referanse-id: " + callId; //$NON-NLS-1$
        }
    }

    private String avsluttMedPunktum(String tekst) {
        return tekst + (tekst.endsWith(".") ? " " : ". "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

}
