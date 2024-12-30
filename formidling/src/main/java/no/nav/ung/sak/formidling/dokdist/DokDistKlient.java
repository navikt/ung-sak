package no.nav.ung.sak.formidling.dokdist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostResponse;

/**
 * https://confluence.adeo.no/pages/viewpage.action?pageId=320038938
 *
 *
 * https://dokdistfordeling.dev.intern.nav.no/swagger-ui/index.html
 *
 */
@ApplicationScoped
public class DokDistKlient {

    private List<DistribuerJournalpostRequest> requests = new ArrayList();
    private List<DistribuerJournalpostResponse> responses = new ArrayList();

    private final String defaultDokdistId = "dokdist1234";

    public DokDistKlient() {
    }

    public DistribuerJournalpostResponse distribuer(DistribuerJournalpostRequest request) {
        requests.add(request);
        var response = new DistribuerJournalpostResponse(defaultDokdistId);
        responses.add(response);
        return response;
    }

    public List<DistribuerJournalpostRequest> getRequests() {
        return Collections.unmodifiableList(requests);
    }

    public List<DistribuerJournalpostResponse> getResponses() {
        return Collections.unmodifiableList(responses);
    }
}
