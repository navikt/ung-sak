package no.nav.ung.sak.web.server.abac;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.Decision;
import no.nav.sif.abac.kontrakt.abac.dto.SaksinformasjonOgPersonerTilgangskontrollInputDto;
import no.nav.sif.abac.kontrakt.abac.resultat.Tilgangsbeslutning;

import java.net.URI;
import java.net.URISyntaxException;

@Dependent
@ScopedRestIntegration(scopeKey = "sif.abac.pdp.scope", defaultScope = "api://prod-gcp.k9saksbehandling.sif-abac-pdp/.default")
public class SifAbacPdpRestKlient {

    private OidcRestClient restClient;
    private URI uriTilgangskontrollMedSaksinformasjon;

    SifAbacPdpRestKlient() {
        // for CDI proxy
    }

    @Inject
    public SifAbacPdpRestKlient(OidcRestClient restClient,
                                @KonfigVerdi(value = "sif.abac.pdp.url", defaultVerdi = "http://sif-abac-pdp/sif/sif-abac-pdp/api/tilgangskontroll/v2/ung") String urlSifAbacPdp) {
        this.restClient = restClient;
        this.uriTilgangskontrollMedSaksinformasjon = tilUri(urlSifAbacPdp, "saksinformasjon");
    }

    public Tilgangsbeslutning sjekkTilgangForInnloggetBruker(SaksinformasjonOgPersonerTilgangskontrollInputDto input) {
        return restClient.post(uriTilgangskontrollMedSaksinformasjon, input, Tilgangsbeslutning.class);
    }

    private static URI tilUri(String baseUrl, String path) {
        try {
            return new URI(baseUrl + "/" + path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for sif.abac.pdp.url", e);
        }
    }

}
