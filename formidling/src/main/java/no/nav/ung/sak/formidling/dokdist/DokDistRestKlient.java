package no.nav.ung.sak.formidling.dokdist;

import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostResponse;


public interface DokDistRestKlient {
    DistribuerJournalpostResponse distribuer(DistribuerJournalpostRequest request);
}
