package no.nav.foreldrepenger.økonomi.simulering.klient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.client.utils.URIBuilder;

import no.nav.k9.oppdrag.kontrakt.BehandlingReferanse;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class K9OppdragRestKlient {
    private OidcRestClient restClient;
    private URI uriIverksett;
    private URI uriSimulering;
    private URI uriSimuleringResultat;

    public K9OppdragRestKlient() {
    }

    @Inject
    public K9OppdragRestKlient(OidcRestClient restClient,
                               @KonfigVerdi(value = "URL_K9OPPDRAG_IVERKSETT", defaultVerdi = "http://k9-oppdrag/k9/oppdrag/api/iverksett/start") String urlIverksett,
                               @KonfigVerdi(value = "URL_K9OPPDRAG_SIMULERING", defaultVerdi = "http://k9-oppdrag/k9/oppdrag/api/simulering/start") String urlSimulering,
                               @KonfigVerdi(value = "URL_K9OPPDRAG_SIMULERING_RESULTAT", defaultVerdi = "http://k9-oppdrag/k9/oppdrag/api/simulering/resultat") String urlSimuleringResultat) {
        this.restClient = restClient;
        this.uriIverksett = tilUri(urlIverksett, "URL_K9OPPDRAG_IVERKSETT");
        this.uriSimulering = tilUri(urlSimulering, "URL_K9OPPDRAG_SIMULERING");
        this.uriSimuleringResultat = tilUri(urlSimuleringResultat, "URL_K9OPPDRAG_SIMULERING_RESULTAT");
    }

    private static URI tilUri(String url, String konfigurertNavn) {
        try {
            return new URIBuilder(url).build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for " + konfigurertNavn, e);
        }
    }

    public void startIverksettelse(TilkjentYtelseOppdrag tilkjentYtelseOppdrag) {
        restClient.post(uriIverksett, tilkjentYtelseOppdrag);
    }

    public void startSimulering(TilkjentYtelseOppdrag tilkjentYtelseOppdrag) {
        restClient.post(uriSimulering, tilkjentYtelseOppdrag);
    }

    public SimuleringResultatDto hentSimuleringResultat(UUID behandlingUuid) {
        BehandlingReferanse behandlingreferanse = new BehandlingReferanse(behandlingUuid);
        return restClient.post(uriSimuleringResultat, behandlingreferanse, SimuleringResultatDto.class);
    }

}
