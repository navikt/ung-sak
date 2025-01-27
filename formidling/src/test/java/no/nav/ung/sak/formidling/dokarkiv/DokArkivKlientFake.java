package no.nav.ung.sak.formidling.dokarkiv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostResponse;


public class DokArkivKlientFake implements DokArkivKlient {

    public DokArkivKlientFake() {
    }

    private final List<OpprettJournalpostRequest> requests = new ArrayList<>();
    private final List<OpprettJournalpostResponse> responses = new ArrayList<>();

    private final String journalpostId = "12345";

    public List<OpprettJournalpostRequest> getRequests() {
        return Collections.unmodifiableList(requests);
    }

    @Override
    public OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest request) {
        requests.add(request);
        var opprettJournalpostResponse = new OpprettJournalpostResponse(journalpostId, Collections.emptyList(), true, "melding");
        responses.add(opprettJournalpostResponse);

        return opprettJournalpostResponse;
    }

    public List<OpprettJournalpostResponse> getResponses() {
        return Collections.unmodifiableList(responses);
    }
}
