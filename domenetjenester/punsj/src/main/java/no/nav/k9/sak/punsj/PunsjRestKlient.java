package no.nav.k9.sak.punsj;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
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

import no.nav.k9.felles.integrasjon.rest.OidcRestClientResponseHandler.ObjectReaderResponseHandler;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.kontrakt.dokument.JournalpostIderDto;
import no.nav.k9.sak.typer.AktørId;


@ApplicationScoped
public class PunsjRestKlient {

    private static final Logger log = LoggerFactory.getLogger(PunsjRestKlient.class);
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
    private final ObjectReader journalpostIderReader = objectMapper.readerFor(JournalpostIderDto.class);
    private CloseableHttpClient restClient;
    private URI punsjEndpoint;

    protected PunsjRestKlient() {
        // cdi
    }

    public PunsjRestKlient(SystemUserOidcRestClient restClient,
                           @KonfigVerdi(value = "k9.punsj.url") URI endpoint) {
        this(endpoint);
        this.restClient = restClient;
    }

    private PunsjRestKlient(URI punsjEndpoint) {
        this.punsjEndpoint = punsjEndpoint;
    }


    public Optional<JournalpostIderDto> getUferdigJournalpostIderPåAktør(AktørId aktørId) {
        Objects.requireNonNull(aktørId);
        URIBuilder builder = new URIBuilder();
        builder.setPathSegments(punsjEndpoint.getPath(), "/journalpost", "/uferdig/", "/" + aktørId.getAktørId());

        try {
            HttpGet kall = new HttpGet(builder.build());
            try (var httpResponse = restClient.execute(kall)) {
                int responseCode = httpResponse.getStatusLine().getStatusCode();
                if (isOk(responseCode)) {
                    ObjectReaderResponseHandler<Object> handler = new ObjectReaderResponseHandler<>(kall.getURI(), journalpostIderReader);
                    return Optional.of((JournalpostIderDto) handler.handleResponse(httpResponse));
                } else {
                    log.info("Fikk ikke hentet informasjon fra k9-punsj - responseCode=" + responseCode);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private boolean isOk(int responseCode) {
        return responseCode == HttpStatus.SC_OK
            || responseCode == HttpStatus.SC_CREATED;
    }
}
