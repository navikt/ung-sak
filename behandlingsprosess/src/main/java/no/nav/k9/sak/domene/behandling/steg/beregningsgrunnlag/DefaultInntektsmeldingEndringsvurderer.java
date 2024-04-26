package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.Collection;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;

@FagsakYtelseTypeRef
@VilkårTypeRef
public class DefaultInntektsmeldingEndringsvurderer implements InntektsmeldingerEndringsvurderer {

    @Override
    public Collection<Inntektsmelding> finnInntektsmeldingerMedRelevanteEndringer(Collection<Inntektsmelding> gjeldendeInntektsmeldinger, Collection<Inntektsmelding> inntektsmeldingerForrigeVedtak) {
        return gjeldendeInntektsmeldinger;
    }
}
