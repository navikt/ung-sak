package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;

public interface SigruninntekterForBeregningFilter {

    static SigruninntekterForBeregningFilter finnTjeneste(Instance<SigruninntekterForBeregningFilter> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(SigruninntekterForBeregningFilter.class, instances, ytelseType)
            .orElse(new FinnInntekterFraForrigeRegisterinnhenting());
    }

    List<Inntekt> finnInntekter(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate skjæringstidspunkt);

}
