package no.nav.k9.sak.økonomi.tilbakekreving.klient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.økonomi.tilbakekreving.dto.BehandlingStatusOgFeilutbetalinger;

@Dependent
public class K9TilbakeRestKlient {

    private static final Logger log = LoggerFactory.getLogger(K9TilbakeRestKlient.class);

    private OidcRestClient restClient;
    private URI uriHarÅpenTilbakekrevingsbehandling;
    private URI uriFeilutbetalingerSisteBehandling;
    private boolean k9tilbakeAktivert;

    K9TilbakeRestKlient() {
        // for CDI proxy
    }

    @Inject
    public K9TilbakeRestKlient(OidcRestClient restClient,
                               @KonfigVerdi(value = "k9.tilbake.direkte.url", defaultVerdi = "http://k9-tilbake/k9/tilbake/api") String urlK9Tilbake) {

        this.restClient = restClient;
        this.uriHarÅpenTilbakekrevingsbehandling = tilUri(urlK9Tilbake, "behandlinger/tilbakekreving/aapen");
        this.uriFeilutbetalingerSisteBehandling = tilUri(urlK9Tilbake, "feilutbetaling/siste-behandling");
        this.k9tilbakeAktivert = !Environment.current().isLocal(); //i proaksis mocker bort k9-tilbake ved kjøring lokalt og i verdikjedetester.
    }

    public boolean harÅpenTilbakekrevingsbehandling(Saksnummer saksnummer) {
        URI uri = leggTilParameter(uriHarÅpenTilbakekrevingsbehandling, "saksnummer", saksnummer.getVerdi());
        if (k9tilbakeAktivert) {
            return restClient.get(uri, Boolean.class);
        } else {
            log.info("k9-tilbake er ikke aktivert - antar at sak {} ikke har tilbakekrevingsbehandling", saksnummer);
            return false;
        }
    }

    public Optional<BehandlingStatusOgFeilutbetalinger> hentFeilutbetalingerForSisteBehandling(Saksnummer saksnummer) {
        URI uri = leggTilParameter(uriFeilutbetalingerSisteBehandling, "saksnummer", saksnummer.getVerdi());
        if (k9tilbakeAktivert) {
            return restClient.postReturnsOptional(uri, saksnummer, BehandlingStatusOgFeilutbetalinger.class);
        } else {
            log.info("k9-tilbake er ikke aktivert - antar at sak {} ikke har tilbakekrevingsbehandling", saksnummer);
            return Optional.empty();
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
