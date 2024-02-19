package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg;

import static java.util.Arrays.asList;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_AO;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;

@FagsakYtelseTypeRef(OMSORGSPENGER_AO)
@BehandlingTypeRef
@ApplicationScoped
public class AleneomsorgVilkårUtleder implements VilkårUtleder {

    private static final List<VilkårType> YTELSE_VILKÅR = asList(
        VilkårType.ALDERSVILKÅR_BARN,
        VilkårType.OMSORGEN_FOR,
        VilkårType.UTVIDETRETT);

    @Override
    public UtledeteVilkår utledVilkår(Behandling behandling) {
        return new UtledeteVilkår(null, YTELSE_VILKÅR);
    }

}
