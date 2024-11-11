package no.nav.ung.sak.mottak.dokumentmottak;

import java.util.Collection;

import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;

public interface DokumentValidator {

    void validerDokumenter(Long behandlingId, Collection<MottattDokument> mottatteDokumenter);

    void validerDokument(MottattDokument mottatteDokumenter);
}
