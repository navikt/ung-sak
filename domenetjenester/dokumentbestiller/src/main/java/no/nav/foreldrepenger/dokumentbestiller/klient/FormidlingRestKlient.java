package no.nav.foreldrepenger.dokumentbestiller.klient;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class FormidlingRestKlient {
    private static final String ENDPOINT_KEY_SJEKK_DOKUMENT_PRODUSERT = "fpformidling.erdokumentprodusert.url";
    private OidcRestClient oidcRestClient;
    private URI endpointSjekkDokumentProdusert;

    public FormidlingRestKlient() {
        // CDI
    }

    @Inject
    public FormidlingRestKlient(OidcRestClient oidcRestClient,
                                    @KonfigVerdi(ENDPOINT_KEY_SJEKK_DOKUMENT_PRODUSERT) URI endpointSjekkDokumentProdusert) {
        this.oidcRestClient = oidcRestClient;
        this.endpointSjekkDokumentProdusert = endpointSjekkDokumentProdusert;
    }

}
