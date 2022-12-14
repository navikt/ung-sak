package no.nav.k9.sak.økonomi.simulering.klient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.oppdrag.kontrakt.BehandlingReferanse;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.k9.sikkerhet.oidc.token.impl.ContextTokenProvider;

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
    public K9OppdragRestKlient(ContextTokenProvider tokenProvider,
                               @KonfigVerdi(value = "k9.oppdrag.direkte.url", defaultVerdi = "http://k9-oppdrag/k9/oppdrag/api") String urlK9Oppdrag,
                               @KonfigVerdi(value = "k9.oppdrag.scope", defaultVerdi = "api://prod-fss.k9saksbehandling.k9-oppdrag/.default") String k9OppdragScope) {
        this.uriIverksett = tilUri(urlK9Oppdrag, "iverksett/start");
        this.uriSimulering = tilUri(urlK9Oppdrag, "simulering/start");
        this.uriSimuleringResultat = tilUri(urlK9Oppdrag, "simulering/resultat");
        this.uriKansellerSimulering = tilUri(urlK9Oppdrag, "simulering/kanseller");

        //avviker fra @Inject av OidcRestClient fordi det trengs lenger timeout enn normalt mot k9-oppdrag pga simuleringer som tar lang tid (over 20 sekunder) når det er mange perioder
        restClient = new K9OppdragRestClientConfig().createOidcRestClient(tokenProvider, k9OppdragScope);
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
