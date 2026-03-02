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
@BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD )
public class AktivitetspengerInngangsvilkårUtleder implements VilkårUtleder {

    private static final List<VilkårType> YTELSE_VILKÅR = asList(
        ALDERSVILKÅR,
        BOSTEDSVILKÅR
    );

    public AktivitetspengerInngangsvilkårUtleder() {
        //for CDI proxy
    }

    @Override
    public UtledeteVilkår utledVilkår(BehandlingReferanse referanse) {
        return new UtledeteVilkår(null, YTELSE_VILKÅR);
    }

}
