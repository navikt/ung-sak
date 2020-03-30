package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår;

import static java.util.Arrays.asList;
import static no.nav.k9.kodeverk.vilkår.VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
import static no.nav.k9.kodeverk.vilkår.VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR;
import static no.nav.k9.kodeverk.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET;
import static no.nav.k9.kodeverk.vilkår.VilkårType.OMSORGEN_FOR;
import static no.nav.k9.kodeverk.vilkår.VilkårType.OPPTJENINGSPERIODEVILKÅR;
import static no.nav.k9.kodeverk.vilkår.VilkårType.OPPTJENINGSVILKÅRET;
import static no.nav.k9.kodeverk.vilkår.VilkårType.SØKERSOPPLYSNINGSPLIKT;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;

@FagsakYtelseTypeRef("PSB")
@BehandlingTypeRef
@ApplicationScoped
public class InngangsvilkårUtleder implements VilkårUtleder {

    private static final List<VilkårType> YTELSE_VILKÅR = asList(
        SØKERSOPPLYSNINGSPLIKT,
        MEDLEMSKAPSVILKÅRET,
        OMSORGEN_FOR,
        MEDISINSKEVILKÅR_UNDER_18_ÅR,
        OPPTJENINGSPERIODEVILKÅR,
        OPPTJENINGSVILKÅRET,
        BEREGNINGSGRUNNLAGVILKÅR);

    public InngangsvilkårUtleder() {
    }

    @Override
    public UtledeteVilkår utledVilkår(Behandling behandling) {
        return new UtledeteVilkår(null, YTELSE_VILKÅR);
    }

}
