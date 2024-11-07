package no.nav.k9.sak.web.app.exceptions;

import java.net.URI;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.owasp.encoder.Encode;

import no.nav.k9.felles.sikkerhet.ContextPathHolder;
import no.nav.k9.sak.kontrakt.FeilDto;

public class RedirectExceptionMapper implements ExceptionMapper<Throwable> {

    private String loadBalancerUrl = System.getProperty("loadbalancer.url");

    private KnownExceptionMappers exceptionMappers = new KnownExceptionMappers();

    @SuppressWarnings({ "resource", "unchecked" })
    @Override
    public Response toResponse(Throwable exception) {

        @SuppressWarnings("rawtypes")
        ExceptionMapper exceptionMapper = exceptionMappers.getMapper(exception);
        var response = exceptionMapper.toResponse(exception);
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
