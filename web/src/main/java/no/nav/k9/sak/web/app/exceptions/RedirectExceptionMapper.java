package no.nav.k9.sak.web.app.exceptions;

import java.net.URI;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.owasp.encoder.Encode;

import no.nav.k9.felles.sikkerhet.ContextPathHolder;
import no.nav.k9.sak.kontrakt.FeilDto;

@Provider
public class RedirectExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private String loadBalancerUrl = System.getProperty("loadbalancer.url");

    private GeneralRestExceptionMapper generalRestExceptionMapper = new GeneralRestExceptionMapper();

    @SuppressWarnings("resource")
    @Override
    public Response toResponse(WebApplicationException exception) {
        Response response = generalRestExceptionMapper.toResponse(exception);
        String feilmelding = ((FeilDto) response.getEntity()).getFeilmelding();
        String enkodetFeilmelding = Encode.forUriComponent(feilmelding);

        String formattertFeilmelding = String.format("%s/#?errorcode=%s", getBaseUrl(), enkodetFeilmelding);//$NON-NLS-1$
        Response.ResponseBuilder responser = Response.temporaryRedirect(URI.create(formattertFeilmelding));
        responser.encoding("UTF-8");
        return responser.build();
    }

    protected String getBaseUrl() {
        return loadBalancerUrl + ContextPathHolder.instance().getContextPath();
    }

}
