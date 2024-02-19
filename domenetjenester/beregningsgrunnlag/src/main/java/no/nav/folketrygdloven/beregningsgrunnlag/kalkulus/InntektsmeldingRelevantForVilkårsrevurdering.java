package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Collection;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Finner liste med inntektsmeldinger som er relevant for vilkårsvurdering for gitt vilkår.
 * Opptjening og Beregning har ulike kriterier for om en inntektsmelding skal påvirke vurdering av vilkåret
 */
public interface InntektsmeldingRelevantForVilkårsrevurdering {

    static InntektsmeldingRelevantForVilkårsrevurdering finnTjeneste(Instance<InntektsmeldingRelevantForVilkårsrevurdering> instances, VilkårType vilkårType, FagsakYtelseType fagsakYtelseType) {
        Instance<InntektsmeldingRelevantForVilkårsrevurdering> selected = instances.select(new VilkårTypeRef.VilkårTypeRefLiteral(vilkårType));
        if (selected.isAmbiguous()) {
            return FagsakYtelseTypeRef.Lookup.find(selected, fagsakYtelseType).orElseThrow(() -> new IllegalStateException("Har ikke InntektsmeldingRelevantForVilkårsvurdering for " + fagsakYtelseType));
        } else if (selected.isUnsatisfied()) {
            throw new IllegalArgumentException("Ingen implementasjoner funnet for vilkårtype:" + vilkårType);
        }

        InntektsmeldingRelevantForVilkårsrevurdering minInstans = selected.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException(
                "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return minInstans;
    }


    Collection<Inntektsmelding> begrensInntektsmeldinger(BehandlingReferanse referanse, Collection<Inntektsmelding> sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode);

}
