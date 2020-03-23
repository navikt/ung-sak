package no.nav.k9.sak.økonomi.tilbakekreving.klient;

import java.net.URI;
import java.net.URISyntaxException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.client.utils.URIBuilder;

import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class FptilbakeRestKlientImpl implements FptilbakeRestKlient {

    private OidcRestClient restClient;
    private URI uriHarÅpenTilbakekrevingsbehandling;

    public FptilbakeRestKlientImpl() {
        // for CDI proxy
    }

    @Inject
    public FptilbakeRestKlientImpl(OidcRestClient restClient,
                                   @KonfigVerdi(value = "URL_FPTILBAKE_SJEKK_AAPEN_BEHANDLING", defaultVerdi = "http://fptilbake/fptilbake/api/behandlinger/tilbakekreving/aapen") String urlSjekkÅpenBehandling) {
        this.restClient = restClient;
        this.uriHarÅpenTilbakekrevingsbehandling = tilUri(urlSjekkÅpenBehandling, "URL_FPTILBAKE_SJEKK_AAPEN_BEHANDLING");
    }

    @Override
    public boolean harÅpenTilbakekrevingsbehandling(Saksnummer saksnummer) {
        URI uri = leggTilParameter(uriHarÅpenTilbakekrevingsbehandling, "saksnummer", saksnummer.getVerdi());
        return restClient.get(uri, Boolean.class);
    }

    private static URI leggTilParameter(URI uri, String parameterNavn, String parameterVerdi) {
        try {
            return new URIBuilder(uri).addParameter(parameterNavn, parameterVerdi).build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Klarte ikke legge til parameter " + parameterNavn, e);
        }
    }

    private static URI tilUri(String url, String konfigurertNavn) {
        try {
            return new URIBuilder(url).build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for " + konfigurertNavn, e);
        }
    }

}
