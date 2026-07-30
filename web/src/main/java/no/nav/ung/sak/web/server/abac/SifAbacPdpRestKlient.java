package no.nav.ung.sak.web.server.abac;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.sif.abac.kontrakt.abac.dto.SaksinformasjonOgPersonerTilgangskontrollInputDto;
import no.nav.sif.abac.kontrakt.abac.resultat.Tilgangsbeslutning;

import java.net.URI;
import java.net.URISyntaxException;

@Dependent
@ScopedRestIntegration(scopeKey = "sif.abac.pdp.scope", defaultScope = "api://prod-gcp.k9saksbehandling.sif-abac-pdp/.default")
public class SifAbacPdpRestKlient {

    private OidcRestClient restClient;
    private URI uriTilgangskontrollUngMedSaksinformasjon;
    private URI uriTilgangskontrollAktivitetspengerMedSaksinformasjon;

    SifAbacPdpRestKlient() {
        // for CDI proxy
    }

    @Inject
    public SifAbacPdpRestKlient(OidcRestClient restClient,
                                @KonfigVerdi(value = "sif.abac.pdp.ung.url", defaultVerdi = "http://sif-abac-pdp/sif/sif-abac-pdp/api/tilgangskontroll/v2/ung") String urlSifAbacPdpUngUrl,
                                @KonfigVerdi(value = "sif.abac.pdp.aktivitetspenger.url", defaultVerdi = "http://sif-abac-pdp/sif/sif-abac-pdp/api/tilgangskontroll/v2/aktivitetspenger") String urlSifAbacPdpAktivitetspengerUrl
    ) {
        this.restClient = restClient;
        this.uriTilgangskontrollUngMedSaksinformasjon = tilUri(urlSifAbacPdpUngUrl, "saksinformasjon", "sif.abac.pdp.ung.url");
        this.uriTilgangskontrollAktivitetspengerMedSaksinformasjon = tilUri(urlSifAbacPdpAktivitetspengerUrl, "saksinformasjon", "sif.abac.pdp.aktivitetspenger.url");
    }

    public Tilgangsbeslutning sjekkTilgangForInnloggetBrukerUng(SaksinformasjonOgPersonerTilgangskontrollInputDto input) {
        return restClient.post(uriTilgangskontrollUngMedSaksinformasjon, input, Tilgangsbeslutning.class);
    }

    public Tilgangsbeslutning sjekkTilgangForInnloggetBrukerAktivitetspenger(SaksinformasjonOgPersonerTilgangskontrollInputDto input) {
        return restClient.post(uriTilgangskontrollAktivitetspengerMedSaksinformasjon, input, Tilgangsbeslutning.class);
    }

    private static URI tilUri(String baseUrl, String path, String propertyName) {
        try {
            return new URI(baseUrl + "/" + path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for " + propertyName, e);
        }
    }

}
