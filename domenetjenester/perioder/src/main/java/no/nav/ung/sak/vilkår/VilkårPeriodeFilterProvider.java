package no.nav.ung.sak.vilkår;


import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;

@Dependent
public class VilkårPeriodeFilterProvider {

    private final VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public VilkårPeriodeFilterProvider(VilkårResultatRepository vilkårResultatRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public VilkårPeriodeFilter getFilter(BehandlingReferanse referanse) {
        return new VilkårPeriodeFilter(referanse, vilkårResultatRepository);
    }

}
