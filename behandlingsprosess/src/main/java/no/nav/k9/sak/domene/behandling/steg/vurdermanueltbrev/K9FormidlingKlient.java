package no.nav.k9.sak.domene.behandling.steg.vurdermanueltbrev;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.formidling.kontrakt.forvaltning.aktør.ByttAktørRequest;
import no.nav.k9.formidling.kontrakt.informasjonsbehov.InformasjonsbehovListeDto;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.AktørId;

@Dependent
@ScopedRestIntegration(scopeKey = "k9.formidling.scope", defaultScope = "api://prod-fss.k9saksbehandling.k9-formidling/.default")
public class K9FormidlingKlient {
    private OidcRestClient restClient;
    private URI uriInformasjonsbehov;
    private URI uriOppdaterAktørId;


    @Inject
    public K9FormidlingKlient(OidcRestClient restClient, @KonfigVerdi(value = "k9.formidling.url", defaultVerdi = "http://k9-formidling/k9/formidling") String urlK9Formidling) {
        this.restClient = restClient;
        this.uriInformasjonsbehov = tilUri(urlK9Formidling, "api/brev/informasjonsbehov");
        this.uriOppdaterAktørId = tilUri(urlK9Formidling, "api/forvaltning/oppdaterAktoerId");
    }

    private static URI tilUri(String baseUrl, String path) {
        try {
            return new URI(baseUrl + "/" + path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for URL_K9OPPDRAG", e);
        }
    }

    public InformasjonsbehovListeDto hentInformasjonsbehov(UUID behandingUuid, FagsakYtelseType ytelseType) {
        URI uri = UriBuilder.fromUri(uriInformasjonsbehov)
            .queryParam("behandlingUuid", behandingUuid)
            .queryParam("sakstype", ytelseType.getKode())
            .build();
        return restClient.get(uri, InformasjonsbehovListeDto.class);
    }

    public void oppdaterAktørId(AktørId gyldigAktørId, AktørId utgåttAktørId) {
        restClient.post(UriBuilder.fromUri(uriOppdaterAktørId).build(), new ByttAktørRequest(utgåttAktørId.getAktørId(), gyldigAktørId.getAktørId()));
    }


}
