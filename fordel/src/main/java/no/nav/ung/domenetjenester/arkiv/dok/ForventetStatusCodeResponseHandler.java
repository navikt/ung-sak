package no.nav.ung.domenetjenester.arkiv.dok;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.util.EntityUtils;

public class ForventetStatusCodeResponseHandler extends BasicResponseHandler {

    private final int forventetStatusCode;

    private ForventetStatusCodeResponseHandler(int forventetStatusCode) {
        this.forventetStatusCode = forventetStatusCode;
    }

    public static ForventetStatusCodeResponseHandler of(int forventetStatusCode) {
        return new ForventetStatusCodeResponseHandler(forventetStatusCode);
    }

    @Override
    public String handleResponse(final HttpResponse response) throws IOException {
        final StatusLine statusLine = response.getStatusLine();
        final HttpEntity entity = response.getEntity();
        if (statusLine.getStatusCode() != forventetStatusCode) {
            final String result = (entity != null) ? EntityUtils.toString(entity) : null;
            throw new HttpResponseException(statusLine.getStatusCode(),
                    statusLine.getReasonPhrase() + ", entity: " + result);
        }
        return entity == null ? null : handleEntity(entity);
    }
}
