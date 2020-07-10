package no.nav.k9.sak.økonomi.tilbakekreving.klient;

import java.net.URI;
import java.net.URISyntaxException;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@Dependent
public class K9TilbakeRestKlient {

    private static final Logger log = LoggerFactory.getLogger(K9TilbakeRestKlient.class);

    private OidcRestClient restClient;
    private URI uriHarÅpenTilbakekrevingsbehandling;
    private boolean fptilbakeAktivert;

    K9TilbakeRestKlient() {
        // for CDI proxy
    }

    @Inject
    public K9TilbakeRestKlient(OidcRestClient restClient,
                               @KonfigVerdi(value = "k9.tilbake.direkte.url", defaultVerdi = "http://k9-tilbake/k9/tilbake/api") String urlK9Tilbake,
                               @KonfigVerdi(value = "K9TILBAKE_AKTIVERT", defaultVerdi = "false", required = false) boolean fptilbakeAktivert) {
        this.restClient = restClient;
        this.uriHarÅpenTilbakekrevingsbehandling = tilUri(urlK9Tilbake, "behandlinger/tilbakekreving/aapen");
        this.fptilbakeAktivert = fptilbakeAktivert;
    }

    public boolean harÅpenTilbakekrevingsbehandling(Saksnummer saksnummer) {
        URI uri = leggTilParameter(uriHarÅpenTilbakekrevingsbehandling, "saksnummer", saksnummer.getVerdi());
        if(fptilbakeAktivert){
            return restClient.get(uri, Boolean.class);
        } else {
            log.info("Fptilbake er ikke aktivert - antar at sak {} ikke har tilbakekrevingsbehandling", saksnummer);
            return false;
        }
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
