package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.k9.sak.typer.Saksnummer;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClientResponseHandler;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClientResponseHandler.ObjectReaderResponseHandler;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class UttakRestKlient {
    private static final String TJENESTE_NAVN = "pleiepenger-barn-uttak";

    private static final Logger log = LoggerFactory.getLogger(UttakRestKlient.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
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

    private ObjectReader uttaksplanReader = objectMapper.readerFor(Uttaksplan.class);

    private OidcRestClient restKlient;
    private URI endpointUttaksplan;

    protected UttakRestKlient() {
        // for proxying
    }

    @Inject
    public UttakRestKlient(OidcRestClient restKlient, @KonfigVerdi(value = "k9.psb.uttak.url") URI endpoint) {
        this.restKlient = restKlient;
        this.endpointUttaksplan = toUri(endpoint, "/uttaksplan");
    }

    public Uttaksplan opprettUttaksplan(Uttaksgrunnlag request) {
        URIBuilder builder = new URIBuilder(endpointUttaksplan);
        try {
            HttpPost kall = new HttpPost(builder.build());
            var json = objectMapper.writer().writeValueAsString(request);
            
             // FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN
             // FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN
             // FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN
             // FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN
             // FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN
             // FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN
             // FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN
             // FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN
             // FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN FJERN
            log.info(json);
            kall.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            return utførOgHent(kall, json, new ObjectReaderResponseHandler<>(endpointUttaksplan, uttaksplanReader));
        } catch (IOException | URISyntaxException e) {
            throw RestTjenesteFeil.FEIL.feilKallTilUttak(UUID.fromString(request.getBehandlingUUID()), e).toException();
        }
    }

    public Uttaksplan hentUttaksplan(UUID behandlingUuid) {
        Objects.requireNonNull(behandlingUuid);
        URIBuilder builder = new URIBuilder(endpointUttaksplan);
        builder.addParameter("behandlingUUID", behandlingUuid.toString());
        try {
            HttpGet kall = new HttpGet(builder.build());
            return utførOgHent(kall, null, new ObjectReaderResponseHandler<>(endpointUttaksplan, uttaksplanReader));
        } catch (IOException | URISyntaxException e) {
            throw RestTjenesteFeil.FEIL.feilKallTilUttak(behandlingUuid, e).toException();
        }
    }

    private <T> T utførOgHent(HttpUriRequest request, @SuppressWarnings("unused") String jsonInput, OidcRestClientResponseHandler<T> responseHandler) throws IOException {
        try (var httpResponse = restKlient.execute(request)) {
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
                String feilmelding = "Kunne ikke hente utføre kall til "
                    + TJENESTE_NAVN
                    + ", HTTP status=" + httpResponse.getStatusLine()
                    + ". HTTP Errormessage=" + responseBody;
                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw RestTjenesteFeil.FEIL.feilKallTilUttak(feilmelding).toException();
                } else {
                    throw RestTjenesteFeil.FEIL.feilVedKallTilUttak(feilmelding).toException();
                }
            }
        } catch (RuntimeException re) {
            log.warn("Feil ved henting av data. uri=" + request.getURI(), re);
            throw re;
        }
    }

    private boolean isOk(int responseCode) {
        return responseCode == HttpStatus.SC_OK
            || responseCode == HttpStatus.SC_CREATED;
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

        @TekniskFeil(feilkode = "K9SAK-UT-1000011", feilmelding = "Feil ved kall til K9Uttak: %s", logLevel = LogLevel.ERROR)
        Feil feilVedKallTilUttak(String feilmelding);

        @TekniskFeil(feilkode = "K9SAK-UT-1000012", feilmelding = "Feil ved kall til K9Uttak: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilUttak(String feilmelding);

        @TekniskFeil(feilkode = "K9SAK-UT-1000013", feilmelding = "Feil ved kall til K9Uttak: %s", logLevel = LogLevel.WARN)
        Feil feilVedJsonParsing(String feilmelding);

        @TekniskFeil(feilkode = "K9SAK-UT-1000014", feilmelding = "Feil ved kall til K9Uttak: Kunne ikke hente uttaksplan for behandling: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilUttak(UUID behandlingUuid, Throwable t);

        @TekniskFeil(feilkode = "K9SAK-UT-1000015", feilmelding = "Feil ved kall til K9Uttak: Kunne ikke hente uttaksplaner for behandlinger: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilUttakForPlaner(Collection<UUID> behandlingUuid, Throwable t);

        @TekniskFeil(feilkode = "K9SAK-UT-1000016", feilmelding = "Feil ved kall til K9Uttak: Kunne ikke hente uttaksplaner for saker: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilUttakForPlanerForSaker(Collection<Saksnummer> saksnummere, Throwable t);
    }

}
