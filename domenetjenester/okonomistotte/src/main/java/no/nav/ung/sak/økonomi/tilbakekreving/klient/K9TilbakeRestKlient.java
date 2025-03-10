package no.nav.ung.sak.økonomi.tilbakekreving.klient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.økonomi.tilbakekreving.dto.BehandlingStatusOgFeilutbetalinger;

@Dependent
@ScopedRestIntegration(scopeKey = "k9.tilbake.scope", defaultScope = "api://prod-fss.k9saksbehandling.k9-tilbake/.default")
public class K9TilbakeRestKlient {

    private static final Logger log = LoggerFactory.getLogger(K9TilbakeRestKlient.class);

    private OidcRestClient restClient;
    private URI uriHarÅpenTilbakekrevingsbehandling;
    private URI uriFeilutbetalingerSisteBehandling;
    private URI uriOppdaterAktørId;
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
        this.uriOppdaterAktørId = tilUri(urlK9Tilbake, "forvaltning/aktør/oppdaterAktoerId");
        this.k9tilbakeAktivert = false; //FIXME integrer mot tilbakekrevingsløsning
    }

    public boolean harÅpenTilbakekrevingsbehandling(Saksnummer saksnummer) {
        URI uri = leggTilParameter(uriHarÅpenTilbakekrevingsbehandling, "saksnummer", saksnummer.getVerdi());
        if (k9tilbakeAktivert) {
            return restClient.get(uri, Boolean.class);
        } else {
            log.info("integrasjon mot tilbakekrevingsløsningen er ikke aktivert - antar at sak {} ikke har tilbakekrevingsbehandling", saksnummer);
            return false;
        }
    }

    public Optional<BehandlingStatusOgFeilutbetalinger> hentFeilutbetalingerForSisteBehandling(Saksnummer saksnummer) {
        URI uri = leggTilParameter(uriFeilutbetalingerSisteBehandling, "saksnummer", saksnummer.getVerdi());
        if (k9tilbakeAktivert) {
            return restClient.getReturnsOptional(uri, BehandlingStatusOgFeilutbetalinger.class);
        } else {
            log.info("integrasjon mot tilbakekrevingsløsningen er ikke aktivert - antar at sak {} ikke har tilbakekrevingsbehandling", saksnummer);
            return Optional.empty();
        }
    }

    public Integer utførAktørbytte(AktørId gyldigAktørId, AktørId utgåttAktørId) {
        return restClient.post(uriOppdaterAktørId, new ByttAktørRequest(utgåttAktørId.getAktørId(), gyldigAktørId.getAktørId()), Integer.class);
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

    private record ByttAktørRequest(
        @JsonProperty(value = "utgatt", required = true) String utgåttAktør,
        @JsonProperty(value = "gyldig", required = true) String gyldigAktør
    ) {
    }


}
