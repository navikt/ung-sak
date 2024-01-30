package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Collection;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef
@ApplicationScoped
class DefaultInntektsmeldingRelevantForBeregningVilkårsvurdering implements InntektsmeldingRelevantForBeregningVilkårsvurdering {
    @Override
    public Collection<Inntektsmelding> begrensInntektsmeldinger(BehandlingReferanse referanse, Collection<Inntektsmelding> sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode) {
        return sakInntektsmeldinger;
    }
}
