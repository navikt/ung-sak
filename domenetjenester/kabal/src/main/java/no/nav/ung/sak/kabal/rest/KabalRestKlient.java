package no.nav.ung.sak.kabal.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.kabal.kontrakt.KabalRequest;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;


@ApplicationScoped
@ScopedRestIntegration(scopeKey = "azure.scope.kabal", defaultScope = "api://prod-gcp.klage.kabal-api/.default")
public class KabalRestKlient {

    private static final Logger log = LoggerFactory.getLogger(KabalRestKlient.class);

    static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule())
        .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
    private OidcRestClient oidcRestClient;

    private URI endpointKabalApi;

    protected KabalRestKlient() {
        // for proxying
    }

    @Inject
    public KabalRestKlient(
        @KonfigVerdi(value = "kabal.url", defaultVerdi = "http://kabal-api") URI kabalEndpoint,
        OidcRestClient oidcRestClient) {

        // https://github.com/navikt/kabal-api/tree/main/docs/integrasjon
        this.endpointKabalApi = toUri(kabalEndpoint, "/api/oversendelse/v2/klage");
        this.oidcRestClient = oidcRestClient;
    }

    public void overførKlagebehandling(KabalRequest request) {
        URIBuilder builder = new URIBuilder(endpointKabalApi);
        try {
            HttpPost kall = new HttpPost(builder.build());
            var json = objectMapper.writer().writeValueAsString(request);
            kall.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            post(kall);
        } catch (IOException | URISyntaxException e) {
            throw RestTjenesteFeil.FEIL.feilKallTilKabal(request.getBehandlingUuid(), e).toException();
        }
    }

    private void post(HttpUriRequest request) throws IOException {
        try (var httpResponse = oidcRestClient.execute(request)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode == HttpStatus.SC_OK || responseCode == HttpStatus.SC_CREATED) {
                return;
            } else if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                throw RestTjenesteFeil.FEIL.feilKallTilKabal(EntityUtils.toString(httpResponse.getEntity())).toException();
            } else {
                throw RestTjenesteFeil.FEIL.feilVedKallTilKabal(EntityUtils.toString(httpResponse.getEntity())).toException();
            }
        } catch (RuntimeException re) {
            throw re;
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

    interface RestTjenesteFeil extends DeklarerteFeil {
        static final RestTjenesteFeil FEIL = FeilFactory.create(RestTjenesteFeil.class);

        @TekniskFeil(feilkode = "K9KLAGE-KABAL-1000011", feilmelding = "Feil ved kall til Kabal: %s", logLevel = LogLevel.ERROR)
        Feil feilVedKallTilKabal(String feilmelding);

        @TekniskFeil(feilkode = "K9KLAGE-KABAL-1000012", feilmelding = "Feil ved kall til Kabal: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilKabal(String feilmelding);

        @TekniskFeil(feilkode = "K9KLAGE-KABAL-1000014", feilmelding = "Feil ved kall til Kabal: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilKabal(UUID behandlingUuid, Throwable t);
    }

}
