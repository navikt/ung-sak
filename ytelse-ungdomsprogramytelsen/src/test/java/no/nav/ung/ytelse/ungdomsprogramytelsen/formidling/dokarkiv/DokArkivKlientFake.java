package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dokarkiv;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Alternative
@ApplicationScoped
@Priority(value = 1)
public class DokArkivKlientFake implements no.nav.ung.sak.formidling.dokarkiv.DokArkivKlient {

    public DokArkivKlientFake() {
    }

    private final List<OpprettJournalpostRequest> requests = new ArrayList<>();
    private final List<OpprettJournalpostResponse> responses = new ArrayList<>();

    private final String journalpostId = "12345";

    public List<OpprettJournalpostRequest> getRequests() {
        return Collections.unmodifiableList(requests);
    }

    @Override
    public OpprettJournalpostResponse opprettJournalpostOgFerdigstill(OpprettJournalpostRequest request) {
        requests.add(request);
        var opprettJournalpostResponse = new OpprettJournalpostResponse(journalpostId, Collections.emptyList(), true, "melding");
        responses.add(opprettJournalpostResponse);

        return opprettJournalpostResponse;
    }

    @Override
    public OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest request) {
        requests.add(request);
        var opprettJournalpostResponse = new OpprettJournalpostResponse(journalpostId, Collections.emptyList(), false, "melding");
        responses.add(opprettJournalpostResponse);

        return opprettJournalpostResponse;
    }

    public List<OpprettJournalpostResponse> getResponses() {
        return Collections.unmodifiableList(responses);
    }
}
