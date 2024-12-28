package no.nav.ung.sak.formidling.dokarkiv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostResponse;

/**
 * https://confluence.adeo.no/display/BOA/opprettJournalpost
 */
public class DokArkivKlient {

    private List<OpprettJournalpostRequest> requests = new ArrayList();

    private final String journalpostId = "12345";

    public List<OpprettJournalpostRequest> getRequests() {
        return Collections.unmodifiableList(requests);
    }

    public OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest request) {
        requests.add(request);
        return new OpprettJournalpostResponse(journalpostId, Collections.emptyList(), true, "melding");
    }
}
