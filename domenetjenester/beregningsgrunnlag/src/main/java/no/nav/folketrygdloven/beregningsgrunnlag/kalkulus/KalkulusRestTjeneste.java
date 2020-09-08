package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

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
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagDtoListeForGUIRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentGrunnbeløpRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HåndterBeregningListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HåndterBeregningRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.StartBeregningRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.Grunnbeløp;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagListe;
import no.nav.folketrygdloven.kalkulus.response.v1.håndtering.OppdateringListeRespons;
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
public class KalkulusRestTjeneste {

    private static final Logger log = LoggerFactory.getLogger(KalkulusRestTjeneste.class);
    private final ObjectMapper kalkulusMapper = JsonMapper.getMapper();
    private final ObjectWriter kalkulusJsonWriter = kalkulusMapper.writerWithDefaultPrettyPrinter();
    private final ObjectReader tilstandReader = kalkulusMapper.readerFor(TilstandResponse.class);
    private final ObjectReader oppdaterReader = kalkulusMapper.readerFor(OppdateringRespons.class);
    private final ObjectReader oppdaterListeReader = kalkulusMapper.readerFor(OppdateringListeRespons.class);
    private final ObjectReader dtoListeReader = kalkulusMapper.readerFor(BeregningsgrunnlagListe.class);
    private final ObjectReader booleanReader = kalkulusMapper.readerFor(Boolean.class);
    private final ObjectReader grunnlagReader = kalkulusMapper.readerFor(BeregningsgrunnlagGrunnlagDto.class);
    private final ObjectReader fastSattReader = kalkulusMapper.readerFor(no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.fastsatt.BeregningsgrunnlagDto.class);
    private final ObjectReader grunnbeløpReader = kalkulusMapper.readerFor(Grunnbeløp.class);

    private OidcRestClient oidcRestClient;
    private URI kalkulusEndpoint;
    private URI startEndpoint;
    private URI fortsettEndpoint;
    private URI oppdaterEndpoint;
    private URI oppdaterListeEndpoint;
    private URI fastsattEndpoint;
    private URI beregningsgrunnlagListeDtoEndpoint;
    private URI beregningsgrunnlagGrunnlagEndpoint;
    private URI erEndringIBeregningEndpoint;
    private URI deaktiverBeregningsgrunnlag;
    private URI grunnbeløp;

    protected KalkulusRestTjeneste() {
        // cdi
    }

    @Inject
    public KalkulusRestTjeneste(OidcRestClient oidcRestClient,
                                @KonfigVerdi(value = "ftkalkulus.url") URI endpoint) {
        this.oidcRestClient = oidcRestClient;
        this.kalkulusEndpoint = endpoint;
        this.startEndpoint = toUri("/api/kalkulus/v1/start");
        this.fortsettEndpoint = toUri("/api/kalkulus/v1/fortsett");
        this.oppdaterEndpoint = toUri("/api/kalkulus/v1/oppdater");
        this.oppdaterListeEndpoint = toUri("/api/kalkulus/v1/oppdaterListe");
        this.fastsattEndpoint = toUri("/api/kalkulus/v1/fastsatt");
        this.beregningsgrunnlagListeDtoEndpoint = toUri("/api/kalkulus/v1/beregningsgrunnlagListe");
        this.beregningsgrunnlagGrunnlagEndpoint = toUri("/api/kalkulus/v1/grunnlag");
        this.erEndringIBeregningEndpoint = toUri("/api/kalkulus/v1/erEndring");
        this.deaktiverBeregningsgrunnlag = toUri("/api/kalkulus/v1/deaktiver");
        this.grunnbeløp = toUri("/api/kalkulus/v1/grunnbelop");

    }

