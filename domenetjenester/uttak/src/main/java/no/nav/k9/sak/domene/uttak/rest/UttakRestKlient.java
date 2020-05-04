package no.nav.k9.sak.domene.uttak.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import no.nav.k9.sak.domene.uttak.uttaksplan.Uttaksplan;
import no.nav.k9.sak.domene.uttak.uttaksplan.UttaksplanListe;
import no.nav.k9.sak.domene.uttak.uttaksplan.input.UttaksplanRequest;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClientResponseHandler;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClientResponseHandler.ObjectReaderResponseHandler;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClientResponseHandler.StringResponseHandler;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class UttakRestKlient {
    private static final String TJENESTE_NAVN = "pleiepenger-barn-uttak";

    private static final Logger log = LoggerFactory.getLogger(UttakRestKlient.class);

    private ObjectMapper objectMapper = JsonMapper.getMapper();
    private ObjectReader uttaksplanListReader = objectMapper.readerFor(UttaksplanListe.class);
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

    public Uttaksplan opprettUttaksplan(UttaksplanRequest req) {
        URIBuilder builder = new URIBuilder(endpointUttaksplan);
        try {
            HttpPost kall = new HttpPost(builder.build());
            var json = objectMapper.writer().writeValueAsString(req);
            kall.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            return utførOgHent(kall, json, new ObjectReaderResponseHandler<>(endpointUttaksplan, uttaksplanReader));
        } catch (IOException | URISyntaxException e) {
            throw RestTjenesteFeil.FEIL.feilKallTilUttak(req.getBehandlingId(), e).toException();
        }
    }

    public UttaksplanListe hentUttaksplaner(UUID... behandlingIder) {
        if (behandlingIder == null || behandlingIder.length == 0) {
            return new UttaksplanListe(Collections.emptyMap());
        }
        URIBuilder builder = new URIBuilder(endpointUttaksplan);
        for (var bid : behandlingIder) {
            builder.addParameter("behandlingId", bid.toString());
        }
        try {
            HttpGet kall = new HttpGet(builder.build());
            return utførOgHent(kall, null, new ObjectReaderResponseHandler<>(endpointUttaksplan, uttaksplanListReader));
        } catch (IOException | URISyntaxException e) {
            throw RestTjenesteFeil.FEIL.feilKallTilUttakForPlaner(Arrays.asList(behandlingIder), e).toException();
        }
    }

    public UttaksplanListe hentUttaksplaner(List<Saksnummer> saksnummere) {
        if (saksnummere == null || saksnummere.isEmpty()) {
            return new UttaksplanListe(Collections.emptyMap());
        }
        URIBuilder builder = new URIBuilder(endpointUttaksplan);
        for (var s : saksnummere) {
            builder.addParameter("saksnummer", s.toString());
        }
        try {
            HttpGet kall = new HttpGet(builder.build());
            return utførOgHent(kall, null, new ObjectReaderResponseHandler<>(endpointUttaksplan, uttaksplanListReader));
        } catch (IOException | URISyntaxException e) {
            throw RestTjenesteFeil.FEIL.feilKallTilUttakForPlanerForSaker(saksnummere, e).toException();
        }
    }

    public String hentUttaksplanerRaw(UUID... behandlingIder) {
        if (behandlingIder == null || behandlingIder.length == 0) {
            return null;
        }
        URIBuilder builder = new URIBuilder(endpointUttaksplan);
        for (var uuid : behandlingIder) {
            builder.addParameter("behandlingId", uuid.toString());
        }
        try {
            HttpGet kall = new HttpGet(builder.build());
            return utførOgHent(kall, null, new StringResponseHandler(endpointUttaksplan));
        } catch (IOException | URISyntaxException e) {
            throw RestTjenesteFeil.FEIL.feilKallTilUttakForPlaner(Arrays.asList(behandlingIder), e).toException();
        }
    }

    public String hentUttaksplanerRaw(List<Saksnummer> saksnummere) {
        if (saksnummere == null || saksnummere.isEmpty()) {
            return null;
        }
        URIBuilder builder = new URIBuilder(endpointUttaksplan);
        for (var s : saksnummere) {
            builder.addParameter("saksnummer", s.toString());
        }
        try {
            HttpGet kall = new HttpGet(builder.build());
            return utførOgHent(kall, null, new StringResponseHandler(endpointUttaksplan));
        } catch (IOException | URISyntaxException e) {
            throw RestTjenesteFeil.FEIL.feilKallTilUttakForPlanerForSaker(saksnummere, e).toException();
        }
    }

    private <T> T utførOgHent(HttpUriRequest request, String jsonInput, OidcRestClientResponseHandler<T> responseHandler) throws IOException {
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

        @TekniskFeil(feilkode = "K9SAK-UT-1000001", feilmelding = "Feil ved kall til K9Uttak: %s", logLevel = LogLevel.ERROR)
        Feil feilVedKallTilUttak(String feilmelding);

        @TekniskFeil(feilkode = "K9SAK-UT-1000002", feilmelding = "Feil ved kall til K9Uttak: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilUttak(String feilmelding);

        @TekniskFeil(feilkode = "K9SAK-UT-1000003", feilmelding = "Feil ved kall til K9Uttak: %s", logLevel = LogLevel.WARN)
        Feil feilVedJsonParsing(String feilmelding);

        @TekniskFeil(feilkode = "K9SAK-UT-1000004", feilmelding = "Feil ved kall til K9Uttak: Kunne ikke hente uttaksplan for behandling: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilUttak(UUID behandlingUuid, Throwable t);

        @TekniskFeil(feilkode = "K9SAK-UT-1000005", feilmelding = "Feil ved kall til K9Uttak: Kunne ikke hente uttaksplaner for behandlinger: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilUttakForPlaner(Collection<UUID> behandlingUuid, Throwable t);

        @TekniskFeil(feilkode = "K9SAK-UT-1000006", feilmelding = "Feil ved kall til K9Uttak: Kunne ikke hente uttaksplaner for saker: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilUttakForPlanerForSaker(Collection<Saksnummer> saksnummere, Throwable t);
    }

}
