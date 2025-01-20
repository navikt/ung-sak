package no.nav.ung.sak.vilkår;

import static java.util.Arrays.asList;
import static no.nav.ung.kodeverk.vilkår.VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
import static no.nav.ung.kodeverk.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET;
import static no.nav.ung.kodeverk.vilkår.VilkårType.OPPTJENINGSPERIODEVILKÅR;
import static no.nav.ung.kodeverk.vilkår.VilkårType.OPPTJENINGSVILKÅRET;
import static no.nav.ung.kodeverk.vilkår.VilkårType.SØKERSOPPLYSNINGSPLIKT;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

@FagsakYtelseTypeRef
@BehandlingTypeRef
@ApplicationScoped
public class DefaultVilkårUtleder implements VilkårUtleder {

    private static final List<VilkårType> STANDARDVILKÅR = asList(
        SØKERSOPPLYSNINGSPLIKT,
        MEDLEMSKAPSVILKÅRET,
        OPPTJENINGSPERIODEVILKÅR,
        OPPTJENINGSVILKÅRET,
        BEREGNINGSGRUNNLAGVILKÅR);

    public DefaultVilkårUtleder() {
    }

    @Override
    public UtledeteVilkår utledVilkår(BehandlingReferanse referanse) {
        return new UtledeteVilkår(null, STANDARDVILKÅR);
    }

}
