package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.core.HttpHeaders;

import no.nav.k9.aarskvantum.kontrakter.*;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;

import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.typer.Saksnummer;
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

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

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
            throw RestTjenesteFeil.FEIL.feilKallTilÅrskvantum(e.getMessage(), e).toException();
        }
    }

    @Override
    public void deaktiverUttakForBehandling(UUID behandlingUUID) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum/inaktiv?behandlingUUID=" + behandlingUUID.toString());
            restKlient.patch(endpoint, Object.class);
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilDeaktiverUttak(e.getMessage(), e).toException();
        }
    }

    @Override
    public void settUttaksplanTilManueltBekreftet(UUID behandlingUUID) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum/bekreft?behandlingUUID=" + behandlingUUID.toString());
            restKlient.patch(endpoint, Object.class);
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilsettUttaksplanTilManueltBekreftet(e.getMessage(), e).toException();
        }
    }

    @Override
    public void slettUttaksplan(UUID behandlingUUID) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum/slett?behandlingUUID=" + behandlingUUID.toString());
            restKlient.patch(endpoint, Object.class);
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilslettUttaksplan(e.getMessage(), e).toException();
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
            throw RestTjenesteFeil.FEIL.feilKallTilhentÅrskvantumForBehandling(e.getMessage(), e).toException();
        }
    }

    @Override
    public Periode hentPeriodeForFagsak(Saksnummer saksnummer) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum/minmax");
            var result = restKlient.post(endpoint, new MinMaxRequest(saksnummer.getVerdi()), Set.of(new BasicHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())), Periode.class);
            if(result == null) {
                return null;
            }
            var constraints = VALIDATOR.validate(result);
            if (!constraints.isEmpty()) {
                throw new IllegalStateException("Ugyldig response fra " + endpoint + ", saksnummer=" + saksnummer + ": " + constraints);
            }
            return result;
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilhentÅrskvantumForFagsak(e.getMessage(), e).toException();
        }
    }

    @Override
    public FullUttaksplan hentFullUttaksplan(Saksnummer saksnummer) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum/uttaksplan?saksnummer=" + saksnummer.getVerdi());
            var result = restKlient.get(endpoint, FullUttaksplan.class);
            var constraints = VALIDATOR.validate(result);
            if (!constraints.isEmpty()) {
                throw new IllegalStateException("Ugyldig response fra " + endpoint + ", : " + constraints);
            }
            return result;
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilhentÅrskvantumForFagsak(e.getMessage(), e).toException();
        }
    }

    @Override
    public FullUttaksplanForBehandlinger hentFullUttaksplanForBehandling(List<UUID> behandlinger) {
        var request = new FullUttaksplanRequest(behandlinger.stream().map(UUID::toString).collect(Collectors.toList()));
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum/uttaksplan");
            return restKlient.post(endpoint, request, FullUttaksplanForBehandlinger.class);
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.klarteIkkeHenteUttaksplanForBehandlinger(e.getMessage(), e).toException();
        }
    }

    @Override
    public ÅrskvantumUtbetalingGrunnlag hentUtbetalingGrunnlag(ÅrskvantumGrunnlag årskvantumGrunnlag) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum/hentUtbetalingGrunnlag");
            return restKlient.post(endpoint, årskvantumGrunnlag, ÅrskvantumUtbetalingGrunnlag.class);
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallHentUtbetalingGrunnlag(e.getMessage(), e).toException();
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
        Feil feilKallTilhentÅrskvantumForBehandling(String feilmelding, Throwable t);

        @TekniskFeil(feilkode = "K9SAK-AK-1000089", feilmelding = "Feil ved kall til K9-AARSKVANTUM: Kunne ikke hente Årskvantum For Fagsak: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilhentÅrskvantumForFagsak(String feilmelding, Throwable t);

        @TekniskFeil(feilkode = "K9SAK-AK-1000091", feilmelding = "Feil ved kall til K9-AARSKVANTUM: Kunne ikke deaktivere Uttak: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilDeaktiverUttak(String feilmelding, Throwable t);

        @TekniskFeil(feilkode = "K9SAK-AK-1000092", feilmelding = "Feil ved kall til K9-AARSKVANTUM: Kunne ikke beregne uttaksplan for årskvantum: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilÅrskvantum(String feilmelding, Throwable t);

        @TekniskFeil(feilkode = "K9SAK-AK-1000093", feilmelding = "Feil ved kall til K9-AARSKVANTUM: Kunne ikke settUttaksplanTilManueltBekreftet: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilsettUttaksplanTilManueltBekreftet(String feilmelding, Throwable t);

        @TekniskFeil(feilkode = "K9SAK-AK-1000094", feilmelding = "Feil ved kall til K9-AARSKVANTUM: Kunne ikke slettUttaksplan: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilslettUttaksplan(String feilmelding, Throwable t);

        @TekniskFeil(feilkode = "K9SAK-AK-1000095", feilmelding = "Feil ved kall til K9-AARSKVANTUM: Kunne ikke hentUtbetalingGrunnlag: %s", logLevel = LogLevel.WARN)
        Feil feilKallHentUtbetalingGrunnlag(String feilmelding, Throwable t);

        @TekniskFeil(feilkode = "K9SAK-AK-1000096", feilmelding = "Feil ved kall til K9-AARSKVANTUM: Kunne ikke uttaksplan for behandling: %s", logLevel = LogLevel.WARN)
        Feil klarteIkkeHenteUttaksplanForBehandlinger(String feilmelding, Throwable t);
    }

}
