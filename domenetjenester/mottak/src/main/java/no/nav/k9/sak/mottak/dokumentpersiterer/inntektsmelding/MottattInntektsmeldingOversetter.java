package no.nav.k9.sak.mottak.dokumentpersiterer.inntektsmelding;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.mottak.repo.MottattDokument;

public interface MottattInntektsmeldingOversetter<T extends MottattInntektsmeldingWrapper<?>> {

    InntektsmeldingInnhold trekkUtDataOgPersister(T wrapper, MottattDokument mottattDokument, Behandling behandling, Optional<LocalDate> gjelderFra);
}
