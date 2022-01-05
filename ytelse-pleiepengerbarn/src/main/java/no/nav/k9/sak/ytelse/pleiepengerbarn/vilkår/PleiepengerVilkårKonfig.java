package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Instance;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;

public interface PleiepengerVilkårKonfig {

    static PleiepengerVilkårKonfig finnVilkårKonfig(Instance<PleiepengerVilkårKonfig> brevkodeMappere, FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(brevkodeMappere, fagsakYtelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + PleiepengerVilkårKonfig.class.getSimpleName() + " for ytelseType=" + fagsakYtelseType));
    }

    Map<VilkårType, VilkårsPeriodiseringsFunksjon> getVilkårsPeriodisering();

    VilkårsPeriodiseringsFunksjon getDefaultVilkårsperiodisering();

    Set<VilkårType> definerendeVilkår();

    VilkårUtleder getVilkårUtleder();
}
