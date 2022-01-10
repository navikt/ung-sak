package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;

@Dependent
class RestInfotrygdPårørendeSykdomClient implements InfotrygdPårørendeSykdomClient {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final JsonConverter jsonConverter = new JsonConverter();
    private CloseableHttpClient httpClient;
    private URI baseUri;

    @Inject
    RestInfotrygdPårørendeSykdomClient(OidcRestClient httpClient, @KonfigVerdi("infotrygd.bs.base.uri") URI baseUri) {
        this.httpClient = httpClient;
        this.baseUri = baseUri;
    }

    @Override
    public List<SakResponse> getSaker(PersonRequest request) {
        String json = getJson("/saker", request);
        return jsonConverter.sakResponse(json);
    }

    @Override
    public List<PårørendeSykdom> getGrunnlagForPleietrengende(PersonRequest request) {
        String json = getJson("/paaroerendeSykdom/grunnlag", request);
        return jsonConverter.grunnlagBarnResponse(json);
    }

    @Override
    public List<VedtakPleietrengende> getVedtakForPleietrengende(PersonRequest request) {
        String json = getJson("/vedtakForPleietrengende", request);
        return jsonConverter.vedtakBarnResponse(json);
    }

    @SuppressWarnings("resource")
    private String getJson(String path, PersonRequest request) {
        requireNonNull(request.fnr(), "fødselsnummer");
        requireNonNull(request.fom(), "fraOgMed");
        // tilOgMed er 'optional'

        try {
            URIBuilder builder = new URIBuilder(toUri(baseUri, "/journalpost/uferdig"));
            var json = JsonObjectMapper.getJson(request);
            HttpPost httpPost = new HttpPost(builder.build());
            httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            httpPost.addHeader(HttpHeaders.ACCEPT, "application/json");
            CloseableHttpResponse response;
            try {
                response = httpClient.execute(httpPost);
                var statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() != 200) {
                    String feilmelding = String.format("Uventet statuskode %s for call mot %s. Feilmelding: %s", statusLine.getStatusCode(),
                        URI.create(baseUri + path).toString(), response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : null);
                    logger.error(feilmelding);
                    throw new InfotrygdPårørendeSykdomException(feilmelding);
                }
                return new BasicResponseHandler().handleResponse(response);
            } catch (IOException e) {
                logger.error("Feil ved oppkobling mot API.", e);
                throw new InfotrygdPårørendeSykdomException("Feil ved oppkobling mot API.", e);
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(String.format("Kunne ikke mappe til request for %s. Feilmelding var %s", path, e.getMessage()));
        }
    }

    private URI toUri(URI baseUri, String relativeUri) {
        String uri = baseUri.toString() + relativeUri;
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig uri: " + uri, e);
        }
    }

}
