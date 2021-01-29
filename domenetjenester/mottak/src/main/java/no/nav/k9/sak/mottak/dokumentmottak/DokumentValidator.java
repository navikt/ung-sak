package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Collection;

import no.nav.k9.sak.mottak.repo.MottattDokument;

public interface DokumentValidator {

    void validerDokumenter(String behandlingId, Collection<MottattDokument> mottatteDokumenter);

    void validerDokument(MottattDokument mottatteDokumenter);
}
