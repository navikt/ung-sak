package no.nav.ung.sak.formidling.dokarkiv;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostResponse;

/**
 * <a href="https://confluence.adeo.no/display/BOA/opprettJournalpost">opprettJournalpost</a>
 */
@Dependent
@ScopedRestIntegration(scopeKey = "DOKARKIV_SCOPE", defaultScope = "api://prod-fss.teamdokumenthandtering.dokarkiv/.default")
public class DokArkivKlientImpl implements DokArkivKlient {

    private final OidcRestClient restClient;
    private final URI uriDokarkiv;

    @Inject
    public DokArkivKlientImpl(
        OidcRestClient restClient,
        @KonfigVerdi(value = "DOKARKIV_URL", defaultVerdi = "http://dokdistfordeling.teamdokumenthandtering/rest/v1") String urlDokarkiv) {

        this.uriDokarkiv = UriBuilder.fromUri(tilUri(urlDokarkiv, "journalpost"))
            .queryParam("forsoekFerdigstill", true)
            .build();
        this.restClient = restClient;
    }

    @Override
    public OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest request) {
        return restClient.post(uriDokarkiv, request, OpprettJournalpostResponse.class);
    }

    private static URI tilUri(String baseUrl, String path) {
        try {
            return new URI(baseUrl + "/" + path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for dokdistfordeling", e);
        }
    }
}
