package no.nav.foreldrepenger.mottak.dokumentpersiterer;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;

public interface MottattDokumentOversetter<T extends MottattDokumentWrapper<?>> {

    void trekkUtDataOgPersister(T wrapper, MottattDokument mottattDokument, Behandling behandling, Optional<LocalDate> gjelderFra);
}
