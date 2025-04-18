package no.nav.ung.sak.domene.abakus;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
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
import no.nav.abakus.iaygrunnlag.JsonObjectMapper;
import no.nav.abakus.iaygrunnlag.UuidDto;
import no.nav.abakus.iaygrunnlag.request.ByttAktørRequest;
import no.nav.abakus.iaygrunnlag.request.InnhentRegisterdataRequest;
import no.nav.abakus.iaygrunnlag.request.InntektArbeidYtelseGrunnlagRequest;
import no.nav.abakus.iaygrunnlag.request.KopierGrunnlagRequest;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.abakus.vedtak.ytelse.Aktør;
import no.nav.k9.felles.exception.VLException;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.OidcRestClientResponseHandler.ObjectReaderResponseHandler;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.typer.AktørId;

@ApplicationScoped
@ScopedRestIntegration(scopeKey = "k9abakus.scope", defaultScope = "api://prod-fss.k9saksbehandling.k9-abakus/.default")
public class AbakusTjeneste {

    private static final Logger log = LoggerFactory.getLogger(AbakusTjeneste.class);
    private final ObjectMapper iayMapper = JsonObjectMapper.getMapper();
    private final ObjectWriter iayJsonWriter = iayMapper.writerWithDefaultPrettyPrinter();
    private final ObjectReader iayGrunnlagReader = iayMapper.readerFor(InntektArbeidYtelseGrunnlagDto.class);
    private final ObjectReader uuidReader = iayMapper.readerFor(UuidDto.class);
    private URI innhentRegisterdata;
    private CloseableHttpClient restClient;
    private URI abakusEndpoint;
    private URI callbackUrl;
    private String callbackScope;
    private URI endpointGrunnlag;
    private URI endpointMottaOppgittOpptjening;
    private URI endpointMottaOppgittOpptjeningV2;
    private URI endpointOverstyrtOppgittOpptjening;
    private URI endpointKopierGrunnlag;
    private URI endpointOppdaterAktørId;


    AbakusTjeneste() {
        // for CDI
    }

    @Inject
    public AbakusTjeneste(OidcRestClient oidcRestClient,
                          @KonfigVerdi(value = "k9abakus.url") URI endpoint,
                          @KonfigVerdi(value = "abakus.callback.url") URI callbackUrl,
                          @KonfigVerdi(value = "abakus.callback.scope") String callbackScope) {
        this(endpoint, callbackUrl, callbackScope);
        this.restClient = oidcRestClient;
    }

    public AbakusTjeneste(SystemUserOidcRestClient oidcRestClient,
                          URI endpoint,
                          URI callbackUrl) {
        this(endpoint, callbackUrl, null);
        this.restClient = oidcRestClient;
    }

    private AbakusTjeneste(URI endpoint, URI callbackUrl, String callbackScope) {
        this.abakusEndpoint = endpoint;
        this.callbackUrl = callbackUrl;
        this.callbackScope = callbackScope;

        this.endpointGrunnlag = toUri("/api/iay/grunnlag/v1/");
        this.endpointMottaOppgittOpptjening = toUri("/api/iay/oppgitt/v1/motta");
        this.endpointMottaOppgittOpptjeningV2 = toUri("/api/iay/oppgitt/v2/motta");
        this.endpointOverstyrtOppgittOpptjening = toUri("/api/iay/oppgitt/v1/overstyr");
        this.endpointKopierGrunnlag = toUri("/api/iay/grunnlag/v1/kopier");
        this.innhentRegisterdata = toUri("/api/registerdata/v1/innhent/async");
        this.endpointOppdaterAktørId = toUri("/api/forvaltning/oppdaterAktoerId");
    }

