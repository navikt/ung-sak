package no.nav.k9.sak.web.app.tjenester.los;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

@ApplicationScoped
@Default
public class LosSystemUserKlient {

    private SystemUserOidcRestClient restKlient;
    private URI endpoint;

    protected LosSystemUserKlient() { }

    @Inject
    public LosSystemUserKlient(SystemUserOidcRestClient restKlient, @KonfigVerdi(value = "k9.los.url") URI endpoint) {
        this.restKlient = restKlient;
        this.endpoint = endpoint;
    }

    public String lagreMerknad(MerknadEndretDto merknad) {
        try {
            var uri = toUri("/saksbehandler/oppgaver/"+merknad.getBehandlingUuid().toString()+"/merknad");
            return restKlient.post(uri, merknad);
        } catch (Exception e) {
            throw LosSystemUserKlient.RestTjenesteFeil.FEIL.feilVedLagringAvMerknad(e.getMessage(), e).toException();
        }
    }

    public String hentMerknad(UUID behandlingUUID) {
        try {
            var uri = toUri("/saksbehandler/oppgaver/"+behandlingUUID.toString()+"/merknad");
            return restKlient.get(uri);
        } catch (Exception e) {
            throw LosSystemUserKlient.RestTjenesteFeil.FEIL.feilVedHentingAvMerknad(e.getMessage(), e).toException();
        }
    }


    private URI toUri(String relativeUri) {
        String uri = endpoint.toString() + relativeUri;
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig uri: " + uri, e);
        }
    }

    interface RestTjenesteFeil extends DeklarerteFeil {
        RestTjenesteFeil FEIL = FeilFactory.create(RestTjenesteFeil.class);

        @TekniskFeil(feilkode = "K9SAK-LOS-1000001", feilmelding = "Feil ved kall til K9-LOS (Lagre merknad): %s", logLevel = LogLevel.WARN)
        Feil feilVedLagringAvMerknad(String feilmelding, Throwable t);
        @TekniskFeil(feilkode = "K9SAK-LOS-1000002", feilmelding = "Feil ved kall til K9-LOS (Hente merknad): %s", logLevel = LogLevel.WARN)
        Feil feilVedHentingAvMerknad(String feilmelding, Throwable t);
    }
}
