package no.nav.k9.sak.mottak.dokumentpersiterer.inntektsmelding;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingInnhold;
import no.nav.k9.sak.mottak.repo.MottattDokument;

public interface MottattInntektsmeldingOversetter<T extends MottattInntektsmeldingWrapper<?>> {

    InntektsmeldingInnhold trekkUtData(T wrapper, MottattDokument mottattDokument, Behandling behandling);
}
