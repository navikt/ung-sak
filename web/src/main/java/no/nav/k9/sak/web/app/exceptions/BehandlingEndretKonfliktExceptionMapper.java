package no.nav.k9.sak.web.app.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.sak.kontrakt.FeilDto;
import no.nav.k9.sak.kontrakt.FeilType;
import no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt.BehandlingEndretKonfliktException;

public class BehandlingEndretKonfliktExceptionMapper implements ExceptionMapper<BehandlingEndretKonfliktException> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Response toResponse(BehandlingEndretKonfliktException exception) {

        exception.log(log);
        // key for å tracke prosess -- nullstill denne
        MDC.remove("prosess"); //$NON-NLS-1$

        return behandlingEndret(exception.getFeil());

    }

    private Response behandlingEndret(Feil feil) {
        String feilmelding = feil.getFeilmelding();
        FeilType feilType = FeilType.BEHANDLING_ENDRET_FEIL;
        return Response.status(Response.Status.CONFLICT)
            .entity(new FeilDto(feilType, feilmelding))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

}
