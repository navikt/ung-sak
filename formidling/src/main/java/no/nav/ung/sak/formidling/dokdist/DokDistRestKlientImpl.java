package no.nav.ung.sak.formidling.dokdist;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostResponse;

/**
 * https://confluence.adeo.no/pages/viewpage.action?pageId=320038938
 *
 *
 * https://dokdistfordeling.dev.intern.nav.no/swagger-ui/index.html
 *
 */
@Dependent
@ScopedRestIntegration(scopeKey = "DOKDISTFORDELING_SCOPE", defaultScope = "api://prod-fss.teamdokumenthandtering.saf/.default")
public class DokDistRestKlientImpl implements DokDistRestKlient {

    private OidcRestClient restClient;
    private URI uriDokdist;

    DokDistRestKlientImpl() {
    }

    @Inject
    public DokDistRestKlientImpl(
        OidcRestClient restClient,
        @KonfigVerdi(value = "DOKDISTFORDELING_URL", defaultVerdi = "http://dokdistfordeling.teamdokumenthandtering/rest/v1") String urlDokdistFordeling
    ) {
        this.restClient = restClient;
        this.uriDokdist = tilUri(urlDokdistFordeling, "distribuerjournalpost");
    }

    @Override
    public DistribuerJournalpostResponse distribuer(DistribuerJournalpostRequest request) {
        return restClient.post(uriDokdist, request, DistribuerJournalpostResponse.class);
    }


    private static URI tilUri(String baseUrl, String path) {
        try {
            return new URI(baseUrl + "/" + path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for dokdistfordeling", e);
        }
    }
}
