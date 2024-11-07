package no.nav.k9.sak.mottak.inntektsmelding;

import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;

public interface MottattInntektsmeldingOversetter<T extends MottattInntektsmeldingWrapper<?>> {

    InntektsmeldingBuilder trekkUtData(T wrapper, MottattDokument mottattDokument);
}
