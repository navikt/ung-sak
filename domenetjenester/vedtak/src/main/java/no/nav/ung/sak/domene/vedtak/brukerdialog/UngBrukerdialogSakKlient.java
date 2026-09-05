package no.nav.ung.sak.domene.vedtak.brukerdialog;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.brukerdialog.kontrakt.vedtak.FagSakRequest;

import java.net.URI;
import java.net.URISyntaxException;

@Dependent
@ScopedRestIntegration(scopeKey = "ung.brukerdialog.api.scope", defaultScope = "api://prod-gcp.k9saksbehandling.ung-brukerdialog-api/.default")
public class UngBrukerdialogSakKlient {

    private final OidcRestClient restClient;
    private final URI fagsakUri;

    @Inject
    public UngBrukerdialogSakKlient(
        OidcRestClient restClient,
        @KonfigVerdi(value = "ung.brukerdialog.api.url", defaultVerdi = "http://ung-brukerdialog-api/ung/brukerdialog/intern/api") String url) {
        this.restClient = restClient;
        this.fagsakUri = tilUri(url, "aktivitetspenger/fagsak");
    }

    public void sendVedtaksstatus(FagSakRequest request) {
        restClient.post(fagsakUri, request);
    }

    private static URI tilUri(String baseUrl, String path) {
        try {
            return new URI(baseUrl + "/" + path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for ung.brukerdialog.api.url", e);
        }
    }
}
