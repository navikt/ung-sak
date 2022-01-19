package no.nav.k9.sak.punsj;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.kontrakt.dokument.JournalpostIderDto;


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

    @Inject
    public PunsjRestKlient(SystemUserOidcRestClient restClient,
                           @KonfigVerdi(value = "k9.punsj.url") URI endpoint) {
        this(endpoint);
        this.restClient = restClient;
    }

    private PunsjRestKlient(URI punsjEndpoint) {
        this.punsjEndpoint = punsjEndpoint;
    }


    public Optional<JournalpostIderDto> getUferdigJournalpostIderPåAktør(String aktørId, String aktørIdBarn) {
        Objects.requireNonNull(aktørId);
        URIBuilder builder = new URIBuilder(toUri(punsjEndpoint, "/journalpost/uferdig"));

        try {
            SøkDto søk = new SøkDto(aktørId, aktørIdBarn);
            String json = JsonObjectMapper.getJson(søk);
            HttpPost httpPost = new HttpPost(builder.build());
            httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            httpPost.setHeader(HttpHeaders.ACCEPT, "application/json");

            try (var httpResponse = restClient.execute(httpPost)) {
                int responseCode = httpResponse.getStatusLine().getStatusCode();
                if (isOk(responseCode)) {
                    ObjectReaderResponseHandler<Object> handler = new ObjectReaderResponseHandler<>(httpPost.getURI(), journalpostIderReader);
                    return Optional.of((JournalpostIderDto) handler.handleResponse(httpResponse));
                } else {
                    log.info("Fikk ikke hentet informasjon fra k9-punsj - responseCode=" + responseCode);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    @JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
    @JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_ARBEIDSFORHOLD_KODE)
    static class SøkDto {
        @JsonProperty(value = "aktorIdentDto", required = true)
        @Valid
        @NotNull
        private String aktorIdentDto;

        @JsonProperty(value = "aktorIdentBarnDto")
        @Valid
        private String aktorIdentBarnDto;

        public SøkDto(String aktorIdentDto, String aktorIdentBarnDto) {
            this.aktorIdentDto = aktorIdentDto;
            this.aktorIdentBarnDto = aktorIdentBarnDto;
        }

        public SøkDto() {
        }

        public String getAktorIdentDto() {
            return aktorIdentDto;
        }

        public void setAktorIdentDto(String aktorIdentDto) {
            this.aktorIdentDto = aktorIdentDto;
        }

        public String getAktorIdentBarnDto() {
            return aktorIdentBarnDto;
        }

        public void setAktorIdentBarnDto(String aktorIdentBarnDto) {
            this.aktorIdentBarnDto = aktorIdentBarnDto;
        }
    }

}
