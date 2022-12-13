package no.nav.k9.sak.web.app.tjenester.register;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectReader;

import no.nav.k9.felles.exception.IntegrasjonException;
import no.nav.k9.felles.exception.ManglerTilgangException;

public abstract class DebugOidcRestClientResponseHandler<T> implements ResponseHandler<T> {

    private static final Logger logger = LoggerFactory.getLogger(DebugOidcRestClientResponseHandler.class);


    private final URI endpoint;

    public DebugOidcRestClientResponseHandler(URI endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public T handleResponse(final HttpResponse response) throws IOException {
        int status = response.getStatusLine().getStatusCode();
        if (status == HttpStatus.SC_NO_CONTENT) {
            return null;
        }
        if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES) {
            HttpEntity entity = response.getEntity();
            return entity != null ? readEntity(entity) : null;
        }
        if (status == HttpStatus.SC_FORBIDDEN) {
            throw new ManglerTilgangException("F-468815", "Feilet mot " + endpoint);
        }

        if (status == HttpStatus.SC_UNAUTHORIZED) {
            var challenge = response.getHeaders("WWW-Authenticate");
            logger.info(Arrays.toString(challenge));
        }

        // håndter andre feil
        throw new IntegrasjonException("F-468815", String.format("Uventet respons %s fra %s: %s", status, endpoint,
            response.getStatusLine().getReasonPhrase()));
    }

    protected abstract T readEntity(HttpEntity entity) throws IOException;

    public static class StringResponseHandler extends DebugOidcRestClientResponseHandler<String> {
        public StringResponseHandler(URI endpoint) {
            super(endpoint);
        }

        /**
         * default håndteres alt som string.
         */
        @Override
        protected String readEntity(HttpEntity entity) throws IOException {
            return EntityUtils.toString(entity, StandardCharsets.UTF_8);
        }
    }

}
