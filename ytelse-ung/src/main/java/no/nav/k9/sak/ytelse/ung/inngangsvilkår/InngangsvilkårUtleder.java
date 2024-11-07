package no.nav.k9.sak.ytelse.ung.inngangsvilkår;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;

import java.util.List;

import static java.util.Arrays.asList;
import static no.nav.k9.kodeverk.vilkår.VilkårType.ALDERSVILKÅR;
import static no.nav.k9.kodeverk.vilkår.VilkårType.UNGDOMSPROGRAMVILKÅRET;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@BehandlingTypeRef
public class InngangsvilkårUtleder implements VilkårUtleder {

    private static final List<VilkårType> YTELSE_VILKÅR = asList(
        ALDERSVILKÅR,
        UNGDOMSPROGRAMVILKÅRET
    );

    public InngangsvilkårUtleder() {
    }

    @Override
    public UtledeteVilkår utledVilkår(BehandlingReferanse referanse) {
        return new UtledeteVilkår(null, YTELSE_VILKÅR);
    }

}
