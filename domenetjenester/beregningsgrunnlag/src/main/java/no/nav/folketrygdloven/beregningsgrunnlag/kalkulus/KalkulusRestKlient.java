package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.kalkulus.mappers.JsonMapper;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregnListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregningsgrunnlagListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagDtoListeForGUIRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentGrunnbeløpRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HåndterBeregningListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.KontrollerGrunnbeløpRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.KopierBeregningListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.KopierOgResettBeregningListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.migrerAksjonspunkt.MigrerAksjonspunktListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.regelinput.KomprimerRegelInputRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.simulerTilkommetInntekt.SimulerTilkommetInntektListeRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.Grunnbeløp;
import no.nav.folketrygdloven.kalkulus.response.v1.GrunnbeløpReguleringRespons;
import no.nav.folketrygdloven.kalkulus.response.v1.KopiResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandListeResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagListe;
import no.nav.folketrygdloven.kalkulus.response.v1.håndtering.OppdateringListeRespons;
import no.nav.folketrygdloven.kalkulus.response.v1.regelinput.Saksnummer;
import no.nav.folketrygdloven.kalkulus.response.v1.simulerTilkommetInntekt.SimulertTilkommetAktivitetListe;
import no.nav.folketrygdloven.kalkulus.response.v1.simulerTilkommetInntekt.SimulertTilkommetInntektListe;
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
    private final ObjectMapper kalkulusMapper = JsonMapper.getMapper();
    private final ObjectWriter kalkulusJsonWriter = kalkulusMapper.writerWithDefaultPrettyPrinter();
    private final ObjectReader tilstandReader = kalkulusMapper.readerFor(TilstandListeResponse.class);
    private final ObjectReader kopierReader = kalkulusMapper.readerFor(new TypeReference<List<KopiResponse>>() {
    });
    private final ObjectReader oppdaterListeReader = kalkulusMapper.readerFor(OppdateringListeRespons.class);
    private final ObjectReader dtoListeReader = kalkulusMapper.readerFor(BeregningsgrunnlagListe.class);
    private final ObjectReader behovForGreguleringReader = kalkulusMapper.readerFor(GrunnbeløpReguleringRespons.class);
    private final ObjectReader tilkommetInntektReader = kalkulusMapper.readerFor(SimulertTilkommetInntektListe.class);
    private final ObjectReader tilkommetAktivitetReader = kalkulusMapper.readerFor(SimulertTilkommetAktivitetListe.class);

    private final ObjectReader grunnlagListReader = kalkulusMapper.readerFor(new TypeReference<List<BeregningsgrunnlagGrunnlagDto>>() {
    });
    private final ObjectReader grunnbeløpReader = kalkulusMapper.readerFor(Grunnbeløp.class);
    private final ObjectReader saksnummerReader = kalkulusMapper.readerFor(no.nav.folketrygdloven.kalkulus.response.v1.regelinput.Saksnummer.class);


    private CloseableHttpClient restClient;
    private URI kalkulusEndpoint;
    private URI kopierEndpoint;
    private URI beregnEndpoint;
    private URI oppdaterListeEndpoint;
    private URI beregningsgrunnlagListeDtoEndpoint;

    private URI beregningsgrunnlagGrunnlagBolkEndpoint;
    private URI deaktiverBeregningsgrunnlag;
    private URI grunnbeløp;

    private URI kontrollerGrunnbeløp;
    private URI simulerTilkommetInntekt;
    private URI simulerTilkommetAktivitet;

    private URI migrerAksjonspunkter;
    private URI komprimerRegelinput;
    private URI komprimerFlereRegelinput;

    private URI kopierOgResettEndpoint;


    protected KalkulusRestKlient() {
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
        this.beregnEndpoint = toUri("/api/kalkulus/v1/beregn/bolk");
        this.kopierEndpoint = toUri("/api/kalkulus/v1/kopier/bolk");
        this.deaktiverBeregningsgrunnlag = toUri("/api/kalkulus/v1/deaktiver/bolk");
        this.oppdaterListeEndpoint = toUri("/api/kalkulus/v1/oppdaterListe");
        this.beregningsgrunnlagListeDtoEndpoint = toUri("/api/kalkulus/v1/beregningsgrunnlagListe");
        this.beregningsgrunnlagGrunnlagBolkEndpoint = toUri("/api/kalkulus/v1/grunnlag/bolk");
        this.grunnbeløp = toUri("/api/kalkulus/v1/grunnbelop");
        this.kontrollerGrunnbeløp = toUri("/api/kalkulus/v1/kontrollerGregulering");
        this.migrerAksjonspunkter = toUri("/api/kalkulus/v1/migrerAksjonspunkter");
        this.komprimerRegelinput = toUri("/api/kalkulus/v1/komprimerRegelSporing");
        this.komprimerFlereRegelinput = toUri("/api/kalkulus/v1/komprimerAntallRegelSporinger");
        this.kopierOgResettEndpoint = toUri("/api/kalkulus/v1//kopierOgResett/bolk");
        this.simulerTilkommetInntekt = toUri("/api/kalkulus/v1/simulerTilkommetInntektForKoblinger");
        this.simulerTilkommetAktivitet = toUri("/api/kalkulus/v1/simulerTilkommetAktivitetForKoblinger");
    }


    public TilstandListeResponse beregn(BeregnListeRequest request) {
        var endpoint = beregnEndpoint;

        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request), tilstandReader);
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }

    public List<KopiResponse> kopierBeregning(KopierBeregningListeRequest request) {
        var endpoint = kopierEndpoint;

        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request), kopierReader);
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }

    public void kopierOgResettBeregning(KopierOgResettBeregningListeRequest request) {
        var endpoint = kopierOgResettEndpoint;

        try {
            utfør(endpoint, kalkulusJsonWriter.writeValueAsString(request));
        } catch (IOException e) {
            throw RestTjenesteFeil.FEIL.feilVedKallTilKalkulus(endpoint, e.getMessage()).toException();
        }
    }

    public OppdateringListeRespons oppdaterBeregningListe(HåndterBeregningListeRequest request) {
        try {
            return getResponse(oppdaterListeEndpoint, kalkulusJsonWriter.writeValueAsString(request), oppdaterListeReader);
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }

    public void deaktiverBeregningsgrunnlag(BeregningsgrunnlagListeRequest request) {
        var endpoint = deaktiverBeregningsgrunnlag;
        try {
            deaktiver(endpoint, kalkulusJsonWriter.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }

    public List<BeregningsgrunnlagGrunnlagDto> hentBeregningsgrunnlagGrunnlag(HentBeregningsgrunnlagListeRequest req) {
        var endpoint = beregningsgrunnlagGrunnlagBolkEndpoint;

        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(req), grunnlagListReader);
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

    public Grunnbeløp hentGrunnbeløp(HentGrunnbeløpRequest request) {
        var endpoint = grunnbeløp;

        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request), grunnbeløpReader);
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }

    public GrunnbeløpReguleringRespons kontrollerBehovForGRegulering(KontrollerGrunnbeløpRequest request) {
        var endpoint = kontrollerGrunnbeløp;
        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request), behovForGreguleringReader);
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }

    public SimulertTilkommetInntektListe simulerTilkommetInntekt(SimulerTilkommetInntektListeRequest request) {
        var endpoint = simulerTilkommetInntekt;
        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request), tilkommetInntektReader);
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }
    
    public SimulertTilkommetAktivitetListe simulerTilkommetAktivitet(SimulerTilkommetInntektListeRequest request) {
        var endpoint = simulerTilkommetAktivitet;
        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request), tilkommetAktivitetReader);
        } catch (JsonProcessingException e) {
            throw RestTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        }
    }

    public void migrerAksjonspunkter(MigrerAksjonspunktListeRequest request) {
        var endpoint = migrerAksjonspunkter;
        try {
            utfør(endpoint, kalkulusJsonWriter.writeValueAsString(request));
        } catch (IOException e) {
            throw RestTjenesteFeil.FEIL.feilVedKallTilKalkulus(endpoint, e.getMessage()).toException();
        }
    }

    public String komprimerRegelinput(KomprimerRegelInputRequest request) {
        var endpoint = komprimerRegelinput;
        try {
            Saksnummer saksnummmer = getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request), saksnummerReader);
            return saksnummmer == null ? null : saksnummmer.getSaksnummer();
        } catch (IOException e) {
            throw RestTjenesteFeil.FEIL.feilVedKallTilKalkulus(endpoint, e.getMessage()).toException();
        }
    }

    public int komprimerFlereRegelinput(KomprimerRegelInputRequest request) {
        var endpoint = komprimerFlereRegelinput;
        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request), JsonMapper.getMapper().readerFor(Integer.class));
        } catch (IOException e) {
            throw RestTjenesteFeil.FEIL.feilVedKallTilKalkulus(endpoint, e.getMessage()).toException();
        }
    }


    private <T> T getResponse(URI endpoint, String json, ObjectReader reader) {
        try {
            return utførOgHent(endpoint, json, new ObjectReaderResponseHandler<>(endpoint, reader));
        } catch (IOException e) {
            throw RestTjenesteFeil.FEIL.feilVedKallTilKalkulus(endpoint, e.getMessage()).toException();
        }
    }

    private void deaktiver(URI endpoint, String json) {
        try {
            utfør(endpoint, json);
        } catch (IOException e) {
            throw RestTjenesteFeil.FEIL.feilVedKallTilKalkulus(endpoint, e.getMessage()).toException();
        }
    }

    private void utfør(URI endpoint, String json) throws IOException {
        var httpPost = new HttpPost(endpoint); // NOSONAR håndterer i responseHandler
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        try (var httpResponse = restClient.execute(httpPost)) {
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
                    throw RestTjenesteFeil.FEIL.feilKallTilKalkulus(endpoint, feilmelding).toException();
                }
            }
        } catch (VLException e) {
            throw e; // rethrow
        } catch (RuntimeException re) {
            log.warn("Feil ved henting av data. uri=" + endpoint, re);
            throw re;
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
