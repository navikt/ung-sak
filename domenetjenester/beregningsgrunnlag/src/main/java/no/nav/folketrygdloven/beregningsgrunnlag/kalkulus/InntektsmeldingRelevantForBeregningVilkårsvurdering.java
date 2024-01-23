package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Collection;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface InntektsmeldingRelevantForBeregningVilkårsvurdering {
    static InntektsmeldingRelevantForBeregningVilkårsvurdering finnTjeneste(Instance<InntektsmeldingRelevantForBeregningVilkårsvurdering> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(instances, ytelseType)
            .orElse(new DefaultInntektsmeldingRelevantForBeregningVilkårsvurdering());
    }

    Collection<Inntektsmelding> begrensInntektsmeldinger(BehandlingReferanse referanse, Collection<Inntektsmelding> sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode);

}
