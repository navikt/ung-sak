package no.nav.k9.sak.økonomi.tilbakekreving.klient;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

@Dependent
public class K9TilbakeRestKlient {

    private OidcRestClient restClient;
    private URI uriHarÅpenTilbakekrevingsbehandling;

    K9TilbakeRestKlient() {
        // for CDI proxy
    }

    @Inject
    public K9TilbakeRestKlient(OidcRestClient restClient,
                               @KonfigVerdi(value = "k9.tilbake.direkte.url", defaultVerdi = "http://k9-tilbake/k9/tilbake/api") String urlK9Tilbake) {
        this.restClient = restClient;
        this.uriHarÅpenTilbakekrevingsbehandling = tilUri(urlK9Tilbake, "behandlinger/tilbakekreving/aapen");
    }

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

    private static URI tilUri(String baseUrl, String path) {
        try {
            return new URI(baseUrl + "/" + path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for URL_K9TILBAKE", e);
        }
    }

}
