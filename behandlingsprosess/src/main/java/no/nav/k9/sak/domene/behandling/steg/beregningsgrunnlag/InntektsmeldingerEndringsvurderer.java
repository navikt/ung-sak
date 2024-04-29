package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.Collection;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingRelevantForVilkårsrevurdering;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;

public interface InntektsmeldingerEndringsvurderer {


    static InntektsmeldingerEndringsvurderer finnTjeneste(Instance<InntektsmeldingerEndringsvurderer> instances, VilkårType vilkårType, FagsakYtelseType fagsakYtelseType) {
        Instance<InntektsmeldingerEndringsvurderer> selected = instances.select(new VilkårTypeRef.VilkårTypeRefLiteral(vilkårType));
        if (selected.isAmbiguous()) {
            return FagsakYtelseTypeRef.Lookup.find(selected, fagsakYtelseType).orElse(new DefaultInntektsmeldingEndringsvurderer());
        } else if (selected.isUnsatisfied()) {
            return new DefaultInntektsmeldingEndringsvurderer();
        }

        InntektsmeldingerEndringsvurderer minInstans = selected.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException(
                "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return minInstans;
    }



    Collection<Inntektsmelding> finnInntektsmeldingerMedRelevanteEndringer(Collection<Inntektsmelding> gjeldendeInntektsmeldinger, Collection<Inntektsmelding> inntektsmeldingerForrigeVedtak);



}
