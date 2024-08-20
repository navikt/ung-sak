package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;

import java.util.List;

import static java.util.Arrays.asList;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS;

@FagsakYtelseTypeRef(OMSORGSPENGER_KS)
@BehandlingTypeRef
@ApplicationScoped
public class KroniskSykVilkårUtleder implements VilkårUtleder {
    private boolean aldersvilkårIKroniskSyk;

    @Inject
    public KroniskSykVilkårUtleder(
        @KonfigVerdi(value = "ALDERSVILKAR_I_KRONISK_SYK", defaultVerdi = "false") boolean aldersvilkårIKroniskSyk
    ) {
        this.aldersvilkårIKroniskSyk = aldersvilkårIKroniskSyk;
    }

    private static final List<VilkårType> YTELSE_VILKÅR = asList(
        VilkårType.ALDERSVILKÅR_BARN,
        VilkårType.OMSORGEN_FOR,
        VilkårType.UTVIDETRETT);
    private static final List<VilkårType> GAMLE_YTELSE_VILKÅR = asList(
        VilkårType.OMSORGEN_FOR,
        VilkårType.UTVIDETRETT);

    @Override
    public UtledeteVilkår utledVilkår(BehandlingReferanse referanse) {
        return new UtledeteVilkår(null, aldersvilkårIKroniskSyk ? YTELSE_VILKÅR : GAMLE_YTELSE_VILKÅR);
    }

}
