package no.nav.ung.sak.vilkår;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

import java.util.List;

import static java.util.Arrays.asList;
import static no.nav.ung.kodeverk.vilkår.VilkårType.ALDERSVILKÅR;
import static no.nav.ung.kodeverk.vilkår.VilkårType.UNGDOMSPROGRAMVILKÅRET;

@FagsakYtelseTypeRef
@BehandlingTypeRef
@ApplicationScoped
public class DefaultVilkårUtleder implements VilkårUtleder {

    private static final List<VilkårType> STANDARDVILKÅR = asList(
        ALDERSVILKÅR,
        UNGDOMSPROGRAMVILKÅRET);

    public DefaultVilkårUtleder() {
    }

    @Override
    public UtledeteVilkår utledVilkår(BehandlingReferanse referanse) {
        return new UtledeteVilkår(null, STANDARDVILKÅR);
    }

}
