package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

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
    public SakResponse getSaker(InfotrygdPårørendeSykdomRequest request) {
        String json = getJson("/saker", request);
        return jsonConverter.sakResponse(json);
    }

    @Override
    public List<PårørendeSykdom> getGrunnlagForPleietrengende(InfotrygdPårørendeSykdomRequest request) {
        String json = getJson("/paaroerendeSykdom/grunnlag", request);
        return jsonConverter.grunnlagBarnResponse(json);
    }


    @Override
    public List<VedtakPleietrengende> getVedtakForPleietrengende(InfotrygdPårørendeSykdomRequest request) {
        String json = getJson("/vedtakForPleietrengende", request);
        return jsonConverter.vedtakBarnResponse(json);
    }

    @SuppressWarnings("resource")
    private String getJson(String path, InfotrygdPårørendeSykdomRequest request) {
        requireNonNull(request.getFødselsnummer(), "fødselsnummer");
        requireNonNull(request.getFraOgMed(), "fraOgMed");
        // tilOgMed er 'optional'

        DateTimeFormatter format = DateTimeFormatter.ISO_LOCAL_DATE;
        Map<String, String> params = new HashMap<>();
        params.put("fnr", request.getFødselsnummer());
        params.put("fom", request.getFraOgMed().format(format));
        if (request.getTilOgMed() != null) {
            params.put("tom", request.getTilOgMed().format(format));
        }

        String uri = baseUri.toString() + path + "?" + toQueryString(params);

        var httpGet = new HttpGet(URI.create(uri));
        httpGet.addHeader(HttpHeaders.ACCEPT, "application/json");

        CloseableHttpResponse response;
        try {
            response = httpClient.execute(httpGet);
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
    }

    String toQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() != 0) {
                sb.append('&');
            }
            sb.append(entry.getKey());
            sb.append('=');
            sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
