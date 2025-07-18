package no.nav.ung.sak.økonomi.simulering.klient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.oppdrag.kontrakt.BehandlingReferanse;
import no.nav.k9.oppdrag.kontrakt.aktørbytte.ByttAktørRequest;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringDto;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.k9.sikkerhet.oidc.token.impl.ContextTokenProvider;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.økonomi.simulering.klient.dto.OppdragXmlDto;

@Dependent
public class K9OppdragRestKlient {
    private OidcRestClient restClient;
    private URI uriIverksett;
    private URI uriSimulering;
    private URI uriAktørBytte;
    private URI uriSimuleringDiagnostikk;
    private URI uriSimuleringResultat;
    private URI uriAlleOppdragXmler;
    private URI uriDetaljertSimuleringResultat;
    private URI uriKansellerSimulering;

    public K9OppdragRestKlient() {
    }

    @Inject
    public K9OppdragRestKlient(ContextTokenProvider tokenProvider,
                               @KonfigVerdi(value = "k9.oppdrag.direkte.url", defaultVerdi = "http://k9-oppdrag/k9/oppdrag/api") String urlK9Oppdrag,
                               @KonfigVerdi(value = "k9.oppdrag.scope", defaultVerdi = "api://prod-fss.k9saksbehandling.k9-oppdrag/.default") String k9OppdragScope) {
        this.uriIverksett = tilUri(urlK9Oppdrag, "iverksett/start");
        this.uriSimulering = tilUri(urlK9Oppdrag, "simulering/start");
        this.uriAktørBytte = tilUri(urlK9Oppdrag, "forvaltning/oppdaterAktoerId");
        this.uriSimuleringDiagnostikk = tilUri(urlK9Oppdrag, "diagnostikk/simulering");
        this.uriSimuleringResultat = tilUri(urlK9Oppdrag, "simulering/resultat");
        this.uriAlleOppdragXmler = tilUri(urlK9Oppdrag, "iverksett/forvaltning/hent-oppdrag-xmler");
        this.uriDetaljertSimuleringResultat = tilUri(urlK9Oppdrag, "simulering/detaljert-resultat");
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

    public String utførSimuleringDiagnostikk(TilkjentYtelseOppdrag tilkjentYtelseOppdrag) {
        return restClient.post(uriSimuleringDiagnostikk, tilkjentYtelseOppdrag);
    }

    public Optional<SimuleringResultatDto> hentSimuleringResultat(UUID behandlingUuid) {
        BehandlingReferanse behandlingreferanse = new BehandlingReferanse(behandlingUuid);
        return restClient.postReturnsOptional(uriSimuleringResultat, behandlingreferanse, SimuleringResultatDto.class);
    }

    public Optional<SimuleringDto> hentDetaljertSimuleringResultat(UUID behandlingUuid) {
        BehandlingReferanse behandlingreferanse = new BehandlingReferanse(behandlingUuid);
        return restClient.postReturnsOptional(uriDetaljertSimuleringResultat, behandlingreferanse, SimuleringDto.class);
    }

    public void kansellerSimulering(UUID behandlingUuid) {
        BehandlingReferanse behandlingreferanse = new BehandlingReferanse(behandlingUuid);
        restClient.post(uriKansellerSimulering, behandlingreferanse);
    }

    public List<OppdragXmlDto> alleOppdragXmler(Saksnummer saksnummer){
        OppdragXmlDto[] resultat = restClient.post(uriAlleOppdragXmler, new no.nav.k9.oppdrag.kontrakt.Saksnummer(saksnummer.getVerdi()), OppdragXmlDto[].class);
        return resultat != null ? Arrays.asList(resultat) : null;
    }

    public Integer utførAktørbytte(AktørId gyldigAktørId, AktørId utgåttAktørId, Set<PersonIdent> utgåttPersonident) {
        var identer = utgåttPersonident.stream().map(PersonIdent::getIdent)
            .map(ByttAktørRequest.PersonIdent::new)
            .collect(Collectors.toSet());
        var request = new ByttAktørRequest(utgåttAktørId.getAktørId(), gyldigAktørId.getAktørId(), identer);
        return restClient.post(uriAktørBytte, request, Integer.class);
    }


}
