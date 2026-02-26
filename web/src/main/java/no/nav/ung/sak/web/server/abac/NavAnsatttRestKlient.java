package no.nav.ung.sak.web.server.abac;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.Decision;
import no.nav.sif.abac.kontrakt.abac.InnloggetAnsattDto;
import no.nav.sif.abac.kontrakt.abac.InnloggetAnsattUngDto;
import no.nav.sif.abac.kontrakt.abac.InnloggetAnsattUngV2Dto;
import no.nav.sif.abac.kontrakt.abac.dto.SaksinformasjonTilgangskontrollInputDto;

import java.net.URI;
import java.net.URISyntaxException;

@Dependent
@ScopedRestIntegration(scopeKey = "sif.abac.pdp.scope", defaultScope = "api://prod-gcp.k9saksbehandling.sif-abac-pdp/.default")
public class NavAnsatttRestKlient {

    private OidcRestClient restClient;
    private URI uriNavAnsatt;
    private URI uriNavV2Ansatt;

    NavAnsatttRestKlient() {
        // for CDI proxy
    }

    @Inject
    public NavAnsatttRestKlient(OidcRestClient restClient,
                                @KonfigVerdi(value = "nav.ansatt.url", defaultVerdi = "http://sif-abac-pdp/sif/sif-abac-pdp/api/ung/nav-ansatt") String urlSifAbacPdpNavAnsatt,
                                @KonfigVerdi(value = "nav.ansatt.v2.url", defaultVerdi = "http://sif-abac-pdp/sif/sif-abac-pdp/api/ung/nav-ansatt/v2") String urlSifAbacPdpNavAnsattV2
                                ) {
        this.restClient = restClient;
        this.uriNavAnsatt = tilUri(urlSifAbacPdpNavAnsatt, "nav.ansatt.url");
        this.uriNavV2Ansatt = tilUri(urlSifAbacPdpNavAnsattV2, "nav.ansatt.v2.url");
    }

    public InnloggetAnsattDto tilangerForInnloggetBruker() {
        return restClient.get(uriNavAnsatt, InnloggetAnsattDto.class);
    }

    public InnloggetAnsattUngV2Dto tilangerForInnloggetBrukerV2() {
        return restClient.get(uriNavV2Ansatt, InnloggetAnsattUngV2Dto.class);
    }

    private static URI tilUri(String baseUrl, String propertyName) {
        try {
            return new URI(baseUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for " + propertyName, e);
        }
    }

}
