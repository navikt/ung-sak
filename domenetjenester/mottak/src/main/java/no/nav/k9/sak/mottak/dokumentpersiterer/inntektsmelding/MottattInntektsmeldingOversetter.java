package no.nav.k9.sak.mottak.dokumentpersiterer.inntektsmelding;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.mottak.repo.MottattDokument;

public interface MottattInntektsmeldingOversetter<T extends MottattInntektsmeldingWrapper<?>> {

    InntektsmeldingBuilder trekkUtData(T wrapper, MottattDokument mottattDokument, Behandling behandling);
}
