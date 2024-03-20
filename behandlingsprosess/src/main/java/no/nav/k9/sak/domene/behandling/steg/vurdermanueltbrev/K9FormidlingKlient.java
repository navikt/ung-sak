package no.nav.k9.sak.domene.behandling.steg.vurdermanueltbrev;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.formidling.kontrakt.informasjonsbehov.InformasjonsbehovListeDto;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sikkerhet.oidc.token.impl.ContextTokenProvider;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

@Dependent
public class K9FormidlingKlient {
    private OidcRestClient restClient;
    private URI uriInformasjonsbehov;

    @Inject
    public K9FormidlingKlient(ContextTokenProvider tokenProvider,
                              @KonfigVerdi(value = "k9.formidling.url", defaultVerdi = "http://k9-formidling/k9/formidling") String urlK9Formidling,
                              @KonfigVerdi(value = "k9.formidling.scope", defaultVerdi = "api://prod-fss.k9saksbehandling.k9-formidling/.default") String k9FormidlingScope) {
        this.uriInformasjonsbehov = tilUri(urlK9Formidling, "api/brev/informasjonsbehov");

        //avviker fra @Inject av OidcRestClient fordi det trengs lenger timeout enn normalt mot k9-sak pga tilbakekall til k9sak (vilkaar-v3 tar 12sek og brukes i både formidling og formidling-dokumentdata)
        restClient = new K9FormidlingRestClientConfig().createOidcRestClient(tokenProvider, k9FormidlingScope);
    }

    private static URI tilUri(String baseUrl, String path) {
        try {
            return new URI(baseUrl + "/" + path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for URL_K9Formidling", e);
        }
    }

    public InformasjonsbehovListeDto hentInformasjonsbehov(UUID behandingUuid, FagsakYtelseType ytelseType) {
        URI uri = UriBuilder.fromUri(uriInformasjonsbehov)
            .queryParam("behandlingUuid", behandingUuid)
            .queryParam("sakstype", ytelseType.getKode())
            .build();
        return restClient.get(uri, InformasjonsbehovListeDto.class);
    }
}
