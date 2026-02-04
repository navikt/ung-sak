package no.nav.ung.sak.økonomi.tilbakekreving.klient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import no.nav.k9.felles.konfigurasjon.env.Environment;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.økonomi.tilbakekreving.dto.BehandlingStatusOgFeilutbetalinger;

@Dependent
@ScopedRestIntegration(scopeKey = "ung.tilbake.scope", defaultScope = "api://prod-gcp.k9saksbehandling.ung-tilbake/.default")
public class UngTilbakeRestKlient {

    private static final Logger log = LoggerFactory.getLogger(UngTilbakeRestKlient.class);

    private OidcRestClient restClient;
    private URI uriHarÅpenTilbakekrevingsbehandling;
    private URI uriFeilutbetalingerSisteBehandling;
    private URI uriOppdaterAktørId;
    private boolean ungTilbakeAktivert;

    UngTilbakeRestKlient() {
        // for CDI proxy
    }

    @Inject
    public UngTilbakeRestKlient(OidcRestClient restClient,
                                @KonfigVerdi(value = "ung.tilbake.url", defaultVerdi = "http://ung-tilbake/ung/tilbake/api") String urlUngTilbake) {

        this.restClient = restClient;
        this.uriHarÅpenTilbakekrevingsbehandling = tilUri(urlUngTilbake, "behandlinger/tilbakekreving/aapen");
        this.uriFeilutbetalingerSisteBehandling = tilUri(urlUngTilbake, "feilutbetaling/siste-behandling");
        this.uriOppdaterAktørId = tilUri(urlUngTilbake, "forvaltning/aktør/oppdaterAktoerId");
        this.ungTilbakeAktivert = !Environment.current().isLocal(); //i praksis mocker bort ung-tilbake ved kjøring lokalt og i verdikjedetester.
    }

    public boolean harÅpenTilbakekrevingsbehandling(Saksnummer saksnummer) {
        URI uri = leggTilParameter(uriHarÅpenTilbakekrevingsbehandling, "saksnummer", saksnummer.getVerdi());
        if (ungTilbakeAktivert) {
            return restClient.get(uri, Boolean.class);
        } else {
            log.info("integrasjon mot tilbakekrevingsløsningen er ikke aktivert - antar at sak {} ikke har tilbakekrevingsbehandling", saksnummer);
            return false;
        }
    }

    public Optional<BehandlingStatusOgFeilutbetalinger> hentFeilutbetalingerForSisteBehandling(Saksnummer saksnummer) {
        URI uri = leggTilParameter(uriFeilutbetalingerSisteBehandling, "saksnummer", saksnummer.getVerdi());
        if (ungTilbakeAktivert) {
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
            throw new IllegalArgumentException("Ugyldig konfigurasjon for ung.tilbake.url eller UNG_TILBAKE_URL", e);
        }
    }

    private record ByttAktørRequest(
        @JsonProperty(value = "utgatt", required = true) String utgåttAktør,
        @JsonProperty(value = "gyldig", required = true) String gyldigAktør
    ) {
    }


}
