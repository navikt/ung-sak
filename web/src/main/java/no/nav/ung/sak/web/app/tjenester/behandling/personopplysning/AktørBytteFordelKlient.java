package no.nav.ung.sak.web.app.tjenester.behandling.personopplysning;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.typer.AktørId;

@ApplicationScoped
@ScopedRestIntegration(scopeKey = "k9fordel.scope", defaultScope = "api://prod-fss.k9saksbehandling.k9fordel/.default")
public class AktørBytteFordelKlient {

    private OidcRestClient restClient;
    private URI endpoint;
    private URI aktørbytteEndpoint;

    public AktørBytteFordelKlient() {
    }

    @Inject
    public AktørBytteFordelKlient(OidcRestClient restClient,
                              @KonfigVerdi(value = "k9fordel.url") URI endpoint) {
        this.restClient = restClient;
        this.endpoint = endpoint;
        this.aktørbytteEndpoint = toUri("/forvaltning/oppdaterAktoerId");
    }

    public int utførAktørbytte(AktørId gyldig, AktørId utgått) {
        var httpPost = new HttpPost(aktørbytteEndpoint);
        String json = null;
        try {
            json = JsonObjectMapper.getJson(new ByttAktørRequest(utgått, gyldig));
        } catch (IOException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        try (var httpResponse = restClient.execute(httpPost)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (!isOk(responseCode)) {
                if (responseCode != HttpStatus.SC_NO_CONTENT && responseCode != HttpStatus.SC_ACCEPTED) {
                    String responseBody = EntityUtils.toString(httpResponse.getEntity());
                    String feilmelding = "Kunne ikke utføre kall til k9-fordel,"
                        + " endpoint=" + httpPost.getURI()
                        + ", HTTP status=" + httpResponse.getStatusLine()
                        + ". HTTP Errormessage=" + responseBody;
                    throw RestTjenesteFeil.FEIL.feilVedKallTilFordel(endpoint, feilmelding).toException();
                }
            }
            return Integer.parseInt(EntityUtils.toString(httpResponse.getEntity()));
        } catch (IOException e) {
            throw RestTjenesteFeil.FEIL.feilVedKallTilFordel(endpoint, e.getMessage()).toException();
        }
    }
    private boolean isOk(int responseCode) {
        return responseCode == HttpStatus.SC_OK;
    }

    private URI toUri(String relativeUri) {
        String uri = endpoint.toString() + relativeUri;
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig uri: " + uri, e);
        }
    }


    record ByttAktørRequest (
        @JsonProperty(value = "utgatt", required = true) AktørId utgåttAktør,
        @JsonProperty(value = "gyldig", required = true) AktørId gyldigAktør
    ) {
    }


    interface RestTjenesteFeil extends DeklarerteFeil {
        RestTjenesteFeil FEIL = FeilFactory.create(RestTjenesteFeil.class);

        @TekniskFeil(feilkode = "F-K9-FORDEL-1000001", feilmelding = "Feil ved kall til k9-fordel [%s]: %s", logLevel = LogLevel.ERROR)
        Feil feilVedKallTilFordel(URI endpoint, String feilmelding);


        @TekniskFeil(feilkode = "F-K9-FORDEL-1000003", feilmelding = "Feil ved kall til k9-fordel: %s", logLevel = LogLevel.ERROR)
        Feil feilVedJsonParsing(String feilmelding);
    }



}
