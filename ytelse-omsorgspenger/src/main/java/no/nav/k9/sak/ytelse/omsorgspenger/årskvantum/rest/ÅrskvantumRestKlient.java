package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.core.HttpHeaders;

import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.aarskvantum.kontrakter.MinMaxRequest;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumForbrukteDager;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumGrunnlag;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumResultat;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResterendeDager;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@Default
public class ÅrskvantumRestKlient implements ÅrskvantumKlient {
    private static final String TJENESTE_NAVN = "k9-aarskvantum";

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static final Logger log = LoggerFactory.getLogger(ÅrskvantumRestKlient.class);

    private OidcRestClient restKlient;
    private URI endpointUttaksplan;

    protected ÅrskvantumRestKlient() {
        // for proxying
    }

    @Inject
    public ÅrskvantumRestKlient(OidcRestClient restKlient, @KonfigVerdi(value = "k9.oms.aarskvantum.url") URI endpoint) {
        this.restKlient = restKlient;
        this.endpointUttaksplan = toUri(endpoint, "/k9/aarskvantum");
    }

    @Override
    public ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumGrunnlag årskvantumRequest) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum");
            var result = restKlient.post(endpoint, årskvantumRequest, ÅrskvantumResultat.class);
            var constraints = VALIDATOR.validate(result);
            if (!constraints.isEmpty()) {
                throw new IllegalStateException("Ugyldig response fra " + endpoint + ", ref=" + årskvantumRequest.getBehandlingUUID() + ": " + constraints);
            }
            return result;
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilÅrskvantum(e).toException();
        }
    }

    @Override
    public void avbrytÅrskvantumForBehandling(UUID behandlingUUID) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/avbrytkvantumforbehandling");
            restKlient.post(endpoint, behandlingUUID.toString());
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilhentÅrskvantumForBehandling(e).toException();
        }
    }

    @Override
    public ÅrskvantumForbrukteDager hentÅrskvantumForBehandling(UUID behandlingUUID) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum/forbruktedager?behandlingUUID=" + behandlingUUID.toString());
            var result = restKlient.get(endpoint, ÅrskvantumForbrukteDager.class);
            var constraints = VALIDATOR.validate(result);
            if (!constraints.isEmpty()) {
                throw new IllegalStateException("Ugyldig response fra " + endpoint + ", behandlingUUID=" + behandlingUUID + ": " + constraints);
            }
            return result;
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilhentÅrskvantumForBehandling(e).toException();
        }
    }

    @Override
    public Periode hentPeriodeForFagsak(String saksnummer) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum/minmax");
            var result = restKlient.post(endpoint, new MinMaxRequest(saksnummer), Set.of(new BasicHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())), Periode.class);
            var constraints = VALIDATOR.validate(result);
            if (!constraints.isEmpty()) {
                throw new IllegalStateException("Ugyldig response fra " + endpoint + ", saksnummer=" + saksnummer + ": " + constraints);
            }
            return result;
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilhentÅrskvantumForFagsak(e).toException();
        }
    }

    @Override
    public ÅrskvantumResterendeDager hentResterendeKvantum(String aktørId) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/hentresterendekvantum");
            var result = restKlient.post(endpoint, aktørId, ÅrskvantumResterendeDager.class);
            var constraints = VALIDATOR.validate(result);
            if (!constraints.isEmpty()) {
                throw new IllegalStateException("Ugyldig response fra " + endpoint + ", aktørid=" + aktørId + ": " + constraints);
            }
            return result;
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilhentResterendeKvantum(e).toException();
        }
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
        RestTjenesteFeil FEIL = FeilFactory.create(RestTjenesteFeil.class);

        @TekniskFeil(feilkode = "K9SAK-AK-1000088", feilmelding = "Feil ved kall til K9-AARSKVANTUM: Kunne ikke hente Årskvantum For Behandling: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilhentÅrskvantumForBehandling(Throwable t);

        @TekniskFeil(feilkode = "K9SAK-AK-1000089", feilmelding = "Feil ved kall til K9-AARSKVANTUM: Kunne ikke hente Årskvantum For Fagsak: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilhentÅrskvantumForFagsak(Throwable t);

        @TekniskFeil(feilkode = "K9SAK-AK-1000090", feilmelding = "Feil ved kall til K9-AARSKVANTUM: Kunne ikke hente Resterende Kvantum: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilhentResterendeKvantum(Throwable t);

        @TekniskFeil(feilkode = "K9SAK-AK-1000091", feilmelding = "Feil ved kall til K9-AARSKVANTUM: Kunne ikke beregne uttaksplan for årskvantum: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilÅrskvantum(Throwable t);

    }

}
