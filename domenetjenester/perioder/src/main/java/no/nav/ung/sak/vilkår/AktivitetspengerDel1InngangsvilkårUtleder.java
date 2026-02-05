package no.nav.ung.sak.vilkår;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

import java.util.List;

import static java.util.Arrays.asList;
import static no.nav.ung.kodeverk.vilkår.VilkårType.ALDERSVILKÅR;
import static no.nav.ung.kodeverk.vilkår.VilkårType.BOSTEDSVILKÅR;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
@BehandlingTypeRef(BehandlingType.AKTIVITETSPENGER_DEL_1)
public class AktivitetspengerDel1InngangsvilkårUtleder implements VilkårUtleder {

    private static final List<VilkårType> YTELSE_VILKÅR = asList(
        ALDERSVILKÅR, //TODO AKT denne skal kanskje flyttes til del2
        BOSTEDSVILKÅR
    );

    public AktivitetspengerDel1InngangsvilkårUtleder() {
        //for CDI proxy
    }

    @Override
    public UtledeteVilkår utledVilkår(BehandlingReferanse referanse) {
        return new UtledeteVilkår(null, YTELSE_VILKÅR);
    }

}
