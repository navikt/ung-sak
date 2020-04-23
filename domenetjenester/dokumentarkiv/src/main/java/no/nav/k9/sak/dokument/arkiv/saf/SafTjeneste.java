package no.nav.k9.sak.dokument.arkiv.saf;

import no.nav.k9.sak.dokument.arkiv.saf.graphql.DokumentoversiktFagsakQuery;
import no.nav.k9.sak.dokument.arkiv.saf.graphql.HentDokumentQuery;
import no.nav.k9.sak.dokument.arkiv.saf.graphql.JournalpostQuery;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.DokumentoversiktFagsak;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.Journalpost;

public interface SafTjeneste {
    DokumentoversiktFagsak dokumentoversiktFagsak(DokumentoversiktFagsakQuery query);

    Journalpost hentJournalpostInfo(JournalpostQuery journalpostId);

    String hentDokument(HentDokumentQuery query);
}
