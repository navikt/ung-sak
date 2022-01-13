package no.nav.k9.sak.dokument.bestill.klient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

@ApplicationScoped
@Default
public class FormidlingDokumentdataRestKlient implements FormidlingDokumentdataKlient {

    private OidcRestClient restKlient;
    private URI endpoint;

    protected FormidlingDokumentdataRestKlient() {
        // for proxying
    }

    @Inject
    public FormidlingDokumentdataRestKlient(OidcRestClient restKlient, @KonfigVerdi(value = "k9.formidling.dokumentdata.url") URI endpoint) {
        this.restKlient = restKlient;
        this.endpoint = toUri(endpoint);
    }

    @Override
    public void ryddVedTilbakehopp(UUID behandlingUUID) {
        try {
            var uri = URI.create(endpoint.toString() + "/api/saksprosess/ryddVedTilbakehopp?behandlingUuid=" + behandlingUUID.toString());
            restKlient.post(uri, Collections.emptyMap());
        } catch (Exception e) {
            throw RestTjenesteFeil.FEIL.feilVedSlettingAvDokumentdata(e.getMessage(), e).toException();
        }
    }

    private URI toUri(URI baseUri) {
        String uri = baseUri.toString();
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig uri: " + uri, e);
        }
    }

    interface RestTjenesteFeil extends DeklarerteFeil {
        RestTjenesteFeil FEIL = FeilFactory.create(RestTjenesteFeil.class);

        @TekniskFeil(feilkode = "K9SAK-DD-1000001", feilmelding = "Feil ved kall til K9-FORMIDLING-DOKUMENTDATA: Kunne ikke rydde i dokumentdata: %s", logLevel = LogLevel.WARN)
        Feil feilVedSlettingAvDokumentdata(String feilmelding, Throwable t);
    }
}
