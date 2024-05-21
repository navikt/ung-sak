package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding;

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
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusKodelisteSerializer;
import no.nav.folketrygdloven.kalkulus.mappers.JsonMapper;
import no.nav.k9.felles.exception.VLException;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;


@ApplicationScoped
@ScopedRestIntegration(scopeKey = "ftinntektsmelding.scope", defaultScope = "api://prod-fss.k9saksbehandling.ftinntektsmelding/.default")
public class InntektsmeldingRestKlient {

    private static final Logger log = LoggerFactory.getLogger(InntektsmeldingRestKlient.class);
    private static final ObjectMapper ftInntektsmeldingMapper = JsonMapper.getMapper().copy().registerModule(createModuleWithStringSerializer());
    private final ObjectWriter innteksmeldingJsonWriter = ftInntektsmeldingMapper.writerWithDefaultPrettyPrinter();
    private CloseableHttpClient restClient;
    private URI endpoint;
    private URI opprettForespørselEndpoint;


    protected InntektsmeldingRestKlient() {
        // cdi
    }

    @Inject
    public InntektsmeldingRestKlient(OidcRestClient restClient,
                                     @KonfigVerdi(value = "ftinntektsmelding.url") URI endpoint) {
        this(endpoint);
        this.restClient = restClient;
    }

    public InntektsmeldingRestKlient(SystemUserOidcRestClient restClient,
                                     URI endpoint) {
        this(endpoint);
        this.restClient = restClient;
    }

    private InntektsmeldingRestKlient(URI endpoint) {
        this.endpoint = endpoint;
        this.opprettForespørselEndpoint = toUri("/api/foreporsel/opprett");
    }


    public void opprettForespørsel(OpprettForespørselRequest request) {
        var endpoint = opprettForespørselEndpoint;
        try {
            opprett(endpoint, innteksmeldingJsonWriter.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }


    private void opprett(URI endpoint, String json) {
        try {
            utfør(endpoint, json);
        } catch (IOException e) {
            throw RestTjenesteFeil.FEIL.feilVedKallTilInntektsmelding(endpoint, e.getMessage()).toException();
        }
    }

    private void utfør(URI endpoint, String json) throws IOException {
        var httpPost = new HttpPost(endpoint); // NOSONAR håndterer i responseHandler
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        try (var httpResponse = restClient.execute(httpPost)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (!isOk(responseCode)) {
                if (responseCode == HttpStatus.SC_NOT_MODIFIED) {
                    log.warn("Kall for opprettelse opprettet ingen forespørsel");
                } else if (responseCode != HttpStatus.SC_NO_CONTENT && responseCode != HttpStatus.SC_ACCEPTED) {
                    String responseBody = EntityUtils.toString(httpResponse.getEntity());
                    String feilmelding = "Kunne ikke utføre kall til ftinntektsmelding,"
                        + " endpoint=" + httpPost.getURI()
                        + ", HTTP status=" + httpResponse.getStatusLine()
                        + ". HTTP Errormessage=" + responseBody;
                    throw RestTjenesteFeil.FEIL.feilVedKallTilInntektsmelding(endpoint, feilmelding).toException();
                }
            }
        } catch (VLException e) {
            throw e; // rethrow
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
        String uri = endpoint.toString() + relativeUri;
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig uri: " + uri, e);
        }
    }

    interface RestTjenesteFeil extends DeklarerteFeil {
        InntektsmeldingRestKlient.RestTjenesteFeil FEIL = FeilFactory.create(InntektsmeldingRestKlient.RestTjenesteFeil.class);

        @TekniskFeil(feilkode = "F-FT-IM-1000001", feilmelding = "Feil ved kall til ft-inntektsmelding [%s]: %s", logLevel = LogLevel.ERROR)
        Feil feilVedKallTilInntektsmelding(URI endpoint, String feilmelding);


        @TekniskFeil(feilkode = "F-FT-IM-1000002", feilmelding = "Feil ved kall til ft-inntektsmelding: %s", logLevel = LogLevel.WARN)
        Feil feilVedJsonParsing(String feilmelding);
    }

    private static SimpleModule createModuleWithStringSerializer() {
        SimpleModule module = new SimpleModule("KALKULUS-REST", new Version(1, 0, 0, null, null, null));
        // Bruker kodeverdi serialisert som String mot kalkulus (innkommende rest returnerer kodeverdi som objekt)
        module.addSerializer(new KalkulusKodelisteSerializer(false));
        return module;
    }

    public static ObjectMapper getMapper() {
        return ftInntektsmeldingMapper;
    }
}
