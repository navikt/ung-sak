package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import no.nav.folketrygdloven.kalkulus.mappers.JsonMapper;
import no.nav.folketrygdloven.kalkulus.request.v1.ErEndringIBeregningRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.FortsettBeregningRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagDtoForGUIRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagGrunnlagForReferanseRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HåndterBeregningRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.StartBeregningRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.frisinn.BeregningsgrunnlagFRISINNDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.håndtering.OppdateringRespons;
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
public class FrisinnKalkulusRestTjeneste {

    private static final Logger log = LoggerFactory.getLogger(FrisinnKalkulusRestTjeneste.class);
    private final ObjectMapper kalkulusMapper = JsonMapper.getMapper();
    private final ObjectWriter kalkulusJsonWriter = kalkulusMapper.writerWithDefaultPrettyPrinter();
    private final ObjectReader frisinnGrunnlagReader = kalkulusMapper.readerFor(BeregningsgrunnlagFRISINNDto.class);

    private OidcRestClient oidcRestClient;
    private URI frisinnGrunnlagEndpoint;
    private URI kalkulusEndpoint;

    protected FrisinnKalkulusRestTjeneste() {
        // cdi
    }

    @Inject
    public FrisinnKalkulusRestTjeneste(OidcRestClient oidcRestClient,
                                       @KonfigVerdi(value = "ftkalkulus.url") URI endpoint) {
        this.oidcRestClient = oidcRestClient;
        this.kalkulusEndpoint = endpoint;
        this.frisinnGrunnlagEndpoint = toUri("/api/kalkulus/v1/frisinnGrunnlag");
    }

    public BeregningsgrunnlagFRISINNDto hentBeregningsgrunnlagFRISINNDto(HentBeregningsgrunnlagDtoForGUIRequest request) {
        var endpoint = frisinnGrunnlagEndpoint;

        try {
            return getBeregningsgrunnlagFrisinnDtoResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }

    private BeregningsgrunnlagFRISINNDto getBeregningsgrunnlagFrisinnDtoResponse(URI endpoint, String json) {
        try {
            return utførOgHent(endpoint, json, new ObjectReaderResponseHandler<>(endpoint, frisinnGrunnlagReader));
        } catch (IOException e) {
            throw RestTjenesteFeil.FEIL.feilVedKallTilKalkulus(e.getMessage()).toException();
        }
    }


    private <T> T utførOgHent(URI endpoint, String json, OidcRestClientResponseHandler<T> responseHandler) throws IOException {
        var httpPost = new HttpPost(endpoint); // NOSONAR håndterer i responseHandler
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        try (var httpResponse = oidcRestClient.execute(httpPost)) {
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
                    throw RestTjenesteFeil.FEIL.feilKallTilKalkulus(feilmelding).toException();
                } else {
                    throw RestTjenesteFeil.FEIL.feilVedKallTilKalkulus(feilmelding).toException();
                }
            }
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
        static final FrisinnKalkulusRestTjeneste.RestTjenesteFeil FEIL = FeilFactory.create(FrisinnKalkulusRestTjeneste.RestTjenesteFeil.class);

        @TekniskFeil(feilkode = "F-FT-K-1000001", feilmelding = "Feil ved kall til Kalkulus: %s", logLevel = LogLevel.ERROR)
        Feil feilVedKallTilKalkulus(String feilmelding);

        @TekniskFeil(feilkode = "F-FT-K-1000002", feilmelding = "Feil ved kall til Kalkulus: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilKalkulus(String feilmelding);

        @TekniskFeil(feilkode = "F-FT-K-1000003", feilmelding = "Feil ved kall til Kalkulus: %s", logLevel = LogLevel.WARN)
        Feil feilVedJsonParsing(String feilmelding);
    }
}
