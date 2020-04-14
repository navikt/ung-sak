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
public class FptilbakeRestKlientImpl implements FptilbakeRestKlient {

    private static final Logger log = LoggerFactory.getLogger(FptilbakeRestKlientImpl.class);

    private OidcRestClient restClient;
    private URI uriHarÅpenTilbakekrevingsbehandling;
    private boolean fptilbakeAktivert;

    public FptilbakeRestKlientImpl() {
        // for CDI proxy
    }

    @Inject
    public FptilbakeRestKlientImpl(OidcRestClient restClient,
                                   @KonfigVerdi(value = "URL_FPTILBAKE_SJEKK_AAPEN_BEHANDLING", defaultVerdi = "http://fptilbake/fptilbake/api/behandlinger/tilbakekreving/aapen") String urlSjekkÅpenBehandling,
                                   @KonfigVerdi(value = "FPTILBAKE_AKTIVERT", defaultVerdi = "false", required = false) boolean fptilbakeAktivert) {
        this.restClient = restClient;
        this.uriHarÅpenTilbakekrevingsbehandling = tilUri(urlSjekkÅpenBehandling, "URL_FPTILBAKE_SJEKK_AAPEN_BEHANDLING");
        this.fptilbakeAktivert = fptilbakeAktivert;
    }

    @Override
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

    private static URI tilUri(String url, String konfigurertNavn) {
        try {
            return new URIBuilder(url).build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for " + konfigurertNavn, e);
        }
    }

}
