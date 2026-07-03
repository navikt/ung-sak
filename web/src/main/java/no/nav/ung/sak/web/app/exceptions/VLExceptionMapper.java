package no.nav.ung.sak.web.app.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import no.nav.k9.felles.exception.VLException;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FunksjonellFeil;
import no.nav.ung.sak.kontrakt.FeilDto;
import no.nav.ung.sak.kontrakt.FeilType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class VLExceptionMapper implements ExceptionMapper<VLException> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Response toResponse(VLException exception) {
        Feil feil = exception.getFeil();
        exception.log(log);

        String feilmelding = getVLExceptionFeilmelding(feil);
        try {
            return Response.serverError()
                .entity(new FeilDto(FeilType.GENERELL_FEIL, feilmelding, feil.getKode(), null))
                .type(MediaType.APPLICATION_JSON)
                .build();
        } finally {
            // key for å tracke prosess -- nullstill denne
            MDC.remove("prosess");
        }
    }

    private String getVLExceptionFeilmelding(Feil feil) {
        String feilbeskrivelse = feil.getKode() + ": " + feil.getFeilmelding(); //$NON-NLS-1$
        if (feil instanceof FunksjonellFeil) {
            String løsningsforslag = ((FunksjonellFeil) feil).getLøsningsforslag();
            return (avsluttMedPunktum(feilbeskrivelse)
                + avsluttMedPunktum(løsningsforslag)).trim();
        } else {
            return avsluttMedPunktum(feilbeskrivelse).trim();
        }
    }

    private String avsluttMedPunktum(String tekst) {
        return (tekst == null ? "" : tekst + (tekst.endsWith(".") ? " " : ". "));
    }

}