    private URI toUri(String relativeUri) {
        String uri = abakusEndpoint.toString() + relativeUri;
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig uri: " + uri, e);
        }
    }

    public UuidDto innhentRegisterdata(InnhentRegisterdataRequest request) {
        var endpoint = innhentRegisterdata;

        var responseHandler = new ObjectReaderResponseHandler<UuidDto>(endpoint, uuidReader);
        try {
            var json = iayJsonWriter.writeValueAsString(request);
            return hentFraAbakus(new HttpPost(endpoint), responseHandler, json);// NOSONAR håndterer i responseHandler
        } catch (JsonProcessingException e) {
            throw AbakusTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        } catch (IOException e) {
            throw AbakusTjenesteFeil.FEIL.feilVedKallTilAbakus(e.getMessage()).toException();
        }
    }

    public InntektArbeidYtelseGrunnlagDto hentGrunnlag(InntektArbeidYtelseGrunnlagRequest request) throws IOException {
        var endpoint = endpointGrunnlag;
        var reader = iayGrunnlagReader;
        var responseHandler = new ObjectReaderResponseHandler<InntektArbeidYtelseGrunnlagDto>(endpoint, reader);
        var json = iayJsonWriter.writeValueAsString(request);
        return hentFraAbakus(new HttpPost(endpoint), responseHandler, json);// NOSONAR håndterer i responseHandler
    }

    public int utførAktørbytte(AktørId gyldigAktørid, AktørId utgåttAktørId) {
        try {

            String json = iayJsonWriter.writeValueAsString(new ByttAktørRequest(getAktør(utgåttAktørId), getAktør(gyldigAktørid)));
            return utførAktørbytte(json);
        } catch (IOException e) {
            throw AbakusTjenesteFeil.FEIL.feilVedKallTilAbakus(e.getMessage()).toException();
        }
    }

    private static Aktør getAktør(AktørId utgåttAktørId) {
        var utgåttAktør = new Aktør();
        utgåttAktør.setVerdi(utgåttAktørId.getId());
        return utgåttAktør;
    }

    private <T> T hentFraAbakus(HttpEntityEnclosingRequestBase httpKall, ObjectReaderResponseHandler<T> responseHandler, String json) throws IOException {
        if (json != null) {
            httpKall.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        }

        try (var httpResponse = restClient.execute(httpKall)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode == HttpStatus.SC_OK || responseCode == HttpStatus.SC_CREATED) {
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
                String feilmelding = "Kunne ikke hente grunnlag fra abakus: " + httpKall.getURI()
                    + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;
                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw AbakusTjenesteFeil.FEIL.feilKallTilAbakus(feilmelding).toException();
                } else {
                    throw AbakusTjenesteFeil.FEIL.feilVedKallTilAbakus(feilmelding).toException();
                }
            }
        } catch (VLException e) {
            throw e; // rethrow
        } catch (RuntimeException re) {
            throw new IllegalStateException("Feil ved henting av data fra abakus: endpoint=" + httpKall.getURI() + (json != null ? ", input=" + json.replaceAll("\\d{8,}", "x") : ""), re);
        }
    }


    public void lagreOppgittOpptjening(OppgittOpptjeningMottattRequest request) throws IOException {
        var json = iayJsonWriter.writeValueAsString(request);
        lagreOppgittOpptjening(request.getKoblingReferanse(), json, endpointMottaOppgittOpptjening);
    }

    public void lagreOppgittOpptjeningV2(OppgittOpptjeningMottattRequest request) throws IOException {
        var json = iayJsonWriter.writeValueAsString(request);
        lagreOppgittOpptjening(request.getKoblingReferanse(), json, endpointMottaOppgittOpptjeningV2);
    }

    public void lagreOppgittOpptjening(UUID behandlingRef, String json, URI endpoint) throws IOException {
        HttpPost httpPost = new HttpPost(endpoint);
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        log.info("Lagre oppgitt opptjening (behandlingUUID={}) i Abakus", behandlingRef);
        try (var httpResponse = restClient.execute(httpPost)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != HttpStatus.SC_OK) {
                String responseBody = EntityUtils.toString(httpResponse.getEntity());
                String feilmelding = "Kunne ikke lagre oppgitt opptjening for behandling: " + behandlingRef + " til abakus: " + httpPost.getURI()
                    + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;

                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw AbakusTjenesteFeil.FEIL.feilKallTilAbakus(feilmelding).toException();
                } else {
                    throw AbakusTjenesteFeil.FEIL.feilVedKallTilAbakus(feilmelding).toException();
                }
            }
        }
    }

    public void kopierGrunnlag(KopierGrunnlagRequest request) throws IOException {
        var json = iayJsonWriter.writeValueAsString(request);

        HttpPost httpPost = new HttpPost(endpointKopierGrunnlag);
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        log.info("Kopierer grunnlag fra (behandlingUUID={}) til (behandlingUUID={}) i Abakus", request.getGammelReferanse(), request.getNyReferanse());
        try (var httpResponse = restClient.execute(httpPost)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != HttpStatus.SC_OK) {
                String responseBody = EntityUtils.toString(httpResponse.getEntity());
                String feilmelding = "Feilet med å kopiere grunnlag fra (behandlingUUID=" + request.getGammelReferanse() + ") til (behandlingUUID=" + request.getNyReferanse() + ") i Abakus: "
                    + httpPost.getURI()
                    + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;

                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw AbakusTjenesteFeil.FEIL.feilKallTilAbakus(feilmelding).toException();
                } else {
                    throw AbakusTjenesteFeil.FEIL.feilVedKallTilAbakus(feilmelding).toException();
                }
            }
        }
    }

    public String getCallbackUrl() {
        return callbackUrl.toString();
    }

    public String getCallbackScope() {
        return callbackScope;
    }


    public void lagreOverstyrtOppgittOpptjening(OppgittOpptjeningMottattRequest request) throws IOException {
        var json = iayJsonWriter.writeValueAsString(request);
        lagreOverstyrtOppgittOpptjening(request.getKoblingReferanse(), json);
    }

    public void lagreOverstyrtOppgittOpptjening(UUID behandlingRef, String json) throws IOException {
        HttpPost httpPost = new HttpPost(endpointOverstyrtOppgittOpptjening);
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        log.info("Lagre overstyrt oppgitt opptjening (behandlingUUID={}) i Abakus", behandlingRef);
        try (var httpResponse = restClient.execute(httpPost)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != HttpStatus.SC_OK) {
                String responseBody = EntityUtils.toString(httpResponse.getEntity());
                String feilmelding = "Kunne ikke lagre overstyrt oppgitt opptjening for behandling: " + behandlingRef + " til abakus: " + httpPost.getURI()
                    + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;

                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw AbakusTjenesteFeil.FEIL.feilKallTilAbakus(feilmelding).toException();
                } else {
                    throw AbakusTjenesteFeil.FEIL.feilVedKallTilAbakus(feilmelding).toException();
                }
            }
        }
    }

    private int utførAktørbytte(String json) throws IOException {
        HttpPost httpPost = new HttpPost(endpointOppdaterAktørId);
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        try (var httpResponse = restClient.execute(httpPost)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != HttpStatus.SC_OK) {
                String responseBody = EntityUtils.toString(httpResponse.getEntity());
                String feilmelding = "Kunne ikke oppdatere aktørid" + httpPost.getURI()
                    + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;

                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw AbakusTjenesteFeil.FEIL.feilKallTilAbakus(feilmelding).toException();
                } else {
                    throw AbakusTjenesteFeil.FEIL.feilVedKallTilAbakus(feilmelding).toException();
                }
            }
            return Integer.parseInt(EntityUtils.toString(httpResponse.getEntity()));
        }
    }

    public interface AbakusTjenesteFeil extends DeklarerteFeil {
        AbakusTjenesteFeil FEIL = FeilFactory.create(AbakusTjenesteFeil.class);

        @TekniskFeil(feilkode = "FP-018669", feilmelding = "Feil ved kall til Abakus: %s", logLevel = LogLevel.ERROR)
        Feil feilVedKallTilAbakus(String feilmelding);

        @TekniskFeil(feilkode = "FP-918669", feilmelding = "Feil ved kall til Abakus: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilAbakus(String feilmelding);

        @TekniskFeil(feilkode = "FP-851387", feilmelding = "Feil ved kall til Abakus: %s", logLevel = LogLevel.WARN)
        Feil feilVedJsonParsing(String feilmelding);

    }

}
