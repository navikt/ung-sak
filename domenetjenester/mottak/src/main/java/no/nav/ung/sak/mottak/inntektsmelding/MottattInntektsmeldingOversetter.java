package no.nav.ung.sak.mottak.inntektsmelding;

import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.domene.iay.modell.InntektsmeldingBuilder;

public interface MottattInntektsmeldingOversetter<T extends MottattInntektsmeldingWrapper<?>> {

    InntektsmeldingBuilder trekkUtData(T wrapper, MottattDokument mottattDokument);
}
