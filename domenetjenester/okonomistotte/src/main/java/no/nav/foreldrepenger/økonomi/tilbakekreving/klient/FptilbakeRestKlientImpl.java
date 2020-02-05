package no.nav.foreldrepenger.økonomi.tilbakekreving.klient;

import java.net.URI;
import java.net.URISyntaxException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.client.utils.URIBuilder;

import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

@ApplicationScoped
public class FptilbakeRestKlientImpl implements FptilbakeRestKlient {

    public static final String FPTILBAKE_HENT_ÅPEN_TILBAKEKREVING = "/behandlinger/tilbakekreving/aapen";

    private OidcRestClient restClient;

    public FptilbakeRestKlientImpl() {
        // for CDI proxy
    }

    @Inject
    public FptilbakeRestKlientImpl(OidcRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public boolean harÅpenTilbakekrevingsbehandling(Saksnummer saksnummer) {
        URI uriHentÅpenTilbakekreving = lagRequestUri(saksnummer);
        return restClient.get(uriHentÅpenTilbakekreving, Boolean.class);
    }

    private URI lagRequestUri(Saksnummer saksnummer) {
        String fptilbakeBaseUrl = FptilbakeFelles.getFptilbakeBaseUrl();
        String endpoint = fptilbakeBaseUrl + FPTILBAKE_HENT_ÅPEN_TILBAKEKREVING;
        try {
            return new URIBuilder(endpoint).addParameter("saksnummer", saksnummer.getVerdi()).build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
