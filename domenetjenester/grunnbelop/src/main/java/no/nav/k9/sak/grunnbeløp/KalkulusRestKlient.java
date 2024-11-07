package no.nav.k9.sak.grunnbeløp;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.kalkulus.mappers.JsonMapper;
import no.nav.folketrygdloven.kalkulus.request.v1.HentGrunnbeløpRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.Grunnbeløp;
import no.nav.k9.felles.exception.VLException;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.OidcRestClientResponseHandler;
import no.nav.k9.felles.integrasjon.rest.OidcRestClientResponseHandler.ObjectReaderResponseHandler;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;


@ApplicationScoped
@ScopedRestIntegration(scopeKey = "ftkalkulus.scope", defaultScope = "api://prod-fss.k9saksbehandling.ftkalkulus/.default")
public class KalkulusRestKlient {

    private static final Logger log = LoggerFactory.getLogger(KalkulusRestKlient.class);
    private static final ObjectMapper kalkulusMapper = JsonMapper.getMapper();
    private final ObjectWriter kalkulusJsonWriter = kalkulusMapper.writerWithDefaultPrettyPrinter();
    private final ObjectReader grunnbeløpReader = kalkulusMapper.readerFor(Grunnbeløp.class);


    private CloseableHttpClient restClient;
    private URI kalkulusEndpoint;
    private URI grunnbeløp;


    public KalkulusRestKlient() {
        // cdi
    }

    @Inject
    public KalkulusRestKlient(OidcRestClient restClient,
                              @KonfigVerdi(value = "ftkalkulus.url") URI endpoint) {
        this(endpoint);
        this.restClient = restClient;
    }

    public KalkulusRestKlient(SystemUserOidcRestClient restClient,
                              URI endpoint) {
        this(endpoint);
        this.restClient = restClient;
    }

    private KalkulusRestKlient(URI endpoint) {
        this.kalkulusEndpoint = endpoint;
        this.grunnbeløp = toUri("/api/kalkulus/v1/grunnbelop");
    }


    public Grunnbeløp hentGrunnbeløp(HentGrunnbeløpRequest request) {
        var endpoint = grunnbeløp;

        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request), grunnbeløpReader);
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }


    private <T> T getResponse(URI endpoint, String json, ObjectReader reader) {
        try {
            return utførOgHent(endpoint, json, new ObjectReaderResponseHandler<>(endpoint, reader));
        } catch (IOException e) {
            throw RestTjenesteFeil.FEIL.feilVedKallTilKalkulus(endpoint, e.getMessage()).toException();
        }
    }

    private <T> T utførOgHent(URI endpoint, String json, OidcRestClientResponseHandler<T> responseHandler) throws IOException {
        var httpPost = new HttpPost(endpoint); // NOSONAR håndterer i responseHandler
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        try (var httpResponse = restClient.execute(httpPost)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (isOk(responseCode)) {
                return responseHandler.handleResponse(httpResponse);
            } else {
                if (responseCode == HttpStatus.SC_NOT_MODIFIED) {
                    return null;
                }
                if (responseCode == HttpStatus.SC_NO_CONTENT) {
                    return null;
                }
                if (responseCode == HttpStatus.SC_ACCEPTED) {
                    return null;
                }
                String responseBody = EntityUtils.toString(httpResponse.getEntity());
                String feilmelding = "Kunne ikke hente utføre kall til kalkulus,"
                    + " endpoint=" + httpPost.getURI()
                    + ", HTTP status=" + httpResponse.getStatusLine()
                    + ". HTTP Errormessage=" + responseBody;
                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw RestTjenesteFeil.FEIL.feilKallTilKalkulus(endpoint, feilmelding).toException();
                } else {
                    throw RestTjenesteFeil.FEIL.feilVedKallTilKalkulus(endpoint, feilmelding).toException();
                }
            }
        } catch (VLException e) {
            throw e; // retrhow
        } catch (RuntimeException re) {
            log.warn("Feil ved henting av data. uri=" + endpoint, re);
            throw re;
        }
    }

    private boolean isOk(int responseCode) {
        return responseCode == HttpStatus.SC_OK
            || responseCode == HttpStatus.SC_CREATED;
    }

    private URI toUri(String relativeUri) {
        String uri = kalkulusEndpoint.toString() + relativeUri;
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig uri: " + uri, e);
        }
    }

    interface RestTjenesteFeil extends DeklarerteFeil {
        KalkulusRestKlient.RestTjenesteFeil FEIL = FeilFactory.create(KalkulusRestKlient.RestTjenesteFeil.class);

        @TekniskFeil(feilkode = "F-FT-K-1000001", feilmelding = "Feil ved kall til Kalkulus [%s]: %s", logLevel = LogLevel.ERROR)
        Feil feilVedKallTilKalkulus(URI endpoint, String feilmelding);

        @TekniskFeil(feilkode = "F-FT-K-1000002", feilmelding = "Feil ved kall til Kalkulus [%s]: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilKalkulus(URI endpoint, String feilmelding);

        @TekniskFeil(feilkode = "F-FT-K-1000003", feilmelding = "Feil ved kall til Kalkulus: %s", logLevel = LogLevel.WARN)
        Feil feilVedJsonParsing(String feilmelding);
    }
}
