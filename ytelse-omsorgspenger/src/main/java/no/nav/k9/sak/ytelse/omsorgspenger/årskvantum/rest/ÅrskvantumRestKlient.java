package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.ws.rs.core.HttpHeaders;
import no.nav.k9.aarskvantum.kontrakter.AktørBytte;
import no.nav.k9.aarskvantum.kontrakter.FullUttaksplan;
import no.nav.k9.aarskvantum.kontrakter.FullUttaksplanForBehandlinger;
import no.nav.k9.aarskvantum.kontrakter.FullUttaksplanRequest;
import no.nav.k9.aarskvantum.kontrakter.LukketPeriode;
import no.nav.k9.aarskvantum.kontrakter.ManuellVurderingRequest;
import no.nav.k9.aarskvantum.kontrakter.MinMaxRequest;
import no.nav.k9.aarskvantum.kontrakter.RammevedtakRequest;
import no.nav.k9.aarskvantum.kontrakter.RammevedtakResponse;
import no.nav.k9.aarskvantum.kontrakter.RammevedtakV2Request;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumForbrukteDager;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumForbrukteDagerV2;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumGrunnlag;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumGrunnlagV2;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumResultat;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumUtbetalingGrunnlag;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumUttrekk;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
@Default
@ScopedRestIntegration(scopeKey = "k9.oms.aarskvantum.scope", defaultScope = "api://prod-fss.k9saksbehandling.k9-aarskvantum/.default")
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
    public ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumGrunnlag grunnlag) {
        if (grunnlag.getUttakperioder().isEmpty()) {
            throw new IllegalArgumentException("Har ikke fraværsperioder for " + grunnlag.getBehandlingUUID());
        }

        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum");
            var result = restKlient.post(endpoint, grunnlag, ÅrskvantumResultat.class);
            var constraints = VALIDATOR.validate(result);
            if (!constraints.isEmpty()) {
                throw new IllegalStateException("Ugyldig response fra " + endpoint + ", ref=" + grunnlag.getBehandlingUUID() + ": " + constraints);
            }
            return result;
        } catch (IllegalStateException e) {
            throw e; // rethrow
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilÅrskvantum(e.getMessage(), e).toException();
        }
    }

    @Override
    public ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumGrunnlagV2 grunnlag) {
        if (grunnlag.getUttakperioder().isEmpty()) {
            throw new IllegalArgumentException("Har ikke fraværsperioder for " + grunnlag.getBehandlingUUID());
        }

        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum/v2");
            var result = restKlient.post(endpoint, grunnlag, ÅrskvantumResultat.class);
            var constraints = VALIDATOR.validate(result);
            if (!constraints.isEmpty()) {
                throw new IllegalStateException("Ugyldig response fra " + endpoint + ", ref=" + grunnlag.getBehandlingUUID() + ": " + constraints);
            }
            return result;
        } catch (IllegalStateException e) {
            throw e; // rethrow
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
    public void innvilgeEllerAvslåPeriodeneManuelt(UUID behandlingUUID, boolean innvilgePeriodene, Optional<Integer> antallDager) {
        try {
            var request = new ManuellVurderingRequest(behandlingUUID.toString(), innvilgePeriodene, antallDager.orElse(null));
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum/innvilgEllerAvslaa");
            restKlient.patch(endpoint, request);
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilInnvilgeEllerAvslåPeriodeneManuelt(e.getMessage(), e).toException();
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
        } catch (IllegalStateException e) {
            throw e; // rethrow
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilhentÅrskvantumForBehandling(e.getMessage(), e).toException();
        }
    }

    @Override
    public ÅrskvantumForbrukteDagerV2 hentÅrskvantumForBehandlingV2(UUID behandlingUUID) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum/forbruktedager/v2?behandlingUUID=" + behandlingUUID.toString());
            var result = restKlient.get(endpoint, ÅrskvantumForbrukteDagerV2.class);
            var constraints = VALIDATOR.validate(result);
            if (!constraints.isEmpty()) {
                throw new IllegalStateException("Ugyldig response fra " + endpoint + ", behandlingUUID=" + behandlingUUID + ": " + constraints);
            }
            return result;
        } catch (IllegalStateException e) {
            throw e; // rethrow
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
        } catch (IllegalStateException e) {
            throw e; // rethrow
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
        } catch (IllegalStateException e) {
            throw e; // rethrow
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

    @Override
    public ÅrskvantumUtbetalingGrunnlag hentUtbetalingGrunnlag(ÅrskvantumGrunnlagV2 årskvantumGrunnlag) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum/hentUtbetalingGrunnlag/v2");
            return restKlient.post(endpoint, årskvantumGrunnlag, ÅrskvantumUtbetalingGrunnlag.class);
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallHentUtbetalingGrunnlag(e.getMessage(), e).toException();
        }
    }

    @Override
    public RammevedtakResponse hentRammevedtak(PersonIdent personIdent, List<PersonIdent> barnFnr, LukketPeriode periode) {
        try {
            var barnasFnr = barnFnr.stream().map(barn -> barn.getIdent()).toList();
            var request = new RammevedtakRequest(personIdent.getIdent(), barnasFnr, periode);
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum/hentRammevedtak");
            return restKlient.post(endpoint, request, RammevedtakResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Feil ved kall til rammevedtakstjeneste på årskvantum", e);
        }
    }
    @Override
    public RammevedtakResponse hentRammevedtak(RammevedtakV2Request request) {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum/hentRammevedtak/v2");
            return restKlient.post(endpoint, request, RammevedtakResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Feil ved kall til rammevedtakstjeneste på årskvantum", e);
        }
    }

    @Override
    public ÅrskvantumUttrekk hentUttrekk() {
        try {
            var endpoint = URI.create(endpointUttaksplan.toString() + "/aarskvantum/hentUttrekk");
            return restKlient.post(endpoint, "", ÅrskvantumUttrekk.class);
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilÅrskvantum(e.getMessage(), e).toException();
        }
    }

    @Override
    public void oppdaterPersonident(PersonIdent nyPersonident, List<PersonIdent> gamlePersonidenter) {
        try {
            var request = new AktørBytte(gamlePersonidenter.stream().map(PersonIdent::getIdent).toList(), nyPersonident.getIdent());
            var endpoint = URI.create(endpointUttaksplan.toString() + "/oppdaterPersonident");
            restKlient.post(endpoint, request);
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilKallTilOppdaterPersonident(e.getMessage(), e).toException();
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

        @TekniskFeil(feilkode = "K9SAK-AK-1000097", feilmelding = "Feil ved kall til K9-AARSKVANTUM: Kunne ikke settUttaksplanTilManueltBekreftet: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilInnvilgeEllerAvslåPeriodeneManuelt(String feilmelding, Throwable t);

        @TekniskFeil(feilkode = "K9SAK-AK-1000098", feilmelding = "Feil ved kall til K9-AARSKVANTUM: Kunne ikke oppdatere personident: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilOppdaterPersonident(String feilmelding, Throwable t);

    }

}
