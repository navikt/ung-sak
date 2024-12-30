package no.nav.ung.sak.formidling.dokarkiv;

import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostResponse;

public interface DokArkivKlient {
    OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest request);
}
