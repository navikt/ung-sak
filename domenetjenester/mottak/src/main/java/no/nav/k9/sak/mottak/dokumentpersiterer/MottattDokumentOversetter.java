package no.nav.k9.sak.mottak.dokumentpersiterer;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.mottak.repo.MottattDokument;

public interface MottattDokumentOversetter<T extends MottattDokumentWrapper<?>> {

    void trekkUtDataOgPersister(T wrapper, MottattDokument mottattDokument, Behandling behandling, Optional<LocalDate> gjelderFra);
}
