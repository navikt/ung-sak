package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import static java.util.Arrays.asList;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;

@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
@BehandlingTypeRef
@ApplicationScoped
class UtvidetRettInngangsvilkårUtleder implements VilkårUtleder {

    private static final List<VilkårType> YTELSE_VILKÅR = asList(
        VilkårType.OMSORGEN_FOR,
        VilkårType.ALDERSVILKÅR,
        VilkårType.UTVIDETRETT);

    public UtvidetRettInngangsvilkårUtleder() {
    }

    @Override
    public UtledeteVilkår utledVilkår(Behandling behandling) {
        return new UtledeteVilkår(null, YTELSE_VILKÅR);
    }

}
