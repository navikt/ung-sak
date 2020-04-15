package no.nav.k9.sak.økonomi.simulering.klient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.oppdrag.kontrakt.BehandlingReferanse;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@Dependent
public class K9OppdragRestKlient {
    private OidcRestClient restClient;
    private URI uriIverksett;
    private URI uriSimulering;
    private URI uriSimuleringResultat;
    private URI uriKansellerSimulering;

    public K9OppdragRestKlient() {
    }

    @Inject
    public K9OppdragRestKlient(OidcRestClient restClient, @KonfigVerdi(value = "url.k9oppdrag") String urlK9Oppdrag) {
        this.restClient = restClient;
        this.uriIverksett = tilUri(urlK9Oppdrag, "iverksett/start");
        this.uriSimulering = tilUri(urlK9Oppdrag, "simulering/start");
        this.uriSimuleringResultat = tilUri(urlK9Oppdrag, "simulering/resultat");
        this.uriKansellerSimulering = tilUri(urlK9Oppdrag, "simulering/kanseller");
    }

    private static URI tilUri(String baseUrl, String path) {
        try {
            return new URI(baseUrl + "/" + path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for URL_K9OPPDRAG", e);
        }
    }

    public void startIverksettelse(TilkjentYtelseOppdrag tilkjentYtelseOppdrag) {
        restClient.post(uriIverksett, tilkjentYtelseOppdrag);
    }

    public void startSimulering(TilkjentYtelseOppdrag tilkjentYtelseOppdrag) {
        restClient.post(uriSimulering, tilkjentYtelseOppdrag);
    }

    public Optional<SimuleringResultatDto> hentSimuleringResultat(UUID behandlingUuid) {
        BehandlingReferanse behandlingreferanse = new BehandlingReferanse(behandlingUuid);
        return restClient.postReturnsOptional(uriSimuleringResultat, behandlingreferanse, SimuleringResultatDto.class);
    }

    public void kansellerSimulering(UUID behandlingUuid) {
        BehandlingReferanse behandlingreferanse = new BehandlingReferanse(behandlingUuid);
        restClient.post(uriKansellerSimulering, behandlingreferanse);
    }

}
