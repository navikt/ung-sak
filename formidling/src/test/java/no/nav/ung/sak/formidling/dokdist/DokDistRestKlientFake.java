package no.nav.ung.sak.formidling.dokdist;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Alternative
@ApplicationScoped
@Priority(value = 1)
public class DokDistRestKlientFake implements DokDistRestKlient {

    private final List<DistribuerJournalpostRequest> requests = new ArrayList<>();
    private final List<DistribuerJournalpostResponse> responses = new ArrayList<>();

    private final String defaultDokdistId = "dokdist1234";

    public DokDistRestKlientFake() {
    }

    @Override
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
