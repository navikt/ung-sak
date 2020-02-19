package no.nav.foreldrepenger.inngangsvilkaar.impl;

import static java.util.Arrays.asList;
import static no.nav.foreldrepenger.inngangsvilkaar.impl.UtledeteVilkår.forVilkår;
import static no.nav.k9.kodeverk.vilkår.VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
import static no.nav.k9.kodeverk.vilkår.VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR;
import static no.nav.k9.kodeverk.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET;
import static no.nav.k9.kodeverk.vilkår.VilkårType.OMSORGEN_FOR;
import static no.nav.k9.kodeverk.vilkår.VilkårType.OPPTJENINGSPERIODEVILKÅR;
import static no.nav.k9.kodeverk.vilkår.VilkårType.OPPTJENINGSVILKÅRET;
import static no.nav.k9.kodeverk.vilkår.VilkårType.SØKERSOPPLYSNINGSPLIKT;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.k9.kodeverk.vilkår.VilkårType;

@FagsakYtelseTypeRef
@BehandlingTypeRef
@ApplicationScoped
public class DefaultVilkårUtleder implements VilkårUtleder {

    private static final List<VilkårType> STANDARDVILKÅR = asList(
        SØKERSOPPLYSNINGSPLIKT,
        MEDLEMSKAPSVILKÅRET,
        OMSORGEN_FOR,
        MEDISINSKEVILKÅR_UNDER_18_ÅR,
        OPPTJENINGSPERIODEVILKÅR,
        OPPTJENINGSVILKÅRET,
        BEREGNINGSGRUNNLAGVILKÅR);

    public DefaultVilkårUtleder() {
    }

    @Override
    public UtledeteVilkår utledVilkår(Behandling behandling) {
        // FIXME K9 refactor for K9 ytelser
        return forVilkår(null, STANDARDVILKÅR);
    }

}
