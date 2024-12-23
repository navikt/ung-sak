package no.nav.ung.sak.web.app.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import no.nav.k9.felles.exception.VLException;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FunksjonellFeil;
import no.nav.k9.felles.log.mdc.MDCOperations;
import no.nav.ung.sak.kontrakt.FeilDto;
import no.nav.ung.sak.kontrakt.FeilType;

public class VLExceptionMapper implements ExceptionMapper<VLException> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Response toResponse(VLException exception) {
        String callId = MDCOperations.getCallId();

        Feil feil = exception.getFeil();
        exception.log(log);

        String feilmelding = getVLExceptionFeilmelding(callId, feil);
        try {
            return Response.serverError()
                .entity(new FeilDto(FeilType.GENERELL_FEIL, feilmelding, feil.getKode()))
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
        return (tekst == null ? "" : tekst + (tekst.endsWith(".") ? " " : ". "));
    }

}