    public TilstandResponse startBeregning(StartBeregningRequest request) {
        var endpoint = startEndpoint;

        try {
            String json = kalkulusJsonWriter.writeValueAsString(request);
            return getResponse(endpoint, json, tilstandReader);
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }

    public TilstandResponse fortsettBeregning(FortsettBeregningRequest request) {
        var endpoint = fortsettEndpoint;

        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request), tilstandReader);
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }

    public OppdateringRespons oppdaterBeregning(HåndterBeregningRequest request) {
        var endpoint = oppdaterEndpoint;

        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request), oppdaterReader);
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }

    public OppdateringListeRespons oppdaterBeregningListe(HåndterBeregningListeRequest request) {
        try {
            return getResponse(oppdaterListeEndpoint, kalkulusJsonWriter.writeValueAsString(request), oppdaterListeReader);
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }

    public void deaktiverBeregningsgrunnlag(HentBeregningsgrunnlagRequest request) {
        var endpoint = deaktiverBeregningsgrunnlag;
        try {
            deaktiver(endpoint, kalkulusJsonWriter.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }

    public no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagGrunnlagDto hentBeregningsgrunnlagGrunnlag(HentBeregningsgrunnlagRequest request) {
        var endpoint = beregningsgrunnlagGrunnlagEndpoint;

        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request), grunnlagReader);
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }

    public BeregningsgrunnlagListe hentBeregningsgrunnlagDto(HentBeregningsgrunnlagDtoListeForGUIRequest request) {
        var endpoint = beregningsgrunnlagListeDtoEndpoint;

        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request), dtoListeReader);
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }

    public Boolean erEndringIBeregning(ErEndringIBeregningRequest request) {
        var endpoint = erEndringIBeregningEndpoint;

        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request), booleanReader);
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
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
            throw RestTjenesteFeil.FEIL.feilVedKallTilKalkulus(e.getMessage()).toException();
        }
    }

    private void deaktiver(URI endpoint, String json) {
        try {
            utfør(endpoint, json);
        } catch (IOException e) {
            throw RestTjenesteFeil.FEIL.feilVedKallTilKalkulus(e.getMessage()).toException();
        }
    }

    private no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.fastsatt.BeregningsgrunnlagDto getBeregningsgrunnlagFastsattDtoResponse(URI endpoint, String json) {
        return getResponse(endpoint, json, fastSattReader);
    }

    public no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.fastsatt.BeregningsgrunnlagDto hentFastsatt(HentBeregningsgrunnlagRequest request) {
        var endpoint = fastsattEndpoint;
        try {
            return getBeregningsgrunnlagFastsattDtoResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }

    private void utfør(URI endpoint, String json) throws IOException {
        var httpPost = new HttpPost(endpoint); // NOSONAR håndterer i responseHandler
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        try (var httpResponse = oidcRestClient.execute(httpPost)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (!isOk(responseCode)) {
                if (responseCode == HttpStatus.SC_NOT_MODIFIED) {
                    log.warn("Kall til deaktiver gjorde ingen endring på beregningsgrunnlag");
                } else if (responseCode != HttpStatus.SC_NO_CONTENT && responseCode != HttpStatus.SC_ACCEPTED) {
                    String responseBody = EntityUtils.toString(httpResponse.getEntity());
                    String feilmelding = "Kunne ikke utføre kall til kalkulus,"
                        + " endpoint=" + httpPost.getURI()
                        + ", HTTP status=" + httpResponse.getStatusLine()
                        + ". HTTP Errormessage=" + responseBody;
                    throw RestTjenesteFeil.FEIL.feilKallTilKalkulus(feilmelding).toException();
                }
            }
        } catch (RuntimeException re) {
            log.warn("Feil ved henting av data. uri=" + endpoint, re);
            throw re;
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
        static final KalkulusRestTjeneste.RestTjenesteFeil FEIL = FeilFactory.create(KalkulusRestTjeneste.RestTjenesteFeil.class);

        @TekniskFeil(feilkode = "F-FT-K-1000001", feilmelding = "Feil ved kall til Kalkulus: %s", logLevel = LogLevel.ERROR)
        Feil feilVedKallTilKalkulus(String feilmelding);

        @TekniskFeil(feilkode = "F-FT-K-1000002", feilmelding = "Feil ved kall til Kalkulus: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilKalkulus(String feilmelding);

        @TekniskFeil(feilkode = "F-FT-K-1000003", feilmelding = "Feil ved kall til Kalkulus: %s", logLevel = LogLevel.WARN)
        Feil feilVedJsonParsing(String feilmelding);
    }
}
