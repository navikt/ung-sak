package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest;

import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumRequest;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResterendeDager;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.validation.Validation;
import javax.validation.Validator;
import java.net.URI;
import java.net.URISyntaxException;

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
    public ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumRequest årskvantumRequest) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/mock");
            var result = restKlient.post(endpoint, årskvantumRequest, ÅrskvantumResultat.class);
            var constraints = VALIDATOR.validate(result);
            if (!constraints.isEmpty()) {
                throw new IllegalStateException("Ugyldig response fra " + endpoint + ", ref=" + årskvantumRequest.getBehandlingId() + ": " + constraints);
            }
            return result;
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilÅrskvantum(e).toException();
        }
    }

    @Override
    public void avbrytÅrskvantumForBehandling(String behandlingId) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/avbrytkvantumforbehandling");
            restKlient.post(endpoint, behandlingId);
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilhentÅrskvantumForBehandling(e).toException();
        }
    }

    @Override
    public ÅrskvantumResultat hentÅrskvantumForBehandling(String behandlingId) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/hentkvantumforbehandling");
            var result = restKlient.post(endpoint, behandlingId, ÅrskvantumResultat.class);
            var constraints = VALIDATOR.validate(result);
            if (!constraints.isEmpty()) {
                throw new IllegalStateException("Ugyldig response fra " + endpoint + ", ref=" + behandlingId + ": " + constraints);
            }
            return result;
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilhentÅrskvantumForBehandling(e).toException();
        }
    }

    @Override
    public ÅrskvantumResultat hentÅrskvantumForFagsak(String fagsakId) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/hentkvantumforfagsak");
            var result = restKlient.post(endpoint, fagsakId, ÅrskvantumResultat.class);
            var constraints = VALIDATOR.validate(result);
            if (!constraints.isEmpty()) {
                throw new IllegalStateException("Ugyldig response fra " + endpoint + ", ref=" + fagsakId + ": " + constraints);
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
                throw new IllegalStateException("Ugyldig response fra " + endpoint + ", ref=" + aktørId + ": " + constraints);
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
