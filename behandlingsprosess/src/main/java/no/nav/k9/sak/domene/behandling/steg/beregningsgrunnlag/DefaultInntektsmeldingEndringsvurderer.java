package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.Collection;

import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;

class DefaultInntektsmeldingEndringsvurderer implements InntektsmeldingerEndringsvurderer {
    @Override
    public Collection<Inntektsmelding> finnInntektsmeldingerMedRelevanteEndringer(Collection<Inntektsmelding> gjeldendeInntektsmeldinger, Collection<Inntektsmelding> inntektsmeldingerForrigeVedtak) {
        return gjeldendeInntektsmeldinger;
    }
}
